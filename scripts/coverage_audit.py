import os
import glob
import pandas as pd
import numpy as np
import re
import csv
import json
import unicodedata
from collections import defaultdict

# Setup target languages (normalized to lowercase)
TARGET_LANGS = [
    'indonesian', 'malay', 'vietnamese', 'thai', 'tagalog', 'filipino', 
    'khmer', 'lao', 'burmese', 'tetum',
    'acehnese', 'balinese', 'banjarese', 'buginese', 'madurese', 
    'minangkabau', 'ngaju', 'sundanese', 'toba_batak'
]

def clean_text(text):
    if not isinstance(text, str): return ""
    text = unicodedata.normalize('NFKC', text).strip()
    if re.match(r'^[\W\d_]+$', text): return ""
    return text

def infer_metadata(filepath, filename):
    # Peek first few lines to infer structure
    ext = os.path.splitext(filename)[1].lower()
    
    tasks = [] # list of dicts: {'text_col': ..., 'lang': ..., 'lang_col': ...}
    
    if ext == '.csv':
        try:
            df_sample = pd.read_csv(filepath, nrows=10, encoding='utf-8', on_bad_lines='skip')
        except:
            try:
                df_sample = pd.read_csv(filepath, nrows=10, encoding='latin1', on_bad_lines='skip')
            except:
                return tasks
        
        cols = [c.lower() for c in df_sample.columns]
        
        # Check if columns are target languages (bilingual dictionary style)
        for c in df_sample.columns:
            if str(c).lower() in TARGET_LANGS:
                tasks.append({'text_col': c, 'lang': str(c).lower()})
        
        # Specific known files
        if 'thai-government-corpus' in filename.lower():
            tasks.append({'text_col': 'text', 'lang': 'thai'})
        
        # IndoLeX
        elif 'indolex' in filename.lower():
            if 'word' in [c.lower() for c in df_sample.columns]:
                tasks.append({'text_col': 'word', 'lang': 'indonesian'})
                
    elif ext == '.tsv':
        try:
            df_sample = pd.read_csv(filepath, sep='\t', nrows=10, encoding='utf-8', on_bad_lines='skip')
        except:
            return tasks
            
        if 'Language' in df_sample.columns and 'Text' in df_sample.columns:
            # We process this specially later, or we can just say 'lang_col': 'Language'
            tasks.append({'text_col': 'Text', 'lang_col': 'Language'})
            
    return tasks

def process_file(filepath, filename, ext):
    metadata = infer_metadata(filepath, filename)
    if not metadata:
        return [{
            'file_path': filepath, 'file_name': filename, 'format': ext,
            'text_column': 'UNKNOWN', 'candidate_language': 'UNKNOWN', 'language_code': '',
            'total_rows': 0, 'valid_rows': 0, 'median_text_length': 0,
            'status': 'Perlu Manual', 'reason': 'Tidak dapat mengenali kolom teks/bahasa',
            'len_under_10': 0, 'len_under_20': 0, 'len_under_30': 0
        }]
    
    results = []
    for task in metadata:
        text_col = task['text_col']
        lang = task.get('lang', None)
        lang_col = task.get('lang_col', None)
        
        total_rows = 0
        valid_rows = 0
        lengths = []
        len_under_10, len_under_20, len_under_30 = 0, 0, 0
        
        # If it's a huge TSV with lang_col (like old-newspaper)
        if lang_col:
            lang_stats = defaultdict(lambda: {'total': 0, 'valid': 0, 'lengths': [], 'u10': 0, 'u20': 0, 'u30': 0})
            
            # Streaming read
            try:
                chunk_iter = pd.read_csv(filepath, sep='\t' if ext=='.tsv' else ',', chunksize=100000, 
                                         usecols=[lang_col, text_col], encoding='utf-8', on_bad_lines='skip')
                for chunk in chunk_iter:
                    chunk = chunk.dropna(subset=[lang_col, text_col])
                    # Filter only target languages if we want to save memory, but we want a full audit.
                    # We will only track target languages to avoid blowing up memory with 100 languages.
                    for l, group in chunk.groupby(lang_col):
                        l_clean = str(l).strip().lower()
                        if l_clean not in TARGET_LANGS and l_clean != 'indonesian': # just catching targets
                            pass
                        
                        if l_clean in TARGET_LANGS:
                            texts = group[text_col].apply(clean_text)
                            texts = texts[texts != ""]
                            
                            lang_stats[l_clean]['total'] += len(group)
                            lang_stats[l_clean]['valid'] += len(texts)
                            
                            # calculate lengths
                            lens = texts.str.len()
                            # avoid saving all lengths to memory if too big
                            if len(lang_stats[l_clean]['lengths']) < 100000:
                                lang_stats[l_clean]['lengths'].extend(lens.tolist())
                                
                            lang_stats[l_clean]['u10'] += (lens < 10).sum()
                            lang_stats[l_clean]['u20'] += (lens < 20).sum()
                            lang_stats[l_clean]['u30'] += (lens < 30).sum()
                            
            except Exception as e:
                print(f"Error processing {filename}: {e}")
                
            for l_clean, stats in lang_stats.items():
                med_len = np.median(stats['lengths']) if stats['lengths'] else 0
                results.append({
                    'file_path': filepath, 'file_name': filename, 'format': ext,
                    'text_column': text_col, 'candidate_language': l_clean, 'language_code': '',
                    'total_rows': stats['total'], 'valid_rows': stats['valid'], 'median_text_length': med_len,
                    'status': 'Layak' if stats['valid'] >= 1000 else 'Data belum cukup',
                    'reason': 'Ditemukan via kolom bahasa',
                    'len_under_10': stats['u10'], 'len_under_20': stats['u20'], 'len_under_30': stats['u30']
                })
        else:
            # Standard single/bilingual file
            try:
                df = pd.read_csv(filepath, usecols=[text_col], encoding='utf-8', on_bad_lines='skip')
            except:
                try:
                    df = pd.read_csv(filepath, usecols=[text_col], encoding='latin1', on_bad_lines='skip')
                except:
                    continue
            
            total_rows = len(df)
            texts = df[text_col].apply(clean_text)
            texts = texts[texts != ""]
            valid_rows = len(texts)
            lens = texts.str.len()
            med_len = np.median(lens) if not lens.empty else 0
            u10 = (lens < 10).sum()
            u20 = (lens < 20).sum()
            u30 = (lens < 30).sum()
            
            results.append({
                'file_path': filepath, 'file_name': filename, 'format': ext,
                'text_column': text_col, 'candidate_language': lang, 'language_code': '',
                'total_rows': total_rows, 'valid_rows': valid_rows, 'median_text_length': med_len,
                'status': 'Layak' if valid_rows >= 1000 else 'Data belum cukup',
                'reason': 'Bilingual / Known File',
                'len_under_10': u10, 'len_under_20': u20, 'len_under_30': u30
            })
            
    return results


def main():
    dataset_dir = "datasets"
    all_files = glob.glob(f"{dataset_dir}/**/*", recursive=True)
    registry = []
    
    for filepath in all_files:
        if os.path.isfile(filepath):
            filename = os.path.basename(filepath)
            ext = os.path.splitext(filename)[1].lower()
            if ext in ['.csv', '.tsv', '.txt', '.json', '.jsonl']:
                print(f"Auditing {filename}...")
                file_results = process_file(filepath, filename, ext)
                registry.extend(file_results)

    df_registry = pd.DataFrame(registry)
    
    # Save registry
    output_cols = ['file_path', 'file_name', 'format', 'text_column', 'candidate_language', 'language_code', 
                   'total_rows', 'valid_rows', 'median_text_length', 'status', 'reason']
    
    # ensure cols exist
    for col in output_cols:
        if col not in df_registry.columns:
            df_registry[col] = ""
            
    df_registry[output_cols].to_csv('dataset_registry.csv', index=False)
    
    print("\n=== SUMMARY ===")
    
    # Aggregate by language
    lang_agg = df_registry[df_registry['candidate_language'] != 'UNKNOWN'].groupby('candidate_language').agg(
        valid_rows=('valid_rows', 'sum'),
        total_rows=('total_rows', 'sum')
    )
    
    layak = []
    belum_cukup = []
    
    for lang, row in lang_agg.iterrows():
        if row['valid_rows'] >= 1000:
            layak.append(lang)
        else:
            belum_cukup.append(lang)
            
    belum_ada = [l for l in TARGET_LANGS if l not in lang_agg.index]
    unknown_files = df_registry[df_registry['candidate_language'] == 'UNKNOWN']['file_name'].tolist()
    
    print("\n[BAHASA LAYAK]")
    for l in layak: print(f"- {l} ({lang_agg.loc[l, 'valid_rows']} valid)")
    
    print("\n[BAHASA DATA BELUM CUKUP]")
    for l in belum_cukup: print(f"- {l} ({lang_agg.loc[l, 'valid_rows']} valid)")
    
    print("\n[BAHASA BELUM ADA DATASET]")
    for l in belum_ada: print(f"- {l}")
    
    print("\n[FILE BAHASA TIDAK DIKETAHUI]")
    for f in unknown_files: print(f"- {f}")
    
    print("\n[STATISTIK TEKS TERLALU PENDEK (SECARA KESELURUHAN)]")
    if 'len_under_10' in df_registry.columns:
        print(f"Teks < 10 karakter: {df_registry['len_under_10'].sum()}")
        print(f"Teks < 20 karakter: {df_registry['len_under_20'].sum()}")
        print(f"Teks < 30 karakter: {df_registry['len_under_30'].sum()}")
    
if __name__ == '__main__':
    main()
