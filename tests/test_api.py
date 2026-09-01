import urllib.request
import json

BASE_URL = "http://127.0.0.1:8000"

def post_request(endpoint, payload):
    url = f"{BASE_URL}{endpoint}"
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req) as response:
            return response.status, json.loads(response.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode())

def test_predict_indonesian_success():
    payload = {
        "text": "Bapak presiden sedang berpidato di istana negara Jakarta hari ini."
    }
    status_code, data = post_request("/predict", payload)
    assert status_code == 200, f"Expected 200, got {status_code}"
    assert data["status"] == "success", f"Expected success, got {data['status']}"
    assert data["language_code"] == "indonesian", f"Expected indonesian, got {data['language_code']}"
    assert data["language_name"] == "Indonesia", f"Expected Indonesia, got {data['language_name']}"
    assert "margin_score" in data
    assert data["text_length"] == len(payload["text"])
    print("test_predict_indonesian_success: PASSED")

def test_predict_unknown_language():
    payload = {
        "text": "Po mësoj gjuhën lao. Kjo është një provë."
    }
    status_code, data = post_request("/predict", payload)
    print("Response for Albanian:", data)
    assert status_code == 200, f"Expected 200, got {status_code}"
    assert data["status"] == "unknown", f"Expected unknown, got {data['status']}"
    assert data["language_code"] == "unknown", f"Expected unknown, got {data['language_code']}"
    assert data["language_name"] == "Bahasa tidak dikenali", f"Expected Bahasa tidak dikenali, got {data['language_name']}"
    assert "margin_score" in data
    assert data["text_length"] == len(payload["text"])
    print("test_predict_unknown_language: PASSED")

def test_predict_text_too_short():
    payload = {
        "text": "Pendek"
    }
    status_code, data = post_request("/predict", payload)
    assert status_code == 422, f"Expected 422, got {status_code}"
    assert "detail" in data
    print("test_predict_text_too_short: PASSED")

if __name__ == "__main__":
    print("Running API tests...")
    test_predict_unknown_language()
    test_predict_indonesian_success()
    test_predict_text_too_short()
    print("All tests passed successfully.")
