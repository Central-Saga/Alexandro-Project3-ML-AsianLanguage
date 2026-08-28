import os
import pandas as pd
import numpy as np
import unicodedata
import re
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)

def clean_text(text):
    if not isinstance(text, str): return ""
    text = unicodedata.normalize('NFKC', text).strip()
    if re.match(r'^[\W\d_]+$', text): return ""
    return text

def process_v2_dataset():
    out_dir = "processed_v2"
    os.makedirs(out_dir, exist_ok=True)
    
    registry = pd.read_csv('dataset_registry.csv')
    
    # Exclude unwanted files
    banned_files = ['words freq sorted.csv', 'IndoLeX_Database.csv']
    registry = registry[~registry['file_name'].isin(banned_files)]
    registry = registry[registry['candidate_language'] != 'UNKNOWN']
    
    all_raw_data = []
    
    # Target large languages and local
    # filipino and tagalog merged to fil_tl
    
    # Read files
    for _, row in registry.iterrows():
        filepath = row['file_path']
        filename = row['file_name']
        ext = row['format']
        text_col = row['text_column']
        lang = row['candidate_language']
        
        if lang in ['burmese', 'tetum']:
            continue
            
        print(f"Reading {filename} - {lang}...")
        
        if filename.lower() == 'old-newspaper.tsv':
            # read via chunks to avoid MemoryError
            chunk_iter = pd.read_csv(filepath, sep='\t', chunksize=50000, usecols=['Language', 'Text'], on_bad_lines='skip', encoding='utf-8')
            collected = 0
            for chunk in chunk_iter:
                chunk = chunk.dropna(subset=['Language', 'Text'])
                chunk_lang = chunk[chunk['Language'].str.lower() == lang]
                if not chunk_lang.empty:
                    texts = chunk_lang['Text'].apply(clean_text)
                    texts = texts[texts != ""]
                    for t in texts:
                        all_raw_data.append({'source_file': filename, 'text': t, 'language': lang})
                        collected += 1
                if collected > 10000: # limit per chunk to avoid taking too much, we only need 5000 at the end
                    break
        else:
            try:
                df = pd.read_csv(filepath, usecols=[text_col], encoding='utf-8', on_bad_lines='skip')
            except:
                try:
                    df = pd.read_csv(filepath, usecols=[text_col], encoding='latin1', on_bad_lines='skip')
                except:
                    continue
            
            texts = df[text_col].apply(clean_text)
            texts = texts[texts != ""]
            
            # Subsample if too large to avoid memory issues later, but keep a bit extra for cleaning
            if len(texts) > 10000:
                texts = texts.sample(10000, random_state=RANDOM_SEED)
                
            for t in texts:
                all_raw_data.append({'source_file': filename, 'text': t, 'language': lang})
                
    df_raw = pd.DataFrame(all_raw_data)
    
    # Rule 7: Merge filipino and tagalog
    df_raw.loc[df_raw['language'].isin(['tagalog', 'filipino']), 'language'] = 'fil_tl'
    
    # Drop empty again just in case
    df_raw = df_raw[df_raw['text'] != ""]
    
    df_raw['char_len'] = df_raw['text'].str.len()
    
    # Rule 11: Length < 20 -> excluded_short_text.csv
    short_texts = df_raw[df_raw['char_len'] < 20]
    short_texts.to_csv(os.path.join(out_dir, 'excluded_short_text.csv'), index=False)
    
    # Valid texts >= 20
    df_valid = df_raw[df_raw['char_len'] >= 20].copy()
    
    # Rule 10: normalized duplicate globally and conflicts
    df_valid['norm_text'] = df_valid['text'].str.lower()
    
    # Find conflicts (same text, different label)
    label_counts = df_valid.groupby('norm_text')['language'].nunique()
    conflicts = label_counts[label_counts > 1].index
    
    rejected_conflicts = df_valid[df_valid['norm_text'].isin(conflicts)]
    
    df_clean = df_valid[~df_valid['norm_text'].isin(conflicts)]
    
    # Duplicate globally
    dupes = df_clean.duplicated(subset=['norm_text'], keep='first')
    rejected_dupes = df_clean[dupes]
    df_clean = df_clean[~dupes]
    
    rejected_all = pd.concat([rejected_conflicts, rejected_dupes])
    rejected_all.to_csv(os.path.join(out_dir, 'rejected_rows.csv'), index=False)
    
    # Drop norm_text
    df_clean = df_clean.drop(columns=['norm_text'])
    
    # Source distribution tracking
    source_dist = df_clean.groupby(['language', 'source_file']).size().reset_index(name='count')
    source_dist.to_csv(os.path.join(out_dir, 'source_distribution.csv'), index=False)
    
    # Filtering languages based on counts
    final_data = []
    
    lang_counts = df_clean['language'].value_counts()
    
    for lang, count in lang_counts.items():
        lang_df = df_clean[df_clean['language'] == lang]
        if count >= 500:
            if count > 5000:
                lang_df = lang_df.sample(5000, random_state=RANDOM_SEED)
            final_data.append(lang_df)
        else:
            print(f"Dropping {lang} due to insufficient valid data (< 500): {count}")
            
    df_final = pd.concat(final_data, ignore_index=True)
    
    # Save final dataset
    df_final.to_csv(os.path.join(out_dir, 'verified_dataset.csv'), index=False)
    
    print("\n--- DISTRIBUSI AKHIR PER BAHASA ---")
    final_counts = df_final['language'].value_counts()
    print(final_counts)
    
    print("\n--- DISTRIBUSI THAI BERDASARKAN SOURCE FILE ---")
    thai_dist = df_final[df_final['language'] == 'thai']['source_file'].value_counts()
    print(thai_dist)
    
    # Print 30 random examples per lang-source combo
    print("\n--- 30 CONTOH ACAK PER KOMBINASI BAHASA & SUMBER (Maksimal 5 ditampilkan) ---")
    print("\n--- 30 CONTOH ACAK PER KOMBINASI BAHASA & SUMBER (Maksimal 5 ditampilkan) ---")
    
    samples_list = []
    for combo, group in df_final.groupby(['language', 'source_file']):
        sampled = group.sample(min(len(group), 30), random_state=RANDOM_SEED)
        samples_list.append(sampled)
        print(f"\n[{combo[0]} - {combo[1]}]")
        for i, row in sampled.head(5).iterrows():
            print(f"  - ({row['char_len']} chars): {row['text'][:80]}...")
            
    combo_samples = pd.concat(samples_list, ignore_index=True) if samples_list else pd.DataFrame()
    if not combo_samples.empty:
        combo_samples.to_csv(os.path.join(out_dir, 'combo_30_samples.csv'), index=False)
            
    # Print 10 random examples per language
    print("\n--- 10 CONTOH ACAK PER BAHASA ---")
    lang_samples_list = []
    for lang, group in df_final.groupby('language'):
        sampled = group.sample(min(len(group), 10), random_state=RANDOM_SEED)
        print(f"\n[{lang}]")
        for i, row in sampled.iterrows():
            print(f"  - {row['text'][:80]}...")

if __name__ == '__main__':
    process_v2_dataset()
