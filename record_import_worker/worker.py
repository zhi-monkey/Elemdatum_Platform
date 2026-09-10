"""Apollo CyberRT Record importer for the element-data Kubernetes namespace.

The process blocks on a Redis Stream. MySQL task state is owned by Java; this
process only claims work, converts sensor messages, uploads objects to MinIO,
and reports immutable asset metadata back to Java.
"""

from __future__ import annotations

import io
import logging
import os
import re
import socket
import struct
import tempfile
import time
import traceback
import zlib
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, wait, FIRST_COMPLETED
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import redis
import requests
from minio import Minio

try:
    from cyber_record.record import Record
except ImportError as exc:  # pragma: no cover
    raise SystemExit("cyber-record is required; install requirements.txt") from exc


IMAGE_TYPES = {"apollo.drivers.Image", "apollo.drivers.CompressedImage"}
POINTCLOUD_TYPES = {"apollo.drivers.PointCloud"}
QUEUE_STREAM = "multi:record-import"
QUEUE_GROUP = "recordimport-workers"


class PermanentImportError(RuntimeError):
    """The source Record is invalid or unsupported; retrying cannot help."""


@dataclass(frozen=True)
class Config:
    backend_url: str
    redis_url: str
    minio_endpoint: str
    minio_access_key: str
    minio_secret_key: str
    minio_secure: bool
    work_dir: Path
    worker_id: str
    asset_batch_size: int
    heartbeat_seconds: int
    upload_workers: int

    @classmethod
    def from_env(cls) -> "Config":
        def required(name: str) -> str:
            value = os.getenv(name, "").strip()
            if not value:
                raise RuntimeError("Missing required environment variable: {}".format(name))
            return value

        return cls(
            backend_url=required("BACKEND_URL").rstrip("/"),
            redis_url=required("REDIS_URL"),
            minio_endpoint=required("MINIO_ENDPOINT"),
            minio_access_key=required("MINIO_ACCESS_KEY"),
            minio_secret_key=required("MINIO_SECRET_KEY"),
            minio_secure=os.getenv("MINIO_SECURE", "false").lower() == "true",
            work_dir=Path(os.getenv("WORK_DIR", "/work")),
            worker_id=os.getenv("WORKER_ID", socket.gethostname()),
            asset_batch_size=int(os.getenv("ASSET_BATCH_SIZE", "100")),
            heartbeat_seconds=int(os.getenv("HEARTBEAT_SECONDS", "20")),
            upload_workers=int(os.getenv("UPLOAD_WORKERS", "8")),
        )


class BackendClient:
    def __init__(self, config: Config) -> None:
        self.base_url = config.backend_url + "/internal/multi/record-imports"
        self.headers: dict[str, str] = {}
        self.session = requests.Session()

    def claim(self, task_id: int, worker_id: str) -> dict[str, Any] | None:
        response = self._post("/claim", {"taskId": task_id, "workerId": worker_id})
        if response.status_code >= 400:
            logging.info("Task %s was already claimed or unavailable: %s", task_id, response.text)
            return None
        return self._data(response)

    def progress(self, task_id: int, worker_id: str, counters: "Counters") -> None:
        self._post_data(task_id, "/progress", counters.payload(worker_id))

    def assets(self, task_id: int, worker_id: str, counters: "Counters",
               images: list[dict[str, Any]], pointclouds: list[dict[str, Any]]) -> None:
        payload = counters.payload(worker_id)
        payload.update({"images": images, "pointclouds": pointclouds})
        self._post_data(task_id, "/assets", payload)

    def complete(self, task_id: int, worker_id: str) -> None:
        self._post_data(task_id, "/complete", {"workerId": worker_id})

    def fail(self, task_id: int, worker_id: str, error: str, retryable: bool) -> None:
        self._post_data(task_id, "/fail", {
            "workerId": worker_id,
            "errorMessage": error[:4000],
            "retryable": retryable,
        })

    def _post_data(self, task_id: int, suffix: str, payload: dict[str, Any]) -> None:
        response = self._post("/{}/{}".format(task_id, suffix.lstrip("/")), payload)
        response.raise_for_status()
        self._data(response)

    def _post(self, suffix: str, payload: dict[str, Any]) -> requests.Response:
        return self.session.post(self.base_url + suffix, headers=self.headers, json=payload, timeout=(10, 60))

    @staticmethod
    def _data(response: requests.Response) -> Any:
        response.raise_for_status()
        body = response.json()
        if body.get("code") not in (0, 200):
            raise RuntimeError("Backend rejected request: {}".format(body.get("msg", body)))
        return body.get("data")


@dataclass
class Counters:
    scanned_messages: int = 0
    processed_images: int = 0
    processed_pointclouds: int = 0

    def payload(self, worker_id: str) -> dict[str, Any]:
        return {
            "workerId": worker_id,
            "scannedMessages": self.scanned_messages,
            "processedImages": self.processed_images,
            "processedPointclouds": self.processed_pointclouds,
        }


class RecordImporter:
    def __init__(self, config: Config, minio_client: Minio, backend: BackendClient) -> None:
        self.config = config
        self.minio = minio_client
        self.backend = backend
        self.upload_executor = ThreadPoolExecutor(max_workers=config.upload_workers)

    def run(self, task: dict[str, Any]) -> None:
        task_id = int(task["id"])
        dataset_id = int(task["multiDatasetId"])
        bucket = task["sourceBucket"]
        source_key = task["sourceObjectKey"]
        self.config.work_dir.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="record-{}-".format(task_id), dir=str(self.config.work_dir)) as directory:
            workspace = Path(directory)
            record_path = workspace / "source.record"
            self._download_source(bucket, source_key, record_path, task.get("sourceSize"))
            self._import_record(task_id, dataset_id, bucket, record_path, workspace)

    def _download_source(self, bucket: str, object_key: str, target: Path, expected_size: int | None) -> None:
        partial = target.with_suffix(".part")
        logging.info("Downloading MinIO object %s/%s", bucket, object_key)
        self.minio.fget_object(bucket, object_key, str(partial))
        actual_size = partial.stat().st_size
        if expected_size is not None and actual_size != int(expected_size):
            remove_file(partial)
            raise PermanentImportError("Downloaded Record size does not match the committed object")
        partial.replace(target)

    def _import_record(self, task_id: int, dataset_id: int, bucket: str, record_path: Path, workspace: Path) -> None:
        counters = Counters()
        image_batch: list[dict[str, Any]] = []
        pointcloud_batch: list[dict[str, Any]] = []
        sequence_by_topic: Counter[str] = Counter()
        last_heartbeat = time.monotonic()
        upload_futures: list[tuple[str, Any]] = []
        max_pending_uploads = self.config.upload_workers * 2  # 允许队列中有2倍worker数量的任务

        try:
            record = Record(str(record_path), allow_unindexed=True)
        except Exception as exc:
            raise PermanentImportError("Cannot open CyberRT Record: {}".format(exc)) from exc

        try:
            with record:
                type_by_topic = {channel.name: channel.message_type for channel in record.get_channel_cache()}
                for topic, message, timestamp_ns in iter_record_messages(record):
                    counters.scanned_messages += 1
                    message_type = type_by_topic.get(topic, "")
                    if message_type not in IMAGE_TYPES and message_type not in POINTCLOUD_TYPES:
                        if time.monotonic() - last_heartbeat >= self.config.heartbeat_seconds:
                            self.backend.progress(task_id, self.config.worker_id, counters)
                            last_heartbeat = time.monotonic()
                        continue

                    # 如果队列太长，等待一些任务完成
                    while len(upload_futures) >= max_pending_uploads:
                        self._collect_completed_uploads(upload_futures, image_batch, pointcloud_batch, wait_one=True)
                        if len(image_batch) + len(pointcloud_batch) >= self.config.asset_batch_size:
                            self._flush_assets(task_id, counters, image_batch, pointcloud_batch)
                            last_heartbeat = time.monotonic()

                    sequence_no = sequence_by_topic[topic]
                    sequence_by_topic[topic] += 1

                    if message_type in IMAGE_TYPES:
                        future = self.upload_executor.submit(
                            self._store_image_async, task_id, dataset_id, bucket,
                            topic, timestamp_ns, sequence_no, message
                        )
                        upload_futures.append(('image', future))
                        counters.processed_images += 1
                    else:
                        future = self.upload_executor.submit(
                            self._store_pointcloud_async, task_id, dataset_id, bucket,
                            topic, timestamp_ns, sequence_no, message
                        )
                        upload_futures.append(('pointcloud', future))
                        counters.processed_pointclouds += 1

                    # 只收集已完成的，不阻塞
                    self._collect_completed_uploads(upload_futures, image_batch, pointcloud_batch, wait_one=False)

                    if len(image_batch) + len(pointcloud_batch) >= self.config.asset_batch_size:
                        self._flush_assets(task_id, counters, image_batch, pointcloud_batch)
                        last_heartbeat = time.monotonic()
                    if time.monotonic() - last_heartbeat >= self.config.heartbeat_seconds:
                        self.backend.progress(task_id, self.config.worker_id, counters)
                        last_heartbeat = time.monotonic()

            self._wait_all_uploads(upload_futures, image_batch, pointcloud_batch)

        except PermanentImportError:
            raise
        except Exception as exc:
            raise RuntimeError("Record scan failed: {}".format(exc)) from exc

        self._flush_assets(task_id, counters, image_batch, pointcloud_batch)
        self.backend.progress(task_id, self.config.worker_id, counters)

    def _flush_assets(self, task_id: int, counters: Counters,
                      images: list[dict[str, Any]], pointclouds: list[dict[str, Any]]) -> None:
        if not images and not pointclouds:
            return
        self.backend.assets(task_id, self.config.worker_id, counters, images, pointclouds)
        images.clear()
        pointclouds.clear()

    def _collect_completed_uploads(self, futures_list: list[tuple[str, Any]],
                                   image_batch: list[dict[str, Any]],
                                   pointcloud_batch: list[dict[str, Any]],
                                   wait_one: bool = False) -> None:
        """收集已完成的上传任务

        Args:
            futures_list: 待处理的future列表
            image_batch: 图片结果批次
            pointcloud_batch: 点云结果批次
            wait_one: 如果为True，至少等待一个任务完成
        """
        if wait_one and futures_list:
            # 等待至少一个任务完成
            futures_only = [f for _, f in futures_list]
            wait(futures_only, return_when=FIRST_COMPLETED)

        completed = []
        for i, (asset_type, future) in enumerate(futures_list):
            if future.done():
                try:
                    result = future.result()
                    if asset_type == 'image':
                        image_batch.append(result)
                    else:
                        pointcloud_batch.append(result)
                    completed.append(i)
                except Exception as exc:
                    logging.error("Upload failed: %s", exc)
                    raise

        for i in reversed(completed):
            futures_list.pop(i)

    def _wait_all_uploads(self, futures_list: list[tuple[str, Any]],
                         image_batch: list[dict[str, Any]],
                         pointcloud_batch: list[dict[str, Any]]) -> None:
        """等待所有上传完成"""
        for asset_type, future in futures_list:
            try:
                result = future.result()
                if asset_type == 'image':
                    image_batch.append(result)
                else:
                    pointcloud_batch.append(result)
            except Exception as exc:
                logging.error("Upload failed: %s", exc)
                raise
        futures_list.clear()

    def _store_image(self, task_id: int, dataset_id: int, bucket: str, topic: str, timestamp_ns: int,
                     sequence_no: int, message: Any, workspace: Path) -> dict[str, Any]:
        data = bytes(getattr(message, "data", b""))
        if not data:
            raise PermanentImportError("Image message contains no data on topic {}".format(topic))
        topic_key = safe_name(topic)
        extension, content_type = image_format(data)
        name = "{}_{}.{}".format(timestamp_ns, sequence_no, extension)
        object_key = "multi/{}/record/{}/images/{}/{}".format(dataset_id, task_id, topic_key, name)
        width = int(getattr(message, "width", 0) or 0) or None
        height = int(getattr(message, "height", 0) or 0) or None
        if is_encoded_image(data):
            result = self.minio.put_object(bucket, object_key, io.BytesIO(data), len(data), content_type=content_type)
            return image_asset(name, topic, timestamp_ns, sequence_no, object_key, len(data), content_type, result.etag, width, height)

        generated = workspace / "image-{}-{}.png".format(timestamp_ns, sequence_no)
        try:
            write_raw_image_png(message, data, generated)
            result = self.minio.fput_object(bucket, object_key, str(generated), content_type="image/png")
            return image_asset(name, topic, timestamp_ns, sequence_no, object_key, generated.stat().st_size,
                               "image/png", result.etag, width, height)
        finally:
            remove_file(generated)

    def _store_image_async(self, task_id: int, dataset_id: int, bucket: str, topic: str,
                          timestamp_ns: int, sequence_no: int, message: Any) -> dict[str, Any]:
        """异步上传图片（在线程池中执行，避免磁盘IO）"""
        data = bytes(getattr(message, "data", b""))
        if not data:
            raise PermanentImportError("Image message contains no data on topic {}".format(topic))

        topic_key = safe_name(topic)
        extension, content_type = image_format(data)
        name = "{}_{}.{}".format(timestamp_ns, sequence_no, extension)
        object_key = "dataset/multi/{}/images/{}/{}".format(dataset_id, topic_key, name)
        width = int(getattr(message, "width", 0) or 0) or None
        height = int(getattr(message, "height", 0) or 0) or None

        if is_encoded_image(data):
            result = self.minio.put_object(bucket, object_key, io.BytesIO(data), len(data), content_type=content_type)
            return image_asset(name, topic, timestamp_ns, sequence_no, object_key, len(data), content_type, result.etag, width, height)

        png_data = convert_raw_image_to_png_bytes(message, data)
        result = self.minio.put_object(bucket, object_key, io.BytesIO(png_data), len(png_data), content_type="image/png")
        return image_asset(name, topic, timestamp_ns, sequence_no, object_key, len(png_data),
                          "image/png", result.etag, width, height)

    def _store_pointcloud(self, task_id: int, dataset_id: int, bucket: str, topic: str, timestamp_ns: int,
                          sequence_no: int, message: Any, workspace: Path) -> dict[str, Any]:
        topic_key = safe_name(topic)
        name = "{}_{}.pcd".format(timestamp_ns, sequence_no)
        object_key = "multi/{}/record/{}/pointcloud/{}/{}".format(dataset_id, task_id, topic_key, name)
        generated = workspace / "pointcloud-{}-{}.pcd".format(timestamp_ns, sequence_no)
        try:
            point_count = write_pointcloud_pcd(message, generated)
            result = self.minio.fput_object(bucket, object_key, str(generated), content_type="application/x-pcd")
            return {
                "name": name,
                "topic": topic,
                "timestampNs": timestamp_ns,
                "sequenceNo": sequence_no,
                "objectKey": object_key,
                "fileSize": generated.stat().st_size,
                "contentType": "application/x-pcd",
                "etag": result.etag,
                "pointCount": point_count,
            }
        finally:
            remove_file(generated)

    def _store_pointcloud_async(self, task_id: int, dataset_id: int, bucket: str, topic: str,
                                timestamp_ns: int, sequence_no: int, message: Any) -> dict[str, Any]:
        """异步上传点云（在线程池中执行，避免磁盘IO）"""
        topic_key = safe_name(topic)
        name = "{}_{}.pcd".format(timestamp_ns, sequence_no)
        object_key = "dataset/multi/{}/pointcloud/{}/{}".format(dataset_id, topic_key, name)

        pcd_content, point_count = generate_pcd_content(message)
        result = self.minio.put_object(bucket, object_key, io.BytesIO(pcd_content),
                                      len(pcd_content), content_type="application/x-pcd")
        return {
            "name": name,
            "topic": topic,
            "timestampNs": timestamp_ns,
            "sequenceNo": sequence_no,
            "objectKey": object_key,
            "fileSize": len(pcd_content),
            "contentType": "application/x-pcd",
            "etag": result.etag,
            "pointCount": point_count,
        }


def image_asset(name: str, topic: str, timestamp_ns: int, sequence_no: int, object_key: str, size: int,
                content_type: str, etag: str, width: int | None, height: int | None) -> dict[str, Any]:
    return {
        "name": name,
        "topic": topic,
        "timestampNs": timestamp_ns,
        "sequenceNo": sequence_no,
        "objectKey": object_key,
        "fileSize": size,
        "contentType": content_type,
        "etag": etag,
        "width": width,
        "height": height,
    }


def iter_record_messages(record: Record):
    """Use the safest reader available across cyber-record releases.

    Newer releases expose the explicit section scanner. Older releases expose
    the same behavior as ``read_messages_fallback`` or only ``read_messages``.
    """
    readers = (
        ("read_messages_section_scan", getattr(record, "read_messages_section_scan", None)),
        ("read_messages_section_scan (reader)",
         getattr(getattr(record, "_reader", None), "read_messages_section_scan", None)),
        ("read_messages_fallback", getattr(record, "read_messages_fallback", None)),
        ("read_messages", getattr(record, "read_messages", None)),
    )
    for name, reader in readers:
        if callable(reader):
            logging.info("Reading Record with cyber_record.%s", name)
            yield from reader()
            return
    raise PermanentImportError("Installed cyber_record has no supported message reader")


def safe_name(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", value.strip("/")) or "unnamed"


def remove_file(path: Path) -> None:
    try:
        path.unlink()
    except FileNotFoundError:
        pass


def is_encoded_image(data: bytes) -> bool:
    return data.startswith(b"\x89PNG\r\n\x1a\n") or data.startswith(b"\xff\xd8")


def image_format(data: bytes) -> tuple[str, str]:
    if data.startswith(b"\xff\xd8"):
        return "jpg", "image/jpeg"
    return "png", "image/png"


def write_raw_image_png(message: Any, data: bytes, destination: Path) -> None:
    """Encode supported Apollo raw image messages without external image libraries."""
    width = int(getattr(message, "width", 0) or 0)
    height = int(getattr(message, "height", 0) or 0)
    encoding = str(getattr(message, "encoding", "") or "").lower()
    formats = {
        "mono8": (1, 0),
        "gray8": (1, 0),
        "rgb8": (3, 2),
        "bgr8": (3, 2),
        "rgba8": (4, 6),
        "bgra8": (4, 6),
    }
    if width <= 0 or height <= 0:
        raise PermanentImportError("Raw image has invalid dimensions {}x{}".format(width, height))
    if encoding not in formats:
        raise PermanentImportError("Unsupported raw image encoding: {}".format(encoding or "empty"))

    channels, color_type = formats[encoding]
    row_bytes = width * channels
    step = int(getattr(message, "step", 0) or row_bytes)
    if step < row_bytes or len(data) < step * height:
        raise PermanentImportError("Raw image data size does not match its dimensions")

    rows: list[bytes] = []
    for row_index in range(height):
        row = bytearray(data[row_index * step:row_index * step + row_bytes])
        if encoding == "bgr8":
            for index in range(0, len(row), 3):
                row[index], row[index + 2] = row[index + 2], row[index]
        elif encoding == "bgra8":
            for index in range(0, len(row), 4):
                row[index], row[index + 2] = row[index + 2], row[index]
        rows.append(b"\x00" + bytes(row))

    header = struct.pack(">IIBBBBB", width, height, 8, color_type, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", header) + png_chunk(b"IDAT", zlib.compress(b"".join(rows)))
    destination.write_bytes(png + png_chunk(b"IEND", b""))


def png_chunk(chunk_type: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + chunk_type + payload + struct.pack(">I", zlib.crc32(chunk_type + payload) & 0xffffffff)


def write_pointcloud_pcd(message: Any, destination: Path) -> int:
    """Write Apollo drivers.PointCloud as a standard, inspectable PCD 0.7 file."""
    points = list(getattr(message, "point", ()))
    if not points:
        raise PermanentImportError("Point cloud contains no points")

    with destination.open("w", encoding="ascii", newline="\n") as output:
        output.write("VERSION 0.7\n")
        output.write("FIELDS x y z intensity timestamp\n")
        output.write("SIZE 4 4 4 4 8\n")
        output.write("TYPE F F F U F\n")
        output.write("COUNT 1 1 1 1 1\n")
        output.write("WIDTH {}\n".format(len(points)))
        output.write("HEIGHT 1\n")
        output.write("VIEWPOINT 0 0 0 1 0 0 0\n")
        output.write("POINTS {}\n".format(len(points)))
        output.write("DATA ascii\n")
        for point in points:
            output.write("{} {} {} {} {}\n".format(
                float(getattr(point, "x", 0.0)),
                float(getattr(point, "y", 0.0)),
                float(getattr(point, "z", 0.0)),
                int(getattr(point, "intensity", 0)),
                float(getattr(point, "timestamp", 0.0)),
            ))
    return len(points)


def convert_raw_image_to_png_bytes(message: Any, data: bytes) -> bytes:
    """在内存中将raw image转换为PNG字节，避免磁盘IO"""
    width = int(getattr(message, "width", 0) or 0)
    height = int(getattr(message, "height", 0) or 0)
    encoding = str(getattr(message, "encoding", "") or "").lower()
    formats = {
        "mono8": (1, 0),
        "gray8": (1, 0),
        "rgb8": (3, 2),
        "bgr8": (3, 2),
        "rgba8": (4, 6),
        "bgra8": (4, 6),
    }
    if width <= 0 or height <= 0:
        raise PermanentImportError("Raw image has invalid dimensions {}x{}".format(width, height))
    if encoding not in formats:
        raise PermanentImportError("Unsupported raw image encoding: {}".format(encoding or "empty"))

    channels, color_type = formats[encoding]
    row_bytes = width * channels
    step = int(getattr(message, "step", 0) or row_bytes)
    if step < row_bytes or len(data) < step * height:
        raise PermanentImportError("Raw image data size does not match its dimensions")

    rows: list[bytes] = []
    for row_index in range(height):
        row = bytearray(data[row_index * step:row_index * step + row_bytes])
        if encoding == "bgr8":
            for index in range(0, len(row), 3):
                row[index], row[index + 2] = row[index + 2], row[index]
        elif encoding == "bgra8":
            for index in range(0, len(row), 4):
                row[index], row[index + 2] = row[index + 2], row[index]
        rows.append(b"\x00" + bytes(row))

    header = struct.pack(">IIBBBBB", width, height, 8, color_type, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", header) + png_chunk(b"IDAT", zlib.compress(b"".join(rows)))
    return png + png_chunk(b"IEND", b"")


def generate_pcd_content(message: Any) -> tuple[bytes, int]:
    """在内存中生成PCD文件内容，避免磁盘IO"""
    points = list(getattr(message, "point", ()))
    if not points:
        raise PermanentImportError("Point cloud contains no points")

    lines = [
        "VERSION 0.7\n",
        "FIELDS x y z intensity timestamp\n",
        "SIZE 4 4 4 4 8\n",
        "TYPE F F F U F\n",
        "COUNT 1 1 1 1 1\n",
        "WIDTH {}\n".format(len(points)),
        "HEIGHT 1\n",
        "VIEWPOINT 0 0 0 1 0 0 0\n",
        "POINTS {}\n".format(len(points)),
        "DATA ascii\n"
    ]

    for point in points:
        lines.append("{} {} {} {} {}\n".format(
            float(getattr(point, "x", 0.0)),
            float(getattr(point, "y", 0.0)),
            float(getattr(point, "z", 0.0)),
            int(getattr(point, "intensity", 0)),
            float(getattr(point, "timestamp", 0.0)),
        ))

    content = "".join(lines).encode("ascii")
    return content, len(points)


def ensure_consumer_group(client: redis.Redis, stream: str, group: str) -> None:
    try:
        client.xgroup_create(stream, group, id="0", mkstream=True)
    except redis.exceptions.ResponseError as exc:
        if "BUSYGROUP" not in str(exc):
            raise


def main() -> int:
    logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"), format="%(asctime)s %(levelname)s %(message)s")
    config = Config.from_env()
    minio_client = Minio(config.minio_endpoint, access_key=config.minio_access_key,
                         secret_key=config.minio_secret_key, secure=config.minio_secure)
    backend = BackendClient(config)
    importer = RecordImporter(config, minio_client, backend)
    queue = redis.Redis.from_url(config.redis_url, decode_responses=True, health_check_interval=30)
    ensure_consumer_group(queue, QUEUE_STREAM, QUEUE_GROUP)
    logging.info("Record import worker %s is waiting on %s", config.worker_id, QUEUE_STREAM)

    while True:
        entries = queue.xreadgroup(QUEUE_GROUP, config.worker_id, {QUEUE_STREAM: ">"}, count=1, block=30000)
        if not entries:
            continue
        for _, messages in entries:
            for message_id, fields in messages:
                task_id = int(fields["taskId"])
                try:
                    task = backend.claim(task_id, config.worker_id)
                except Exception:
                    # Claim did not establish a database lease. A later Java reconciliation
                    # republishes QUEUED tasks, so do not report this task as failed.
                    logging.exception("Could not claim Record import task %s", task_id)
                    queue.xack(QUEUE_STREAM, QUEUE_GROUP, message_id)
                    continue
                try:
                    # The database lease now owns recovery; Redis only wakes the worker.
                    queue.xack(QUEUE_STREAM, QUEUE_GROUP, message_id)
                    if task is None:
                        continue
                    logging.info("Starting Record import task %s", task_id)
                    importer.run(task)
                    backend.complete(task_id, config.worker_id)
                    logging.info("Completed Record import task %s", task_id)
                except PermanentImportError as exc:
                    logging.exception("Permanent Record import failure for task %s", task_id)
                    backend.fail(task_id, config.worker_id, str(exc), retryable=False)
                except Exception as exc:  # Keep the worker alive and let Java decide retry policy.
                    logging.error("Record import failed for task %s: %s\n%s", task_id, exc, traceback.format_exc())
                    try:
                        backend.fail(task_id, config.worker_id, str(exc), retryable=True)
                    except Exception:
                        logging.exception("Could not report failure for task %s", task_id)


if __name__ == "__main__":
    raise SystemExit(main())
