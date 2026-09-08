import pandas as pd
import numpy as np
import joblib
import json
from pathlib import Path
import sys

# Tentukan path relatif berdasarkan lokasi file ini (src/predict.py)
BASE_DIR = Path(__file__).resolve().parent.parent
ARTIFACTS_DIR = BASE_DIR / "artifacts"

# Trik agar modul src terbaca
sys.path.insert(0, str(BASE_DIR))

from src.preprocessing import clean_text

# Lazy loading of artifacts
_MODEL = None
_VECTORIZER = None
_LABELS = None

def load_artifacts():
    global _MODEL, _VECTORIZER, _LABELS
    if _MODEL is None:
        model_path = ARTIFACTS_DIR / "model.pkl"
        vec_path = ARTIFACTS_DIR / "preprocessor.pkl"
        labels_path = ARTIFACTS_DIR / "labels.json"
        
        if not model_path.exists() or not vec_path.exists() or not labels_path.exists():
            raise FileNotFoundError(f"Artifacts not found in {ARTIFACTS_DIR}. Silakan jalankan train.py terlebih dahulu.")
            
        _MODEL = joblib.load(model_path)
        _VECTORIZER = joblib.load(vec_path)
        
        with open(labels_path, 'r', encoding='utf-8') as f:
            _LABELS = json.load(f)

def _validate_and_prepare(input_df: pd.DataFrame) -> pd.Series:
    if not isinstance(input_df, pd.DataFrame):
        raise TypeError("Input harus berupa pandas DataFrame.")
    
    if 'text' not in input_df.columns:
        raise ValueError("DataFrame harus memiliki kolom 'text'.")
        
    texts = input_df['text'].fillna("").apply(clean_text)
    return texts

def predict(input_df: pd.DataFrame) -> list:
    """
    Mengembalikan list label hasil prediksi (tanpa threshold unknown).
    """
    load_artifacts()
    texts = _validate_and_prepare(input_df)
    
    # Jika semua teks kosong setelah dibersihkan
    if len(texts) == 0:
        return []
        
    X_vec = _VECTORIZER.transform(texts)
    preds = _MODEL.predict(X_vec)
    
    return preds.tolist()

def predict_with_details(input_df: pd.DataFrame) -> list:
    """
    Mengembalikan list dictionary berisi label dan nilai margin_score (decision_function distance).
    """
    load_artifacts()
    texts = _validate_and_prepare(input_df)
    
    if len(texts) == 0:
        return []
        
    X_vec = _VECTORIZER.transform(texts)
    preds = _MODEL.predict(X_vec)
    
    # LinearSVC tidak punya predict_proba, kita ambil decision_function max value
    if hasattr(_MODEL, "decision_function"):
        decision_scores = _MODEL.decision_function(X_vec)
        if len(decision_scores.shape) == 1:
            # Binary classification (seharusnya multi-class untuk proyek ini)
            margin_score = np.abs(decision_scores)
        else:
            margin_score = np.max(decision_scores, axis=1)
    else:
        # Fallback jika model diganti ke yang punya proba
        probs = _MODEL.predict_proba(X_vec)
        margin_score = np.max(probs, axis=1)
        
    results = []
    for pred, conf in zip(preds, margin_score):
        results.append({
            "label": pred,
            "margin_score": float(conf)
        })
        
    return results

def get_active_languages() -> list:
    """Mengembalikan daftar label bahasa dari model yang dimuat."""
    load_artifacts()
    return _LABELS or []
