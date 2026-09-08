import argparse
import os
import pandas as pd
import numpy as np
import joblib
import json
from pathlib import Path
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.svm import LinearSVC
from sklearn.metrics import accuracy_score, classification_report, f1_score, confusion_matrix
import warnings
import sys

warnings.filterwarnings('ignore')

BASE_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(BASE_DIR))

from src.preprocessing import clean_text
from src.database import get_feedbacks

RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)
MIN_SAMPLES_PER_LANGUAGE = 50

def main():
    parser = argparse.ArgumentParser(description="Active Learning: Retrain model dengan data feedback.")
    parser.add_argument('--base-data', type=str, required=True, help="Path ke dataset utama (CSV).")
    parser.add_argument('--output-dir', type=str, default="artifacts_candidate", help="Direktori output model kandidat.")
    args = parser.parse_args()
    
    base_data_path = Path(args.base_data).resolve()
    output_dir = BASE_DIR / args.output_dir
    output_dir.mkdir(parents=True, exist_ok=True)
    
    print(f"Loading data utama dari {base_data_path}...")
    df_base = pd.read_csv(base_data_path)
    
    if 'text' not in df_base.columns or 'language' not in df_base.columns:
        raise ValueError("Dataset utama harus memiliki kolom 'text' dan 'language'")
        
    df_base['text'] = df_base['text'].fillna("").apply(clean_text)
    df_base = df_base[df_base['text'] != ""]
    
    print("Mengambil data feedback berstatus APPROVED...")
    feedbacks = get_feedbacks(status='APPROVED')
    
    if feedbacks:
        df_fb = pd.DataFrame(feedbacks)
        # Rename columns to match base dataset
        df_fb = df_fb[['text', 'corrected_language']]
        df_fb.rename(columns={'corrected_language': 'language'}, inplace=True)
        df_fb['text'] = df_fb['text'].fillna("").apply(clean_text)
        df_fb = df_fb[df_fb['text'] != ""]
        
        print(f"Menambahkan {len(df_fb)} baris feedback.")
        df = pd.concat([df_base, df_fb], ignore_index=True)
    else:
        print("Tidak ada feedback APPROVED yang ditemukan. Hanya menggunakan dataset utama.")
        df = df_base
        
    # Check minimum samples
    lang_counts = df['language'].value_counts()
    valid_langs = lang_counts[lang_counts >= MIN_SAMPLES_PER_LANGUAGE].index.tolist()
    
    df = df[df['language'].isin(valid_langs)]
    print(f"Distribusi bahasa (>= {MIN_SAMPLES_PER_LANGUAGE} sampel):\n{df['language'].value_counts()}")
    
    # Splitting logic
    unique_texts = df['text'].unique()
    np.random.shuffle(unique_texts)
    
    n_texts = len(unique_texts)
    train_idx = int(n_texts * 0.7)
    val_idx = int(n_texts * 0.85)
    
    train_texts = set(unique_texts[:train_idx])
    val_texts = set(unique_texts[train_idx:val_idx])
    test_texts = set(unique_texts[val_idx:])
    
    df_train = df[df['text'].isin(train_texts)]
    df_val = df[df['text'].isin(val_texts)]
    df_test = df[df['text'].isin(test_texts)]
    
    print(f"Train size: {len(df_train)}, Val size: {len(df_val)}, Test size: {len(df_test)}")
    
    X_train, y_train = df_train['text'], df_train['language']
    X_val, y_val = df_val['text'], df_val['language']
    X_test, y_test = df_test['text'], df_test['language']
    
    print("Training TF-IDF Vectorizer...")
    vec = TfidfVectorizer(analyzer='char_wb', ngram_range=(2,5), max_features=100000)
    X_tr = vec.fit_transform(X_train)
    X_t = vec.transform(X_test)
    
    print("Training LinearSVC...")
    clf = LinearSVC(C=2.0, class_weight='balanced', max_iter=1000, random_state=RANDOM_SEED)
    clf.fit(X_tr, y_train)
    
    print("Evaluating on Test Set...")
    y_pred = clf.predict(X_t)
    acc = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred, average='macro')
    
    labels_order = list(clf.classes_)
    print(f"Accuracy: {acc:.4f}")
    print(f"Macro F1: {f1:.4f}")
    
    print("\nClassification Report:")
    report = classification_report(y_test, y_pred, labels=labels_order, output_dict=True)
    print(classification_report(y_test, y_pred, labels=labels_order))
    
    print("\nConfusion Matrix:")
    cm = confusion_matrix(y_test, y_pred, labels=labels_order)
    print(cm)
    
    print(f"Saving candidate artifacts to {output_dir}...")
    joblib.dump(clf, output_dir / 'model.pkl')
    joblib.dump(vec, output_dir / 'preprocessor.pkl')
    
    with open(output_dir / 'labels.json', 'w', encoding='utf-8') as f:
        json.dump(labels_order, f)
        
    config = {
        'model_type': 'LinearSVC',
        'analyzer': 'char_wb',
        'ngram_range': [2, 5],
        'C': 2.0,
        'accuracy': acc,
        'macro_f1': f1,
        'training_date': pd.Timestamp.now().isoformat(),
        'n_samples_per_lang': df['language'].value_counts().to_dict(),
        'classification_report': report,
        'confusion_matrix': cm.tolist()
    }
    with open(output_dir / 'config.json', 'w', encoding='utf-8') as f:
        json.dump(config, f)
        
    print("Retraining Selesai! Model kandidat siap dievaluasi.")

if __name__ == '__main__':
    main()
