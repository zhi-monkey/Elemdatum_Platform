#!/usr/bin/env python3
import argparse
import copy
import io
import json
import os
from datetime import datetime, timezone
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse
import zipfile


RAW_TASKS_TEMPLATE = [
    {
        "taskid": "ab34fb88-331b-11f1-bccd-0242ac110003",
        "devName": "Entrance Camera",
        "input_streamurl": "rtsp://127.0.0.1/live/cam-001",
        "running": "true",
        "cltstart": "true",
        "isdeleted": False,
        "empty_download": False,
        "cltnum": 26,
    },
    {
        "taskid": "ab33080a-331b-11f1-bccd-0242ac110003",
        "devName": "Warehouse Camera",
        "input_streamurl": "rtsp://127.0.0.1/live/cam-002",
        "running": "false",
        "cltstart": "true",
        "isdeleted": False,
        "empty_download": False,
        "cltnum": 0,
    },
    {
        "taskid": "ab35f02a-331b-11f1-bccd-0242ac110003",
        "devName": "Parking Camera",
        "input_streamurl": "rtsp://127.0.0.1/live/cam-003",
        "running": "true",
        "cltstart": "true",
        "isdeleted": False,
        "empty_download": False,
        "cltnum": 10,
    },
    {
        "taskid": "ab36d2f0-331b-11f1-bccd-0242ac110003",
        "devName": "Gate Camera",
        "input_streamurl": "rtsp://127.0.0.1/live/cam-004",
        "running": "true",
        "cltstart": "true",
        "isdeleted": False,
        "empty_download": False,
        "cltnum": 5,
    },
]


ONE_PIXEL_JPEG = b"\xff\xd8" + (b"\x00" * 256) + b"\xff\xd9"
IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png"}


def load_local_images():
    image_dir = Path(__file__).resolve().parent / "images"
    images = []
    if not image_dir.exists():
        return images
    for file_path in sorted(image_dir.iterdir()):
        if file_path.is_file() and file_path.suffix.lower() in IMAGE_EXTENSIONS:
            images.append(file_path)
    return images


def to_bool(value, default=False):
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"1", "true", "yes", "y"}


class MockHttpCameraHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    auth_token = ""
    local_images = load_local_images()
    task_index = {}
    download_count = {}
    tasks = copy.deepcopy(RAW_TASKS_TEMPLATE)
    scenario = "normal"
    stop_after_downloads = 0

    def do_GET(self):
        if not self._authorize():
            return

        parsed = urlparse(self.path)
        path = parsed.path
        query = parse_qs(parsed.query)

        if path == "/dataclt/rawtask/infos":
            payload = {
                "issucceed": True,
                "data": [self._to_raw_task(item) for item in self.tasks],
            }
            self._send_json(HTTPStatus.OK, payload)
            return

        if path == "/dataclt/file/download":
            dir_name = (query.get("dir_name") or [""])[0]
            if not dir_name:
                self._send_json(HTTPStatus.BAD_REQUEST, {"message": "dir_name is required"})
                return
            task = self._find_task(dir_name)
            if task is None or to_bool(task.get("isdeleted"), False):
                self._send_json(HTTPStatus.NOT_FOUND, {"message": "task not found"})
                return

            if str(task.get("running", "false")).lower() != "true" or str(task.get("cltstart", "false")).lower() != "true":
                self._send_json(HTTPStatus.CONFLICT, {"message": "task is not active"})
                return

            self.download_count[dir_name] = self.download_count.get(dir_name, 0) + 1
            self._apply_scenario_after_download(task, self.download_count[dir_name])

            zip_bytes = self._build_snapshot_zip(dir_name, to_bool(task.get("empty_download"), False))
            self._send_binary(HTTPStatus.OK, zip_bytes, "application/zip")
            return

        if path == "/mock/admin/tasks":
            payload = {
                "scenario": self.scenario,
                "stopAfterDownloads": self.stop_after_downloads,
                "downloadCount": self.download_count,
                "data": [self._to_raw_task(item) for item in self.tasks],
            }
            self._send_json(HTTPStatus.OK, payload)
            return

        self._send_json(HTTPStatus.NOT_FOUND, {"message": "not found"})

    def do_POST(self):
        if not self._authorize():
            return

        parsed = urlparse(self.path)
        path = parsed.path
        query = parse_qs(parsed.query)

        if path == "/mock/admin/reset":
            self.tasks = copy.deepcopy(RAW_TASKS_TEMPLATE)
            self.task_index = {}
            self.download_count = {}
            self._send_json(HTTPStatus.OK, {"message": "reset ok"})
            return

        if path == "/mock/admin/set-state":
            taskid = (query.get("taskid") or [""])[0]
            task = self._find_task(taskid)
            if not task:
                self._send_json(HTTPStatus.NOT_FOUND, {"message": "task not found"})
                return
            if "running" in query:
                task["running"] = "true" if to_bool((query.get("running") or [None])[0], False) else "false"
            if "cltstart" in query:
                task["cltstart"] = "true" if to_bool((query.get("cltstart") or [None])[0], False) else "false"
            if "isdeleted" in query:
                task["isdeleted"] = to_bool((query.get("isdeleted") or [None])[0], False)
            if "empty" in query:
                task["empty_download"] = to_bool((query.get("empty") or [None])[0], False)
            self._send_json(HTTPStatus.OK, {"message": "updated", "task": self._to_raw_task(task)})
            return

        self._send_json(HTTPStatus.NOT_FOUND, {"message": "not found"})

    def _find_task(self, taskid):
        for item in self.tasks:
            if item.get("taskid") == taskid:
                return item
        return None

    def _apply_scenario_after_download(self, task, count):
        if self.scenario == "stop_after_n" and self.stop_after_downloads > 0 and count >= self.stop_after_downloads:
            task["running"] = "false"
            task["cltstart"] = "false"
        if self.scenario == "deleted_after_n" and self.stop_after_downloads > 0 and count >= self.stop_after_downloads:
            task["isdeleted"] = True

    def _to_raw_task(self, item):
        return {
            "taskid": item["taskid"],
            "model": "raw",
            "input_streamurl": item.get("input_streamurl", ""),
            "output_streamurl": "",
            "running": item.get("running", "false"),
            "cltstart": item.get("cltstart", "false"),
            "interval": 10,
            "maxnum": 800,
            "t_min": 0.35,
            "t_max": 0.8,
            "devName": item.get("devName", ""),
            "cltnum": item.get("cltnum", 0),
            "isdeleted": item.get("isdeleted", False),
            "empty_download": item.get("empty_download", False),
            "issupported": True,
        }

    def _authorize(self):
        if not self.auth_token:
            return True
        auth_header = self.headers.get("Authorization", "")
        expected = f"Bearer {self.auth_token}"
        if auth_header == expected:
            return True
        self._send_json(HTTPStatus.UNAUTHORIZED, {"message": "unauthorized"})
        return False

    def _send_json(self, status, payload):
        data = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _send_binary(self, status, data, content_type):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _build_snapshot_zip(self, task_id, empty_download):
        timestamp_folder = datetime.now().strftime("%Y%m%d_%H%M%S")
        idx = self.task_index.get(task_id, 0)
        self.task_index[task_id] = idx + 1

        zip_buffer = io.BytesIO()
        with zipfile.ZipFile(zip_buffer, mode="w", compression=zipfile.ZIP_DEFLATED) as zf:
            if empty_download:
                return zip_buffer.getvalue()

            image_count = (idx % 3) + 1
            for i in range(image_count):
                image_idx = idx + i
                if self.local_images:
                    image_path = self.local_images[image_idx % len(self.local_images)]
                    with image_path.open("rb") as f:
                        image_bytes = f.read()
                    image_name = image_path.name
                else:
                    image_bytes = ONE_PIXEL_JPEG
                    image_name = f"{task_id}_{image_idx:06d}.jpg"

                if i % 2 == 0:
                    entry_name = f"{timestamp_folder}/{task_id}/{image_name}"
                else:
                    entry_name = f"{timestamp_folder}/nested/{task_id}/batch_{idx}/{image_name}"
                zf.writestr(entry_name, image_bytes)
        return zip_buffer.getvalue()

    def log_message(self, fmt, *args):
        return


def parse_args():
    parser = argparse.ArgumentParser(description="Mock HTTP camera source server")
    parser.add_argument("--host", default="127.0.0.1", help="Bind host")
    parser.add_argument("--port", type=int, default=18080, help="Bind port")
    parser.add_argument("--auth-token", default=os.getenv("MOCK_AUTH_TOKEN", ""), help="Optional bearer token")
    parser.add_argument(
        "--scenario",
        choices=["normal", "stop_after_n", "deleted_after_n"],
        default="normal",
        help="Mock runtime scenario",
    )
    parser.add_argument(
        "--stop-after-downloads",
        type=int,
        default=0,
        help="For scenario stop_after_n/deleted_after_n: switch status after N downloads",
    )
    return parser.parse_args()


def run_server(host, port, auth_token, scenario, stop_after_downloads):
    MockHttpCameraHandler.auth_token = auth_token.strip()
    MockHttpCameraHandler.scenario = scenario
    MockHttpCameraHandler.stop_after_downloads = max(0, int(stop_after_downloads))
    MockHttpCameraHandler.tasks = copy.deepcopy(RAW_TASKS_TEMPLATE)
    MockHttpCameraHandler.task_index = {}
    MockHttpCameraHandler.download_count = {}

    server = ThreadingHTTPServer((host, port), MockHttpCameraHandler)
    print(f"Mock HTTP source running at http://{host}:{port}")
    print("Endpoints: /dataclt/rawtask/infos, /dataclt/file/download?dir_name={taskid}&delRaw=true")
    print("Admin: GET /mock/admin/tasks, POST /mock/admin/set-state, POST /mock/admin/reset")
    print(f"Scenario: {scenario}, stopAfterDownloads: {MockHttpCameraHandler.stop_after_downloads}")
    if MockHttpCameraHandler.auth_token:
        print("Authorization required: Bearer <token>")
    if MockHttpCameraHandler.local_images:
        print(f"Loaded local snapshot images: {len(MockHttpCameraHandler.local_images)}")
    else:
        print("No local images found under ./images, fallback to built-in JPEG")
    server.serve_forever()


if __name__ == "__main__":
    args = parse_args()
    run_server(args.host, args.port, args.auth_token, args.scenario, args.stop_after_downloads)
