import argparse
import os
import pandas as pd
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
from src.model_metrics import calibrate_confidence_threshold
from src.splitting import stratified_group_split

RANDOM_SEED = 42
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
    
    # Downsample to avoid OOM with 16 languages
    df = df.groupby('language').sample(n=min(2500, len(df)), random_state=RANDOM_SEED, replace=True).reset_index(drop=True)
    
    # Identical normalized texts stay together while every split remains
    # balanced by language.
    df['group_id'] = df['text']
    train_texts, val_texts, test_texts = stratified_group_split(
        df,
        group_column='group_id',
        random_state=RANDOM_SEED,
    )
    
    df_train = df[df['group_id'].isin(train_texts)]
    df_val = df[df['group_id'].isin(val_texts)]
    df_test = df[df['group_id'].isin(test_texts)]
    
    print(f"Train size: {len(df_train)}, Val size: {len(df_val)}, Test size: {len(df_test)}")
    
    X_train, y_train = df_train['text'], df_train['language']
    X_val, y_val = df_val['text'], df_val['language']
    X_test, y_test = df_test['text'], df_test['language']
    
    print("Training TF-IDF Vectorizer...")
    vec = TfidfVectorizer(analyzer='char_wb', ngram_range=(1,3), min_df=5, max_features=40000)
    X_tr = vec.fit_transform(X_train)
    X_v = vec.transform(X_val)
    X_t = vec.transform(X_test)
    
    print("Training LinearSVC...")
    clf = LinearSVC(C=2.0, class_weight='balanced', max_iter=1000, random_state=RANDOM_SEED)
    clf.fit(X_tr, y_train)
    
    print("Evaluating on Test Set...")
    y_pred = clf.predict(X_t)
    acc = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred, average='macro')
    threshold = calibrate_confidence_threshold(clf, X_v)
    
    labels_order = list(clf.classes_)
    print(f"Accuracy: {acc:.4f}")
    print(f"Macro F1: {f1:.4f}")
    
    print("\nClassification Report:")
    report = classification_report(
        y_test,
        y_pred,
        labels=labels_order,
        output_dict=True,
        zero_division=0,
    )
    print(classification_report(
        y_test,
        y_pred,
        labels=labels_order,
        zero_division=0,
    ))
    
    print("\nConfusion Matrix:")
    cm = confusion_matrix(y_test, y_pred, labels=labels_order)
    print(cm)
    
    print(f"Saving candidate artifacts to {output_dir}...")
    joblib.dump(clf, output_dir / 'model.pkl')
    joblib.dump(vec, output_dir / 'preprocessor.pkl')
    
    with open(output_dir / 'labels.json', 'w', encoding='utf-8') as f:
        json.dump(labels_order, f)

    with open(output_dir / 'confidence_threshold.json', 'w', encoding='utf-8') as f:
        json.dump({
            'threshold': threshold,
            'score_type': 'winning class decision score',
            'source': 'validation in-distribution percentile 1',
        }, f)

    baseline_macro_f1 = None
    baseline_report = None
    active_dir = BASE_DIR / 'artifacts'
    active_model_path = active_dir / 'model.pkl'
    active_vectorizer_path = active_dir / 'preprocessor.pkl'
    if active_model_path.exists() and active_vectorizer_path.exists():
        print("Evaluating active model on the same candidate test set...")
        active_model = joblib.load(active_model_path)
        active_vectorizer = joblib.load(active_vectorizer_path)
        active_predictions = active_model.predict(active_vectorizer.transform(X_test))
        baseline_macro_f1 = f1_score(
            y_test,
            active_predictions,
            labels=labels_order,
            average='macro',
            zero_division=0,
        )
        baseline_report = classification_report(
            y_test,
            active_predictions,
            labels=labels_order,
            output_dict=True,
            zero_division=0,
        )
        print(f"Active model Macro F1 on candidate test set: {baseline_macro_f1:.4f}")
        
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
        'confusion_matrix': cm.tolist(),
        'confidence_threshold': threshold,
        'baseline_macro_f1': baseline_macro_f1,
        'baseline_classification_report': baseline_report,
    }
    with open(output_dir / 'config.json', 'w', encoding='utf-8') as f:
        json.dump(config, f)
        
    print("Retraining Selesai! Model kandidat siap dievaluasi.")

if __name__ == '__main__':
    main()
