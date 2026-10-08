import pytest
from unittest.mock import patch
from fastapi.testclient import TestClient
from api.main import app

@pytest.fixture(scope="module")
def client():
    with TestClient(app) as c:
        yield c

def post_request(endpoint, payload, client=None):
    if client is not None:
        response = client.post(endpoint, json=payload)
        return response.status_code, response.json()
    with TestClient(app) as c:
        response = c.post(endpoint, json=payload)
        return response.status_code, response.json()

def test_predict_indonesian_success(client):
    payload = {
        "text": "Bapak presiden sedang berpidato di istana negara Jakarta hari ini."
    }
    status_code, data = post_request("/predict", payload, client)
    assert status_code == 200, f"Expected 200, got {status_code}"
    assert data["status"] == "success", f"Expected success, got {data['status']}"
    assert data["language_code"] == "indonesian", f"Expected indonesian, got {data['language_code']}"
    assert data["language_name"] == "Indonesia", f"Expected Indonesia, got {data['language_name']}"
    assert "margin_score" in data
    assert data["text_length"] == len(payload["text"])
    print("test_predict_indonesian_success: PASSED")

def test_predict_unknown_language(client):
    payload = {
        "text": "Po mësoj gjuhën lao. Kjo është një provë."
    }
    status_code, data = post_request("/predict", payload, client)
    assert status_code == 200, f"Expected 200, got {status_code}"
    assert data["status"] == "unknown", f"Expected unknown, got {data['status']}"
    assert data["language_code"] == "unknown", f"Expected unknown, got {data['language_code']}"
    assert data["language_name"] == "Bahasa tidak dikenali", f"Expected Bahasa tidak dikenali, got {data['language_name']}"
    assert "margin_score" in data
    assert data["text_length"] == len(payload["text"])
    print("test_predict_unknown_language: PASSED")

def test_predict_text_too_short(client):
    payload = {
        "text": "Pendek"
    }
    status_code, data = post_request("/predict", payload, client)
    assert status_code == 422, f"Expected 422, got {status_code}"
    assert "detail" in data
    print("test_predict_text_too_short: PASSED")

def test_predict_french_unknown_regression(client):
    """
    Regresi: Kalimat Prancis harus menghasilkan status unknown karena margin_score
    berada di bawah confidence threshold.
    """
    payload = {
        "text": "Bonjour, je suis en train d'apprendre la langue française aujourd'hui."
    }
    status_code, data = post_request("/predict", payload, client)
    assert status_code == 200, f"Expected 200, got {status_code}"
    assert data["status"] == "unknown", f"Expected unknown, got {data['status']}"
    assert data["language_code"] == "unknown", f"Expected unknown, got {data['language_code']}"
    assert data["language_name"] == "Bahasa tidak dikenali", f"Expected Bahasa tidak dikenali, got {data['language_name']}"
    assert data["margin_score"] < getattr(app.state, "threshold", -0.018028416157938577)
    assert data["text_length"] == len(payload["text"])
    print("test_predict_french_unknown_regression: PASSED")

def test_predict_german_unknown_below_threshold(client):
    """
    Kalimat Jerman harus menghasilkan unknown jika margin-nya berada di bawah threshold.
    """
    threshold = getattr(app.state, "threshold", -0.018028416157938577)
    payload = {
        "text": "Guten Morgen, ich lerne heute die deutsche Sprache."
    }
    with patch("api.main.predict_with_details", return_value=[{"label": "indonesian", "margin_score": threshold - 0.05}]):
        status_code, data = post_request("/predict", payload, client)
        assert status_code == 200, f"Expected 200, got {status_code}"
        assert data["status"] == "unknown", f"Expected unknown, got {data['status']}"
        assert data["language_code"] == "unknown", f"Expected unknown, got {data['language_code']}"
        assert data["language_name"] == "Bahasa tidak dikenali", f"Expected Bahasa tidak dikenali, got {data['language_name']}"
        assert data["margin_score"] == threshold - 0.05
    print("test_predict_german_unknown_below_threshold: PASSED")

def test_predict_korean_unknown_script(client):
    """
    Kalimat Korea harus tetap menghasilkan unknown melalui pemeriksaan Unicode script
    tanpa memanggil predict_with_details().
    """
    payload = {
        "text": "안녕하세요, 저는 오늘 한국어 문장을 테스트하고 있습니다."
    }
    with patch("api.main.predict_with_details") as mock_predict:
        status_code, data = post_request("/predict", payload, client)
        assert status_code == 200, f"Expected 200, got {status_code}"
        assert data["status"] == "unknown", f"Expected unknown, got {data['status']}"
        assert data["language_code"] == "unknown", f"Expected unknown, got {data['language_code']}"
        assert data["language_name"] == "Bahasa tidak dikenali", f"Expected Bahasa tidak dikenali, got {data['language_name']}"
        assert data["margin_score"] == 0.0
        assert data["text_length"] == len(payload["text"])
        mock_predict.assert_not_called()
    print("test_predict_korean_unknown_script: PASSED")

def test_predict_supported_languages_not_broken(client):
    """
    Memastikan 6 bahasa regional lainnya (Thai, Lao, Khmer, Vietnam, Melayu, Filipino/Tagalog)
    tetap menghasilkan success dengan label yang tepat.
    """
    threshold = getattr(app.state, "threshold", -0.018028416157938577)
    samples = [
        ("thai", "สวัสดี วันนี้ฉันกำลังทดสอบประโยคภาษาไทย", "Thai"),
        ("lao", "ສະບາຍດີ ມື້ນີ້ຂ້ອຍກຳລັງທົດສອບປະໂຫຍກພາສາລາວ", "Lao"),
        ("khmer", "សួស្តី ថ្ងៃនេះខ្ញុំកំពុងសាកល្បងប្រយោគភាសាខ្មែរ", "Khmer"),
        ("vietnamese", "Xin chào, hôm nay tôi sedang thử nghiệm các câu tiếng Việt.", "Vietnam"),
        ("malay", "Ini adalah kenyataan rasmi daripada kerajaan Malaysia pada hari ini.", "Melayu"),
        ("fil_tl", "Ang pelikulang ito ay napakaganda at kawili-wili para sa lahat.", "Filipino/Tagalog"),
    ]
    for lang_code, text, lang_name in samples:
        status_code, data = post_request("/predict", {"text": text}, client)
        assert status_code == 200, f"Failed for {lang_code}: HTTP {status_code}"
        assert data["status"] == "success", f"Failed for {lang_code}: {data}"
        assert data["language_code"] == lang_code, f"Expected {lang_code}, got {data['language_code']}"
        assert data["language_name"] == lang_name, f"Expected {lang_name}, got {data['language_name']}"
        assert data["margin_score"] >= threshold, f"Margin {data['margin_score']} below threshold for {lang_code}"
    print("test_predict_supported_languages_not_broken: PASSED")

def test_confidence_threshold_mock_below(client):
    """
    Mock predict_with_details dengan margin di bawah threshold -> unknown
    """
    threshold = getattr(app.state, "threshold", -0.018028416157938577)
    payload = {"text": "Contoh kalimat untuk pengujian confidence threshold di bawah."}
    with patch("api.main.predict_with_details", return_value=[{"label": "fil_tl", "margin_score": threshold - 0.01}]):
        status_code, data = post_request("/predict", payload, client)
        assert status_code == 200
        assert data["status"] == "unknown"
        assert data["language_code"] == "unknown"
        assert data["language_name"] == "Bahasa tidak dikenali"
        assert data["margin_score"] == threshold - 0.01
    print("test_confidence_threshold_mock_below: PASSED")

def test_confidence_threshold_mock_equal(client):
    """
    Mock predict_with_details dengan margin sama dengan threshold -> tetap dianggap valid (success)
    """
    threshold = getattr(app.state, "threshold", -0.018028416157938577)
    payload = {"text": "Contoh kalimat untuk pengujian confidence threshold sama dengan."}
    with patch("api.main.predict_with_details", return_value=[{"label": "fil_tl", "margin_score": threshold}]):
        status_code, data = post_request("/predict", payload, client)
        assert status_code == 200
        assert data["status"] == "success"
        assert data["language_code"] == "fil_tl"
        assert data["language_name"] == "Filipino/Tagalog"
        assert data["margin_score"] == threshold
    print("test_confidence_threshold_mock_equal: PASSED")

def test_confidence_threshold_mock_above(client):
    """
    Mock predict_with_details dengan margin di atas threshold -> success
    """
    threshold = getattr(app.state, "threshold", -0.018028416157938577)
    payload = {"text": "Contoh kalimat untuk pengujian confidence threshold di atas."}
    with patch("api.main.predict_with_details", return_value=[{"label": "fil_tl", "margin_score": threshold + 0.01}]):
        status_code, data = post_request("/predict", payload, client)
        assert status_code == 200
        assert data["status"] == "success"
        assert data["language_code"] == "fil_tl"
        assert data["language_name"] == "Filipino/Tagalog"
        assert data["margin_score"] == threshold + 0.01
    print("test_confidence_threshold_mock_above: PASSED")

def test_confidence_threshold_dynamic_app_state(client):
    """
    Memastikan endpoint membaca threshold secara dinamis dari app.state.threshold
    """
    original_threshold = getattr(app.state, "threshold", -0.018028416157938577)
    payload = {"text": "Contoh kalimat untuk pengujian dinamis app state threshold."}
    try:
        app.state.threshold = 0.50
        with patch("api.main.predict_with_details", return_value=[{"label": "indonesian", "margin_score": 0.30}]):
            status_code, data = post_request("/predict", payload, client)
            assert status_code == 200
            assert data["status"] == "unknown"
            assert data["margin_score"] == 0.30
    finally:
        app.state.threshold = original_threshold
    print("test_confidence_threshold_dynamic_app_state: PASSED")

if __name__ == "__main__":
    print("Running API tests...")
    with TestClient(app) as test_c:
        test_predict_indonesian_success(test_c)
        test_predict_unknown_language(test_c)
        test_predict_text_too_short(test_c)
        test_predict_french_unknown_regression(test_c)
        test_predict_german_unknown_below_threshold(test_c)
        test_predict_korean_unknown_script(test_c)
        test_predict_supported_languages_not_broken(test_c)
        test_confidence_threshold_mock_below(test_c)
        test_confidence_threshold_mock_equal(test_c)
        test_confidence_threshold_mock_above(test_c)
        test_confidence_threshold_dynamic_app_state(test_c)
    print("All tests passed successfully.")
