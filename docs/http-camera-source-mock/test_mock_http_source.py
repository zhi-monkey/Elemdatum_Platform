import json
import socket
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen


def free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


class MockHttpSourceTest(unittest.TestCase):
    def setUp(self):
        self.base_dir = Path(__file__).resolve().parent
        self.port = free_port()
        self.base_url = f"http://127.0.0.1:{self.port}"
        self.token = "demo-token"
        self.process = subprocess.Popen(
            [
                sys.executable,
                str(self.base_dir / "mock_http_source.py"),
                "--host",
                "127.0.0.1",
                "--port",
                str(self.port),
                "--auth-token",
                self.token,
            ],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            cwd=str(self.base_dir),
        )
        self._wait_until_ready()

    def tearDown(self):
        if self.process.poll() is None:
            self.process.terminate()
            try:
                self.process.wait(timeout=3)
            except subprocess.TimeoutExpired:
                self.process.kill()

    def _wait_until_ready(self):
        deadline = time.time() + 5
        headers = {"Authorization": f"Bearer {self.token}"}
        while time.time() < deadline:
            try:
                req = Request(self.base_url + "/dataclt/rawtask/infos", headers=headers)
                with urlopen(req, timeout=0.5) as resp:
                    if resp.status == 200:
                        return
            except Exception:
                time.sleep(0.1)
        self.fail("Mock server did not start in time")

    def test_rawtask_requires_auth(self):
        with self.assertRaises(HTTPError) as cm:
            urlopen(self.base_url + "/dataclt/rawtask/infos", timeout=1)
        self.assertEqual(cm.exception.code, 401)

    def test_list_raw_tasks(self):
        req = Request(
            self.base_url + "/dataclt/rawtask/infos",
            headers={"Authorization": f"Bearer {self.token}"},
        )
        with urlopen(req, timeout=1) as resp:
            data = json.loads(resp.read().decode("utf-8"))

        print("[rawtask/infos] response:", json.dumps(data, ensure_ascii=False), flush=True)
        self.assertTrue(data["issucceed"])
        self.assertGreaterEqual(len(data["data"]), 1)
        self.assertIn("taskid", data["data"][0])
        self.assertIn("devName", data["data"][0])

    def test_download_zip(self):
        info_req = Request(
            self.base_url + "/dataclt/rawtask/infos",
            headers={"Authorization": f"Bearer {self.token}"},
        )
        with urlopen(info_req, timeout=1) as resp:
            info = json.loads(resp.read().decode("utf-8"))
        task_id = info["data"][0]["taskid"]

        req = Request(
            self.base_url + f"/dataclt/file/download?dir_name={task_id}&delRaw=true",
            headers={"Authorization": f"Bearer {self.token}"},
        )
        with urlopen(req, timeout=1) as resp:
            data = resp.read()
            content_type = resp.headers.get("Content-Type", "")

        output_path = Path(tempfile.gettempdir()) / f"mock_http_source_{task_id}.zip"
        output_path.write_bytes(data)
        print(f"[file/download] saved zip: {output_path}", flush=True)
        print(f"[file/download] content-type: {content_type}, bytes: {len(data)}", flush=True)

        self.assertIn("application/zip", content_type)
        self.assertTrue(data.startswith(b"PK"))

    def test_download_without_dir_name(self):
        req = Request(
            self.base_url + "/dataclt/file/download",
            headers={"Authorization": f"Bearer {self.token}"},
        )
        with self.assertRaises(HTTPError) as cm:
            urlopen(req, timeout=1)
        print(f"[file/download no dir_name] status: {cm.exception.code}", flush=True)
        self.assertEqual(cm.exception.code, 400)


if __name__ == "__main__":
    unittest.main()
