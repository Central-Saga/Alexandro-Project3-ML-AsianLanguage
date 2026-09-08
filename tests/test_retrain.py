import pytest
import pandas as pd
from pathlib import Path
from src.database import save_feedback, update_feedback_status, _get_connection
import subprocess

@pytest.fixture(autouse=True)
def clean_db():
    with _get_connection() as conn:
        conn.cursor().execute("DELETE FROM feedback")
        conn.commit()

def test_retrain_script(tmp_path):
    # Buat dummy dataset dengan banyak kalimat unik agar split train/test berhasil
    dummy_data = {
        'text': [f"Saya makan nasi bagian ke {i}" for i in range(60)] + [f"I eat rice part {i}" for i in range(60)],
        'language': ['indonesian'] * 60 + ['english'] * 60
    }
    df = pd.DataFrame(dummy_data)
    dummy_csv = tmp_path / "dummy.csv"
    df.to_csv(dummy_csv, index=False)
    
    # Masukkan feedback APPROVED dengan kalimat unik
    for i in range(50):
        fid = save_feedback(f"Teks baru bahasa jawa yang panjang ke {i}", "indonesian", "javanese", 0.5, False, "1.0")
        update_feedback_status(fid, "APPROVED")
        
    # Jalankan script retrain
    result = subprocess.run([
        "python", "-m", "src.retrain",
        "--base-data", str(dummy_csv),
        "--output-dir", "artifacts_test"
    ], capture_output=True, text=True)
    
    print(result.stdout)
    print(result.stderr)
    assert result.returncode == 0
    assert "Retraining Selesai" in result.stdout
    
    # Cek apakah artifact candidate ada
    out_dir = Path("artifacts_test")
    assert (out_dir / "model.pkl").exists()
    assert (out_dir / "preprocessor.pkl").exists()
    assert (out_dir / "config.json").exists()
    
    # Bersihkan file artifact_test
    import shutil
    shutil.rmtree("artifacts_test")
