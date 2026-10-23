import os
from pathlib import Path
import shutil
import tempfile


# Configure isolation before pytest imports application modules. This prevents
# database tests from ever opening or deleting data/feedback.db.
_TEST_STATE_DIR = Path(tempfile.mkdtemp(prefix="language-detector-tests-"))
os.environ["FEEDBACK_DB_PATH"] = str(_TEST_STATE_DIR / "feedback.db")
os.environ["ADMIN_API_TOKEN"] = "test-admin-token"
os.environ["FEEDBACK_API_TOKEN"] = "test-feedback-token"


def pytest_sessionfinish(session, exitstatus):
    shutil.rmtree(_TEST_STATE_DIR, ignore_errors=True)
