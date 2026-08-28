import os
import re
import pandas as pd
import numpy as np
import unicodedata
from bs4 import BeautifulSoup
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.svm import LinearSVC
from sklearn.metrics import accuracy_score, f1_score, classification_report, confusion_matrix
import time
import joblib
import json
import warnings
warnings.filterwarnings('ignore')

RANDOM_SEED = 42

datasets_info = [
    {"file": "acehnese.csv", "text_col": "acehnese", "label": "acehnese", "encoding": "ascii"},
    {"file": "balinese.csv", "text_col": "balinese", "label": "balinese", "encoding": "ascii"},
    {"file": "banjarese.csv", "text_col": "banjarese", "label": "banjarese", "encoding": "ascii"},
    {"file": "buginese.csv", "text_col": "buginese", "label": "buginese", "encoding": "ascii"},
    {"file": "madurese.csv", "text_col": "madurese", "label": "madurese", "encoding": "utf-8"},
    {"file": "minangkabau.csv", "text_col": "minangkabau", "label": "minangkabau", "encoding": "ascii"},
    {"file": "ngaju.csv", "text_col": "ngaju", "label": "ngaju", "encoding": "ascii"},
    {"file": "sundanese.csv", "text_col": "sundanese", "label": "sundanese", "encoding": "ascii"},
    {"file": "toba_batak.csv", "text_col": "toba_batak", "label": "toba_batak", "encoding": "ascii"},
    {"file": "thai-government-corpus.csv", "text_col": "text", "label": "thai", "encoding": "utf-8"}
]

dataset_dir = "datasets"
processed_dir = "processed"
os.makedirs(processed_dir, exist_ok=True)

dataframes = []

for info in datasets_info:
    file_path = os.path.join(dataset_dir, info["file"])
    try:
        df_temp = pd.read_csv(file_path, encoding=info["encoding"])
        df_temp = df_temp[[info["text_col"]]].copy()
        df_temp.columns = ["text"]
        df_temp["label"] = info["label"]
        dataframes.append(df_temp)
    except Exception as e:
        print(f"Error loading {info['file']}: {e}")

df_all = pd.concat(dataframes, ignore_index=True)

# 1. Cleaning
def clean_text(text):
    if not isinstance(text, str): return ""
    # Hapus HTML
    if bool(BeautifulSoup(text, "html.parser").find()):
        text = BeautifulSoup(text, "html.parser").get_text()
    
    text = unicodedata.normalize('NFKC', text)
    text = text.strip().lower()
    
    # Hapus jika hanya angka/simbol
    if re.match(r'^[\W\d_]+$', text):
        return ""
    
    return text

df_all['clean_text'] = df_all['text'].apply(clean_text)
df_all = df_all[df_all['clean_text'] != ""]

# Handle duplicate clean_text with different labels
label_counts_per_text = df_all.groupby('clean_text')['label'].nunique()
conflicting_texts = label_counts_per_text[label_counts_per_text > 1].index
# Remove conflicting texts
df_all = df_all[~df_all['clean_text'].isin(conflicting_texts)]

# Remove normalized duplicate (keep first)
df_all = df_all.drop_duplicates(subset=['clean_text'])

# Final Data
df_final = df_all[['clean_text', 'label']].rename(columns={'clean_text': 'text'})

print("--- JUMLAH DATA SETELAH PEMBERSIHAN ---")
dist_clean = df_final['label'].value_counts()
print(dist_clean)

needs_more_data = []
for lang, count in dist_clean.items():
    if count < 1000:
        needs_more_data.append(lang)

print("\n--- MEMBUTUHKAN DATASET TAMBAHAN (< 1000) ---")
print(needs_more_data)

# Save processed dataset
df_final.to_csv(os.path.join(processed_dir, "cleaned_dataset.csv"), index=False)

# Stratified Split (70/15/15)
df_temp, df_test = train_test_split(df_final, test_size=0.15, random_state=RANDOM_SEED, stratify=df_final['label'])
df_train, df_val = train_test_split(df_temp, test_size=(0.15/0.85), random_state=RANDOM_SEED, stratify=df_temp['label'])

# Save splits
df_train.to_csv(os.path.join(processed_dir, "train.csv"), index=False)
df_val.to_csv(os.path.join(processed_dir, "val.csv"), index=False)
df_test.to_csv(os.path.join(processed_dir, "test.csv"), index=False)

print("\n--- EKSPERIMEN MODEL ---")
X_train, y_train = df_train['text'], df_train['label']
X_val, y_val = df_val['text'], df_val['label']
X_test, y_test = df_test['text'], df_test['label']

experiments = []

# A. Char TF-IDF + Logistic Regression
for analyzer in ['char', 'char_wb']:
    for ngram_range in [(2,5), (2,6), (3,6)]:
        for c_val in [0.5, 1.0, 2.0, 4.0]:
            exp_name = f"LogReg_{analyzer}_{ngram_range}_C={c_val}"
            
            start_time = time.time()
            vectorizer = TfidfVectorizer(analyzer=analyzer, ngram_range=ngram_range, lowercase=True, max_features=100000)
            X_train_vec = vectorizer.fit_transform(X_train)
            X_val_vec = vectorizer.transform(X_val)
            
            model = LogisticRegression(C=c_val, class_weight='balanced', random_state=RANDOM_SEED, max_iter=1000)
            model.fit(X_train_vec, y_train)
            train_time = time.time() - start_time
            
            val_preds = model.predict(X_val_vec)
            acc = accuracy_score(y_val, val_preds)
            f1 = f1_score(y_val, val_preds, average='macro')
            
            # Est model size (rough approximation from coefs and vocab)
            est_size_mb = (model.coef_.nbytes + sum(len(k.encode('utf-8')) for k in vectorizer.vocabulary_.keys())) / (1024*1024)
            
            experiments.append({
                "Model": "Logistic Regression",
                "Analyzer": analyzer,
                "Ngram": str(ngram_range),
                "C": c_val,
                "Val Accuracy": acc,
                "Val Macro F1": f1,
                "Train Time (s)": train_time,
                "Est Size (MB)": est_size_mb,
                "vectorizer": vectorizer,
                "model": model
            })

# B. Char TF-IDF + LinearSVC
for analyzer in ['char', 'char_wb']:
    for ngram_range in [(2,5), (2,6), (3,6)]:
        for c_val in [0.5, 1.0, 2.0]:
            exp_name = f"LinearSVC_{analyzer}_{ngram_range}_C={c_val}"
            
            start_time = time.time()
            vectorizer = TfidfVectorizer(analyzer=analyzer, ngram_range=ngram_range, lowercase=True, max_features=100000)
            X_train_vec = vectorizer.fit_transform(X_train)
            X_val_vec = vectorizer.transform(X_val)
            
            model = LinearSVC(C=c_val, class_weight='balanced', random_state=RANDOM_SEED, max_iter=1000)
            model.fit(X_train_vec, y_train)
            train_time = time.time() - start_time
            
            val_preds = model.predict(X_val_vec)
            acc = accuracy_score(y_val, val_preds)
            f1 = f1_score(y_val, val_preds, average='macro')
            
            est_size_mb = (model.coef_.nbytes + sum(len(k.encode('utf-8')) for k in vectorizer.vocabulary_.keys())) / (1024*1024)
            
            experiments.append({
                "Model": "LinearSVC",
                "Analyzer": analyzer,
                "Ngram": str(ngram_range),
                "C": c_val,
                "Val Accuracy": acc,
                "Val Macro F1": f1,
                "Train Time (s)": train_time,
                "Est Size (MB)": est_size_mb,
                "vectorizer": vectorizer,
                "model": model
            })

# Tabel Perbandingan
df_exp = pd.DataFrame(experiments)
df_results = df_exp.drop(columns=['vectorizer', 'model'])
df_results = df_results.sort_values(by="Val Macro F1", ascending=False)
print(df_results.to_string(index=False))

# Pilih model terbaik berdasarkan Validation Macro F1
best_idx = df_exp['Val Macro F1'].idxmax()
best_exp = df_exp.loc[best_idx]
best_model = best_exp['model']
best_vectorizer = best_exp['vectorizer']

print(f"\nModel Terbaik: {best_exp['Model']}, Analyzer: {best_exp['Analyzer']}, Ngram: {best_exp['Ngram']}, C: {best_exp['C']}")

# Evaluate on test set
X_test_vec = best_vectorizer.transform(X_test)
test_preds = best_model.predict(X_test_vec)

print("\n--- EVALUASI FINAL TEST SET ---")
test_acc = accuracy_score(y_test, test_preds)
test_f1 = f1_score(y_test, test_preds, average='macro')
print(f"Test Accuracy: {test_acc:.4f}")
print(f"Test Macro F1: {test_f1:.4f}")

clf_report = classification_report(y_test, test_preds)
print("\nClassification Report:\n", clf_report)

cm = confusion_matrix(y_test, test_preds, labels=best_model.classes_)
df_cm = pd.DataFrame(cm, index=best_model.classes_, columns=best_model.classes_)
print("\nConfusion Matrix:\n", df_cm)

# Contoh Kesalahan Prediksi
df_errors = pd.DataFrame({'text': X_test, 'actual': y_test, 'predicted': test_preds})
df_errors = df_errors[df_errors['actual'] != df_errors['predicted']]
print("\nContoh Kesalahan Prediksi:\n", df_errors.head(10))

# Simpan semua output
output_dir = "artifacts_final"
os.makedirs(output_dir, exist_ok=True)

joblib.dump(best_model, os.path.join(output_dir, "best_model.pkl"))
joblib.dump(best_vectorizer, os.path.join(output_dir, "best_vectorizer.pkl"))

with open(os.path.join(output_dir, "labels.json"), "w") as f:
    json.dump(list(best_model.classes_), f)

prep_config = {
    "analyzer": best_exp['Analyzer'],
    "ngram_range": best_exp['Ngram'],
    "lowercase": True,
    "max_features": 100000
}
with open(os.path.join(output_dir, "preprocessing_config.json"), "w") as f:
    json.dump(prep_config, f)

df_results.to_csv(os.path.join(output_dir, "experiment_results.csv"), index=False)

with open(os.path.join(output_dir, "classification_report.txt"), "w") as f:
    f.write(clf_report)

df_cm.to_csv(os.path.join(output_dir, "confusion_matrix.csv"))

df_errors.to_csv(os.path.join(output_dir, "prediction_errors.csv"), index=False)

print("\nSemua artifact berhasil disimpan di folder 'artifacts_final' dan 'processed'.")
