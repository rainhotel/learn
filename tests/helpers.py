from contextlib import contextmanager
from pathlib import Path
import shutil
import uuid


@contextmanager
def workspace_tempdir():
    base = Path.cwd() / ".tmp-tests"
    base.mkdir(exist_ok=True)
    path = base / f"tmp-{uuid.uuid4().hex}"
    path.mkdir(parents=True, exist_ok=False)
    try:
        yield path
    finally:
        shutil.rmtree(path, ignore_errors=True)
