import argparse
import os
import pandas as pd
import numpy as np
import joblib
import json
from pathlib import Path
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.svm import LinearSVC
from sklearn.metrics import accuracy_score, classification_report, f1_score
import warnings
import sys

warnings.filterwarnings('ignore')

# Trik agar modul src terbaca saat dieksekusi sebagai script
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from src.preprocessing import clean_text

RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)

def main():
    parser = argparse.ArgumentParser(description="Latih model deteksi bahasa.")
    parser.add_argument('--data-path', type=str, required=True, help="Path ke dataset CSV.")
    parser.add_argument('--output-dir', type=str, required=True, help="Direktori output artifact.")
    args = parser.parse_args()
    
    data_path = Path(args.data_path).resolve()
    output_dir = Path(args.output_dir).resolve()
    
    output_dir.mkdir(parents=True, exist_ok=True)
    
    print(f"Loading data dari {data_path}...")
    df = pd.read_csv(data_path)
    
    if 'text' not in df.columns or 'language' not in df.columns:
        raise ValueError("Dataset harus memiliki kolom 'text' dan 'language'")
        
    df['text'] = df['text'].fillna("")
    df['text'] = df['text'].apply(clean_text)
    
    # Filter valid
    df = df[df['text'] != ""]
    
    # Splitting logic (Group by source_file + text hash to avoid leaking sentences from same article)
    if 'document_id' in df.columns:
        df['group_id'] = df['document_id']
    else:
        # Fallback to pseudo-group if source_file exists
        if 'source_file' in df.columns:
            df['group_id'] = df['source_file'] + "_" + df['text'].str[:30]
        else:
            df['group_id'] = df.index.astype(str)
            
    unique_groups = df['group_id'].unique()
    np.random.shuffle(unique_groups)
    
    n_groups = len(unique_groups)
    train_idx = int(n_groups * 0.7)
    val_idx = int(n_groups * 0.85)
    
    train_groups = set(unique_groups[:train_idx])
    val_groups = set(unique_groups[train_idx:val_idx])
    test_groups = set(unique_groups[val_idx:])
    
    df_train = df[df['group_id'].isin(train_groups)]
    df_val = df[df['group_id'].isin(val_groups)]
    df_test = df[df['group_id'].isin(test_groups)]
    
    print(f"Train size: {len(df_train)}, Val size: {len(df_val)}, Test size: {len(df_test)}")
    
    X_train, y_train = df_train['text'], df_train['language']
    X_val, y_val = df_val['text'], df_val['language']
    X_test, y_test = df_test['text'], df_test['language']
    
    print("Training TF-IDF Vectorizer...")
    # Best config from v2: char_wb, ngram=(2,5)
    vec = TfidfVectorizer(analyzer='char_wb', ngram_range=(2,5), max_features=100000)
    X_tr = vec.fit_transform(X_train)
    X_t = vec.transform(X_test)
    
    print("Training LinearSVC...")
    clf = LinearSVC(C=2.0, class_weight='balanced', max_iter=1000, random_state=RANDOM_SEED)
    clf.fit(X_tr, y_train)
    
    print("Evaluating...")
    y_pred = clf.predict(X_t)
    acc = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred, average='macro')
    
    labels_order = list(clf.classes_)
    print(f"Accuracy: {acc:.4f}")
    print(f"Macro F1: {f1:.4f}")
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred, labels=labels_order))
    
    print(f"Saving artifacts to {output_dir}...")
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
        'macro_f1': f1
    }
    with open(output_dir / 'config.json', 'w', encoding='utf-8') as f:
        json.dump(config, f)
        
    print("Selesai!")

if __name__ == '__main__':
    main()
