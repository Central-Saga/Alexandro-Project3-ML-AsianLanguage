import pytest
from fastapi.testclient import TestClient
from api.main import app
from src.database import get_feedbacks, _get_connection

@pytest.fixture(autouse=True)
def clean_db():
    # Clean feedback table before each test
    with _get_connection() as conn:
        conn.cursor().execute("DELETE FROM feedback")
        conn.commit()

def test_get_languages():
    with TestClient(app) as client:
        response = client.get("/languages")
        assert response.status_code == 200
        assert "languages" in response.json()

def test_submit_valid_feedback():
    with TestClient(app) as client:
        payload = {
            "text": "This is a valid long enough text for testing feedback.",
            "predicted_language": "Indonesian",
            "corrected_language": "English",
            "margin_score": 1.23,
            "is_correct": False,
            "model_version": "1.0.0"
        }
        response = client.post("/feedback", json=payload)
        assert response.status_code == 200
        assert response.json()["status"] == "success"
        
        feedbacks = get_feedbacks()
        assert len(feedbacks) == 1
        assert feedbacks[0]["status"] == "PENDING"
        assert feedbacks[0]["corrected_language"] == "english"

def test_submit_invalid_text_length():
    with TestClient(app) as client:
        payload = {
            "text": "Too short",
            "predicted_language": "Indonesian",
            "corrected_language": "English",
            "margin_score": 1.23,
            "is_correct": False,
            "model_version": "1.0.0"
        }
        response = client.post("/feedback", json=payload)
        assert response.status_code == 422
        
def test_submit_invalid_language_name():
    with TestClient(app) as client:
        payload = {
            "text": "This is a valid long enough text for testing feedback.",
            "predicted_language": "Indonesian",
            "corrected_language": "<script>alert(1)</script>",
            "margin_score": 1.23,
            "is_correct": False,
            "model_version": "1.0.0"
        }
        response = client.post("/feedback", json=payload)
        assert response.status_code == 422

def test_feedback_stats_unauthorized():
    with TestClient(app) as client:
        response = client.get("/feedback/stats")
        assert response.status_code == 401
        
def test_feedback_stats_authorized():
    with TestClient(app) as client:
        response = client.get(
            "/feedback/stats",
            headers={"authorization": "Bearer test-admin-token"},
        )
        assert response.status_code == 200


def test_feedback_display_name_is_canonicalized():
    with TestClient(app) as client:
        payload = {
            "text": "Ini adalah kalimat yang cukup panjang untuk pengujian label.",
            "predicted_language": "Indonesia",
            "corrected_language": "Filipino/Tagalog",
            "margin_score": 0.25,
            "is_correct": False,
            "model_version": "1.0.0",
        }
        response = client.post("/feedback", json=payload)
        assert response.status_code == 200

        feedbacks = get_feedbacks()
        assert feedbacks[0]["predicted_language"] == "indonesian"
        assert feedbacks[0]["corrected_language"] == "fil_tl"


def test_feedback_stats_requires_server_configuration(monkeypatch):
    monkeypatch.delenv("ADMIN_API_TOKEN")
    with TestClient(app) as client:
        response = client.get(
            "/feedback/stats",
            headers={"authorization": "Bearer test-admin-token"},
        )
        assert response.status_code == 503
