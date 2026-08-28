import os
import pandas as pd
import numpy as np
import time
import joblib
import json
import sys
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.svm import LinearSVC
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, f1_score, precision_score, recall_score
import warnings
warnings.filterwarnings('ignore')

sys.stdout.reconfigure(encoding='utf-8')

RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)

def get_ood_texts():
    return [
        {"language": "English", "text": "This is a completely random English sentence for out of distribution testing."},
        {"language": "English", "text": "The quick brown fox jumps over the lazy dog."},
        {"language": "Japanese", "text": "これはテスト用の全くランダムな日本語の文章です。"},
        {"language": "Japanese", "text": "桜の花がとても綺麗に咲いています。"},
        {"language": "Korean", "text": "이것은 분포 외 테스트를 위한 완전히 무작위인 한국어 문장입니다."},
        {"language": "Korean", "text": "안녕하세요, 오늘 날씨가 참 좋네요."},
        {"language": "Mandarin", "text": "这是一个完全随机的中文句子，用于分布外测试。"},
        {"language": "Mandarin", "text": "我今天去超市买了一些苹果和香蕉。"},
        {"language": "Burmese", "text": "ဤသည်မှာ ဖြန့်ဖြူးမှုပြင်ပစမ်းသပ်မှုအတွက် လုံးဝကျပန်းမြန်မာစာကြောင်းဖြစ်သည်။"},
        {"language": "Burmese", "text": "မင်္ဂလာပါ၊ ဒီနေ့ နေကောင်းလား။"},
        {"language": "Tetum", "text": "Ida ne'e mak fraze Tetum ne'ebé aleatóriu tebes ba teste li'ur."},
        {"language": "Tetum", "text": "Bondia, oinsa ita nia loron ohin?"},
        {"language": "Javanese", "text": "Iki minangka ukara basa Jawa sing acak banget kanggo tes metu saka distribusi."},
        {"language": "Javanese", "text": "Sugeng enjang, kados pundi pawartosipun dinten menika?"},
        {"language": "Sundanese", "text": "Ieu mangrupikeun kalimah Sunda anu acak pisan pikeun uji coba di luar distribusi."},
        {"language": "Sundanese", "text": "Wilujeng enjing, kumaha damang sadayana?"}
    ]

def main():
    print("Loading verified dataset v2...")
    df = pd.read_csv('processed_v2/verified_dataset.csv')
    
    # 1. Pastikan label
    valid_labels = {'thai', 'khmer', 'fil_tl', 'vietnamese', 'indonesian', 'malay', 'lao'}
    actual_labels = set(df['language'].unique())
    print(f"Labels found: {actual_labels}")
    assert actual_labels == valid_labels, f"Labels do not match! Found {actual_labels}"
    
    # 2. Tampilkan 20 contoh acak setiap bahasa
    print("\n--- 20 CONTOH ACAK PER BAHASA ---")
    for lang in valid_labels:
        print(f"\n[{lang}]")
        sample_df = df[df['language'] == lang].sample(min(20, len(df[df['language'] == lang])), random_state=RANDOM_SEED)
        for i, row in sample_df.iterrows():
            print(f"  - {row['text'][:80]}...")
            
    # 3. Periksa kembali teks kosong, konflik label, dan duplikat
    initial_len = len(df)
    df = df.dropna(subset=['text'])
    df = df[df['text'].str.strip() != ""]
    
    df['norm_text'] = df['text'].str.lower()
    label_counts = df.groupby('norm_text')['language'].nunique()
    conflicts = label_counts[label_counts > 1].index
    df = df[~df['norm_text'].isin(conflicts)]
    df = df.drop_duplicates(subset=['norm_text'], keep='first')
    df = df.drop(columns=['norm_text'])
    
    final_len = len(df)
    print(f"\nPre-checks: Removed {initial_len - final_len} rows (empty/conflicts/dupes).")
    
    # 4 & 6. Group Split
    if 'document_id' not in df.columns and 'article_id' not in df.columns:
        print("\n[PERINGATAN] document_id / article_id tidak tersedia di dataset!")
        print("Membuat kolom 'group_id' sintetis berdasarkan 'source_file' dan hash dari 30 karakter pertama untuk menghindari tersebarnya teks dari paragraf/artikel yang sama.")
        # Create group id based on source file and beginning of text to group chunks of same article
        df['group_id'] = df['source_file'] + "_" + df['text'].str[:30]
    else:
        df['group_id'] = df.get('document_id', df.get('article_id'))
        
    print(f"Total Unique Groups: {df['group_id'].nunique()} over {len(df)} rows.")

    # 5. Group-aware split (70/15/15) manual implementation
    unique_groups = df['group_id'].unique()
    np.random.shuffle(unique_groups)
    
    n_groups = len(unique_groups)
    train_idx = int(n_groups * 0.70)
    val_idx = int(n_groups * 0.85)
    
    train_groups = set(unique_groups[:train_idx])
    val_groups = set(unique_groups[train_idx:val_idx])
    test_groups = set(unique_groups[val_idx:])
    
    df_train = df[df['group_id'].isin(train_groups)]
    df_val = df[df['group_id'].isin(val_groups)]
    df_test = df[df['group_id'].isin(test_groups)]
    
    print(f"Split sizes -> Train: {len(df_train)}, Val: {len(df_val)}, Test: {len(df_test)}")
    
    X_train, y_train = df_train['text'], df_train['language']
    X_val, y_val = df_val['text'], df_val['language']
    X_test, y_test = df_test['text'], df_test['language']
    
    # Experiments
    print("\n--- MENJALANKAN EKSPERIMEN ---")
    experiments = []
    
    # A. LogReg
    for ngram in [(2,5), (2,6), (3,6)]:
        for c in [1.0, 2.0, 4.0]:
            print(f"Running LogReg char_wb {ngram} C={c}...")
            vec = TfidfVectorizer(analyzer='char_wb', ngram_range=ngram, max_features=100000)
            X_tr = vec.fit_transform(X_train)
            X_v = vec.transform(X_val)
            
            clf = LogisticRegression(C=c, class_weight='balanced', max_iter=1000, random_state=RANDOM_SEED)
            clf.fit(X_tr, y_train)
            
            y_pred = clf.predict(X_v)
            macro_f1 = f1_score(y_val, y_pred, average='macro')
            
            experiments.append({
                'model_type': 'LogReg', 'ngram': ngram, 'C': c, 
                'val_macro_f1': macro_f1, 'clf': clf, 'vec': vec
            })
            
    # B. LinearSVC
    for ngram in [(2,5), (2,6), (3,6)]:
        for c in [0.5, 1.0, 2.0]:
            print(f"Running LinearSVC char_wb {ngram} C={c}...")
            vec = TfidfVectorizer(analyzer='char_wb', ngram_range=ngram, max_features=100000)
            X_tr = vec.fit_transform(X_train)
            X_v = vec.transform(X_val)
            
            clf = LinearSVC(C=c, class_weight='balanced', max_iter=1000, random_state=RANDOM_SEED)
            clf.fit(X_tr, y_train)
            
            y_pred = clf.predict(X_v)
            macro_f1 = f1_score(y_val, y_pred, average='macro')
            
            experiments.append({
                'model_type': 'LinearSVC', 'ngram': ngram, 'C': c, 
                'val_macro_f1': macro_f1, 'clf': clf, 'vec': vec
            })
            
    # Select Best Model
    best_exp = max(experiments, key=lambda x: x['val_macro_f1'])
    print(f"\n--- MODEL TERBAIK ---")
    print(f"Type: {best_exp['model_type']}, Ngram: {best_exp['ngram']}, C: {best_exp['C']}, Val Macro F1: {best_exp['val_macro_f1']:.4f}")
    
    best_clf = best_exp['clf']
    best_vec = best_exp['vec']
    
    # Calibration of Unknown Threshold on Validation Set
    print("\n--- KALIBRASI THRESHOLD UNKNOWN ---")
    X_v = best_vec.transform(X_val)
    
    if hasattr(best_clf, "predict_proba"):
        val_probs = best_clf.predict_proba(X_v)
        val_max_probs = np.max(val_probs, axis=1)
    else:
        # For LinearSVC, we use decision_function as a confidence proxy
        val_probs = best_clf.decision_function(X_v)
        val_max_probs = np.max(val_probs, axis=1)
    
    # We set threshold at 1st percentile of correct predictions confidence
    val_preds = best_clf.predict(X_v)
    correct_mask = val_preds == y_val
    correct_probs = val_max_probs[correct_mask]
    unknown_threshold = float(np.percentile(correct_probs, 1)) 
    print(f"Calculated Unknown Threshold (1st percentile of correct val confidences): {unknown_threshold:.4f}")
    
    # Test Evaluation
    print("\n--- EVALUASI TEST SET ---")
    start_time = time.time()
    X_t = best_vec.transform(X_test)
    if hasattr(best_clf, "predict_proba"):
        test_probs = best_clf.predict_proba(X_t)
        max_probs = np.max(test_probs, axis=1)
    else:
        test_probs = best_clf.decision_function(X_t)
        max_probs = np.max(test_probs, axis=1)
        
    test_preds = best_clf.predict(X_t)
    inf_time = time.time() - start_time
    avg_inf_time_ms = (inf_time / len(X_test)) * 1000
    print(f"Inference Time: {avg_inf_time_ms:.2f} ms / text")
    
    # Apply Threshold
    max_probs = np.max(test_probs, axis=1)
    test_preds_thresh = np.where(max_probs < unknown_threshold, 'unknown', test_preds)
    
    # 1. Metrics (ignoring unknown for core metrics or keeping it)
    acc = accuracy_score(y_test, test_preds)
    macro_p = precision_score(y_test, test_preds, average='macro')
    macro_r = recall_score(y_test, test_preds, average='macro')
    macro_f1 = f1_score(y_test, test_preds, average='macro')
    print(f"Accuracy: {acc:.4f}, Precision: {macro_p:.4f}, Recall: {macro_r:.4f}, F1: {macro_f1:.4f}")
    
    # 2 & 3. Class Report & CM
    labels_order = list(best_clf.classes_)
    print("\nClassification Report:")
    print(classification_report(y_test, test_preds, labels=labels_order))
    
    cm = confusion_matrix(y_test, test_preds, labels=labels_order)
    df_cm = pd.DataFrame(cm, index=labels_order, columns=labels_order)
    print("\nConfusion Matrix:")
    print(df_cm)
    
    df_test_res = pd.DataFrame({'text': X_test, 'true': y_test, 'pred': test_preds, 'conf': max_probs, 'char_len': X_test.str.len()})
    
    # 4. Indo vs Malay errors
    indo_malay = df_test_res[
        ((df_test_res['true'] == 'indonesian') & (df_test_res['pred'] == 'malay')) |
        ((df_test_res['true'] == 'malay') & (df_test_res['pred'] == 'indonesian'))
    ]
    print(f"\nKesalahan Indonesian vs Malay: {len(indo_malay)} sampel")
    print(indo_malay.head())
    
    # 5. fil_tl vs Indo/Malay
    fil_indo_malay = df_test_res[
        ((df_test_res['true'] == 'fil_tl') & (df_test_res['pred'].isin(['indonesian', 'malay']))) |
        ((df_test_res['true'].isin(['indonesian', 'malay'])) & (df_test_res['pred'] == 'fil_tl'))
    ]
    print(f"\nKesalahan Filipino/Tagalog vs Indo/Malay: {len(fil_indo_malay)} sampel")
    print(fil_indo_malay.head())
    
    # 6. By Length
    print("\n--- HASIL BERDASARKAN PANJANG TEKS ---")
    bins = [20, 50, 100, 200, np.inf]
    labels = ['20-49', '50-99', '100-199', '>=200']
    df_test_res['len_group'] = pd.cut(df_test_res['char_len'], bins=bins, labels=labels, right=False)
    for g in labels:
        g_df = df_test_res[df_test_res['len_group'] == g]
        if len(g_df) > 0:
            g_acc = accuracy_score(g_df['true'], g_df['pred'])
            print(f"Panjang {g} chars: Accuracy = {g_acc:.4f} (N={len(g_df)})")
            
    # 7 & 8. Confidence Dist
    print("\n--- DISTRIBUSI CONFIDENCE ---")
    correct = df_test_res[df_test_res['true'] == df_test_res['pred']]['conf']
    incorrect = df_test_res[df_test_res['true'] != df_test_res['pred']]['conf']
    print(f"Benar (Correct)   - Mean: {correct.mean():.4f}, Median: {correct.median():.4f}")
    print(f"Salah (Incorrect) - Mean: {incorrect.mean():.4f}, Median: {incorrect.median():.4f}")
    
    # OOD Testing
    print("\n--- PENGUJIAN OUT-OF-DISTRIBUTION (OOD) ---")
    ood_texts = get_ood_texts()
    df_ood = pd.DataFrame(ood_texts)
    X_ood = best_vec.transform(df_ood['text'])
    
    if hasattr(best_clf, "predict_proba"):
        ood_probs = best_clf.predict_proba(X_ood)
        ood_max_probs = np.max(ood_probs, axis=1)
    else:
        ood_probs = best_clf.decision_function(X_ood)
        ood_max_probs = np.max(ood_probs, axis=1)
        
    ood_preds = best_clf.predict(X_ood)
    
    df_ood['pred_raw'] = ood_preds
    df_ood['confidence'] = ood_max_probs
    df_ood['pred_thresholded'] = np.where(ood_max_probs < unknown_threshold, 'unknown', ood_preds)
    
    for i, row in df_ood.iterrows():
        print(f"[{row['language']}] {row['text'][:50]} -> Pred: {row['pred_thresholded']} (Conf: {row['confidence']:.4f})")
        
    # Save artifacts
    out_dir = 'artifacts_v2'
    os.makedirs(out_dir, exist_ok=True)
    joblib.dump(best_clf, os.path.join(out_dir, 'model.joblib'))
    joblib.dump(best_vec, os.path.join(out_dir, 'vectorizer.joblib'))
    
    with open(os.path.join(out_dir, 'labels.json'), 'w') as f:
        json.dump(labels_order, f)
        
    config = {
        'model_type': best_exp['model_type'],
        'analyzer': 'char_wb',
        'ngram_range': best_exp['ngram'],
        'C': best_exp['C'],
        'unknown_threshold': unknown_threshold
    }
    with open(os.path.join(out_dir, 'config.json'), 'w') as f:
        json.dump(config, f)
        
    metrics = {
        'accuracy': acc, 'macro_precision': macro_p, 'macro_recall': macro_r, 'macro_f1': macro_f1,
        'inference_time_ms': avg_inf_time_ms
    }
    with open(os.path.join(out_dir, 'metrics.json'), 'w') as f:
        json.dump(metrics, f)
        
    pd.DataFrame(classification_report(y_test, test_preds, labels=labels_order, output_dict=True)).transpose().to_csv(os.path.join(out_dir, 'classification_report.csv'))
    df_cm.to_csv(os.path.join(out_dir, 'confusion_matrix.csv'))
    
    df_errors = df_test_res[df_test_res['true'] != df_test_res['pred']]
    df_errors.to_csv(os.path.join(out_dir, 'prediction_errors.csv'), index=False)
    
    print("\nSemua artifact Model v2 telah disimpan di folder 'artifacts_v2'.")

if __name__ == "__main__":
    main()
