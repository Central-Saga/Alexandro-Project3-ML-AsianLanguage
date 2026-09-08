import sqlite3
import os
from pathlib import Path
from datetime import datetime
from typing import List, Dict, Any, Optional

BASE_DIR = Path(__file__).resolve().parent.parent
DB_PATH = BASE_DIR / "data" / "feedback.db"

def _get_connection():
    # Ensure data directory exists
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(DB_PATH))
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    """Membangun tabel feedback jika belum ada."""
    with _get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("""
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
        # Index to speed up status-based queries
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_status ON feedback(status)")
        # Index to prevent easy duplicates (optional, doing it in code is safer for now)
        conn.commit()

def save_feedback(
    text: str,
    predicted_language: str,
    corrected_language: str,
    margin_score: Optional[float],
    is_correct: bool,
    model_version: str
) -> int:
    """Menyimpan feedback baru dengan status PENDING."""
    timestamp = datetime.utcnow().isoformat()
    with _get_connection() as conn:
        cursor = conn.cursor()
        
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
        conn.commit()
        return cursor.lastrowid

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
