import sqlite3
import os
import hashlib
from pathlib import Path
from datetime import datetime, timezone
from typing import List, Dict, Any, Optional

BASE_DIR = Path(__file__).resolve().parent.parent
DB_PATH = BASE_DIR / "data" / "feedback.db"
FEEDBACK_HOURLY_LIMIT = 10
FEEDBACK_DAILY_LIMIT = 50
MAX_PENDING_FEEDBACK = 1000
MAX_TOTAL_FEEDBACK = 10000


class FeedbackRateLimitError(Exception):
    pass


class FeedbackCapacityError(Exception):
    pass


def get_db_path() -> Path:
    """Return the configured database path.

    Tests and deployments can point at an isolated database with
    ``FEEDBACK_DB_PATH``. The environment is read for every connection so a
    pytest fixture can safely switch databases after this module is imported.
    """
    configured_path = os.getenv("FEEDBACK_DB_PATH")
    if configured_path:
        return Path(configured_path).expanduser().resolve()
    return DB_PATH

def _get_connection():
    # Ensure data directory exists
    db_path = get_db_path()
    db_path.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(db_path), timeout=10)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys=ON")
    _initialize_schema(conn)
    return conn


def _initialize_schema(conn: sqlite3.Connection) -> None:
    conn.execute("""
        CREATE TABLE IF NOT EXISTS feedback (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            text TEXT NOT NULL,
            predicted_language TEXT NOT NULL,
            corrected_language TEXT NOT NULL,
            margin_score REAL,
            is_correct BOOLEAN NOT NULL,
            status TEXT NOT NULL CHECK(status IN ('PENDING', 'APPROVED', 'REJECTED')),
            timestamp TEXT NOT NULL,
            model_version TEXT NOT NULL
        )
    """)
    conn.execute("CREATE INDEX IF NOT EXISTS idx_status ON feedback(status)")
    conn.execute("""
        CREATE TABLE IF NOT EXISTS feedback_sources (
            feedback_id INTEGER PRIMARY KEY REFERENCES feedback(id) ON DELETE CASCADE,
            source_hash TEXT NOT NULL,
            text_hash TEXT NOT NULL UNIQUE
        )
    """)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS feedback_attempts (
            source_hash TEXT NOT NULL,
            period TEXT NOT NULL,
            bucket INTEGER NOT NULL,
            attempts INTEGER NOT NULL,
            PRIMARY KEY (source_hash, period, bucket)
        )
    """)
    conn.commit()


def init_db():
    """Build the feedback schema at the configured path if needed."""
    with _get_connection():
        pass

def save_feedback(
    text: str,
    predicted_language: str,
    corrected_language: str,
    margin_score: Optional[float],
    is_correct: bool,
    model_version: str,
    source_hash: Optional[str] = None,
) -> int:
    """Menyimpan feedback baru dengan status PENDING."""
    timestamp = datetime.now(timezone.utc).isoformat()
    with _get_connection() as conn:
        # Serialize quota checks and inserts across API workers.
        conn.execute("BEGIN IMMEDIATE")
        cursor = conn.cursor()

        if source_hash is not None:
            now = datetime.now(timezone.utc)
            hour_bucket = int(now.timestamp()) // 3600
            day_bucket = int(now.timestamp()) // 86400
            cursor.execute(
                "DELETE FROM feedback_attempts WHERE "
                "(period = 'hour' AND bucket < ?) OR "
                "(period = 'day' AND bucket < ?)",
                (hour_bucket - 24, day_bucket - 7),
            )
            for period, bucket, limit in (
                ("hour", hour_bucket, FEEDBACK_HOURLY_LIMIT),
                ("day", day_bucket, FEEDBACK_DAILY_LIMIT),
            ):
                row = cursor.execute(
                    "SELECT attempts FROM feedback_attempts "
                    "WHERE source_hash = ? AND period = ? AND bucket = ?",
                    (source_hash, period, bucket),
                ).fetchone()
                if row and row["attempts"] >= limit:
                    raise FeedbackRateLimitError("Batas pengiriman feedback tercapai.")
                cursor.execute(
                    "INSERT INTO feedback_attempts "
                    "(source_hash, period, bucket, attempts) VALUES (?, ?, ?, 1) "
                    "ON CONFLICT(source_hash, period, bucket) "
                    "DO UPDATE SET attempts = attempts + 1",
                    (source_hash, period, bucket),
                )

            text_hash = hashlib.sha256(text.encode("utf-8")).hexdigest()
            if cursor.execute(
                "SELECT 1 FROM feedback_sources WHERE text_hash = ?",
                (text_hash,),
            ).fetchone():
                return -1

            counts = cursor.execute(
                "SELECT COUNT(*) AS total, "
                "SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending "
                "FROM feedback"
            ).fetchone()
            if counts["total"] >= MAX_TOTAL_FEEDBACK or (
                counts["pending"] or 0
            ) >= MAX_PENDING_FEEDBACK:
                raise FeedbackCapacityError("Kapasitas feedback penuh.")
        
        # Check for simple duplicate (same text and correction within a short time frame, or just same text in PENDING)
        cursor.execute(
            "SELECT id FROM feedback WHERE text = ? AND corrected_language = ? AND status = 'PENDING'", 
            (text, corrected_language)
        )
        if cursor.fetchone():
            return -1 # Indicate duplicate
            
        cursor.execute("""
            INSERT INTO feedback (text, predicted_language, corrected_language, margin_score, is_correct, status, timestamp, model_version)
            VALUES (?, ?, ?, ?, ?, 'PENDING', ?, ?)
        """, (text, predicted_language, corrected_language, margin_score, is_correct, timestamp, model_version))
        feedback_id = cursor.lastrowid
        if source_hash is not None:
            cursor.execute(
                "INSERT INTO feedback_sources (feedback_id, source_hash, text_hash) "
                "VALUES (?, ?, ?)",
                (feedback_id, source_hash, text_hash),
            )
        conn.commit()
        return feedback_id

def get_feedbacks(status: Optional[str] = None) -> List[Dict[str, Any]]:
    """Mengambil list feedback, bisa di-filter berdasarkan status."""
    with _get_connection() as conn:
        cursor = conn.cursor()
        if status:
            cursor.execute("SELECT * FROM feedback WHERE status = ? ORDER BY timestamp DESC", (status,))
        else:
            cursor.execute("SELECT * FROM feedback ORDER BY timestamp DESC")
            
        rows = cursor.fetchall()
        return [dict(row) for row in rows]

def get_feedback_stats() -> Dict[str, Any]:
    """Mengembalikan statistik feedback (jumlah PENDING, APPROVED, REJECTED, dan distribusi bahasa)."""
    with _get_connection() as conn:
        cursor = conn.cursor()
        
        # Total counts by status
        cursor.execute("SELECT status, COUNT(*) as count FROM feedback GROUP BY status")
        status_counts = {row['status']: row['count'] for row in cursor.fetchall()}
        
        # Total counts of corrected languages that are APPROVED
        cursor.execute("SELECT corrected_language, COUNT(*) as count FROM feedback WHERE status = 'APPROVED' GROUP BY corrected_language")
        language_approved = {row['corrected_language']: row['count'] for row in cursor.fetchall()}
        
        return {
            "status_counts": status_counts,
            "approved_by_language": language_approved
        }

def update_feedback_status(feedback_id: int, new_status: str) -> bool:
    """Memperbarui status feedback (misal dari PENDING ke APPROVED/REJECTED)."""
    if new_status not in ('PENDING', 'APPROVED', 'REJECTED'):
        raise ValueError("Status tidak valid.")
        
    with _get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("UPDATE feedback SET status = ? WHERE id = ?", (new_status, feedback_id))
        conn.commit()
        return cursor.rowcount > 0

# Inisialisasi db saat modul dimuat
init_db()
