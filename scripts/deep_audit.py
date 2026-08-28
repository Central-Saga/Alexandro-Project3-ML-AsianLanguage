import os
import glob
import pandas as pd
import json
import chardet

dataset_dir = "datasets"
extensions = ["*.csv", "*.tsv", "*.txt", "*.json", "*.jsonl"]
files = []
for ext in extensions:
    files.extend(glob.glob(os.path.join(dataset_dir, ext)))

print("=== DEEP AUDIT DATASETS ===")

results = []

for file_path in files:
    filename = os.path.basename(file_path)
    file_size = os.path.getsize(file_path)
    if file_size > 50 * 1024 * 1024:  # Skip files > 50MB for quick audit
        results.append({
            "filename": filename,
            "status": "Tidak Layak / Perlu Manual (Terlalu Besar)",
            "reason": f"Ukuran {file_size/1024/1024:.2f} MB",
            "format": filename.split('.')[-1],
            "cols": [],
            "sample": ""
        })
        continue
    
    try:
        with open(file_path, 'rb') as f:
            raw = f.read(10000)
        enc = chardet.detect(raw)['encoding'] or 'utf-8'
        
        if filename.endswith('.csv'):
            df = pd.read_csv(file_path, encoding=enc, on_bad_lines='skip', nrows=100)
        elif filename.endswith('.tsv'):
            df = pd.read_csv(file_path, sep='\t', encoding=enc, on_bad_lines='skip', nrows=100)
        elif filename.endswith('.jsonl'):
            df = pd.read_json(file_path, lines=True, encoding=enc, nrows=100)
        elif filename.endswith('.json'):
            df = pd.read_json(file_path, encoding=enc)
        else: # txt
            with open(file_path, 'r', encoding=enc, errors='ignore') as f:
                lines = f.readlines()[:100]
            df = pd.DataFrame({"text": lines})
        
        cols = list(df.columns)
        if len(df) == 0:
             results.append({
                "filename": filename,
                "status": "Tidak Layak",
                "reason": "File Kosong atau gagal di-parse",
                "format": filename.split('.')[-1],
                "cols": cols,
                "sample": ""
             })
             continue
             
        sample = str(df.iloc[0].to_dict())[:200]
        
        # Simple heuristics for classification
        status = "Perlu Pemeriksaan Manual"
        reason = "Kolom teks/bahasa belum teridentifikasi otomatis."
        
        if any(c in filename.lower() for c in ['acehnese', 'balinese', 'banjarese', 'buginese', 'madurese', 'minangkabau', 'ngaju', 'sundanese', 'thai', 'toba_batak']):
             status = "Layak"
             reason = f"Nama file mengindikasikan bahasa target."
        elif len(cols) == 1 and 'text' in cols[0].lower():
             status = "Layak"
             reason = "Satu kolom berisi teks."
        elif 'text' in [str(c).lower() for c in cols]:
             status = "Perlu Pemeriksaan Manual"
             reason = "Ada kolom teks, tapi bahasanya tidak diketahui dari nama file."
             
        results.append({
            "filename": filename,
            "status": status,
            "reason": reason,
            "format": filename.split('.')[-1],
            "cols": cols,
            "sample": sample
        })
        
    except Exception as e:
        results.append({
            "filename": filename,
            "status": "Error",
            "reason": str(e),
            "format": filename.split('.')[-1],
            "cols": [],
            "sample": ""
        })

markdown_output = "=== DEEP AUDIT DATASETS ===\\n\\n"
for r in results:
    markdown_output += f"### File: {r['filename']}\\n"
    markdown_output += f"- **Format:** {r['format']}\\n"
    markdown_output += f"- **Kolom:** {r['cols']}\\n"
    markdown_output += f"- **Status:** {r['status']}\\n"
    markdown_output += f"- **Alasan:** {r['reason']}\\n"
    markdown_output += f"- **Contoh (truncated):** `{r['sample'].encode('unicode_escape').decode('utf-8')}`\\n\\n"

with open("audit_report_temp.md", "w", encoding="utf-8") as f:
    f.write(markdown_output)


