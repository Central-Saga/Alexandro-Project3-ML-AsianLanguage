import json

notebook = {
 "cells": [
  {
   "cell_type": "markdown",
   "metadata": {},
   "source": [
    "# Validasi Akhir Dataset: Multiclass Language Identification\n",
    "Notebook ini memuat tahapan pemuatan data, pembersihan, dan validasi 10 poin sesuai instruksi."
   ]
  },
  {
   "cell_type": "code",
   "execution_count": None,
   "metadata": {},
   "outputs": [],
   "source": [
    "import pandas as pd\n",
    "import numpy as np\n",
    "import os\n",
    "import unicodedata\n",
    "import random\n",
    "from sklearn.model_selection import train_test_split\n",
    "\n",
    "# Poin 10: Random seed harus ditetapkan\n",
    "RANDOM_SEED = 42\n",
    "np.random.seed(RANDOM_SEED)\n",
    "random.seed(RANDOM_SEED)\n",
    "print(f\"Random seed di-set ke: {RANDOM_SEED}\")\n"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": None,
   "metadata": {},
   "outputs": [],
   "source": [
    "# Definisi Dataset Rekomendasi\n",
    "datasets_info = [\n",
    "    {\"file\": \"acehnese.csv\", \"text_col\": \"acehnese\", \"label\": \"acehnese\", \"encoding\": \"ascii\"},\n",
    "    {\"file\": \"balinese.csv\", \"text_col\": \"balinese\", \"label\": \"balinese\", \"encoding\": \"ascii\"},\n",
    "    {\"file\": \"banjarese.csv\", \"text_col\": \"banjarese\", \"label\": \"banjarese\", \"encoding\": \"ascii\"},\n",
    "    {\"file\": \"buginese.csv\", \"text_col\": \"buginese\", \"label\": \"buginese\", \"encoding\": \"ascii\"},\n",
    "    {\"file\": \"madurese.csv\", \"text_col\": \"madurese\", \"label\": \"madurese\", \"encoding\": \"utf-8\"},\n",
    "    {\"file\": \"minangkabau.csv\", \"text_col\": \"minangkabau\", \"label\": \"minangkabau\", \"encoding\": \"ascii\"},\n",
    "    {\"file\": \"ngaju.csv\", \"text_col\": \"ngaju\", \"label\": \"ngaju\", \"encoding\": \"ascii\"},\n",
    "    {\"file\": \"sundanese.csv\", \"text_col\": \"sundanese\", \"label\": \"sundanese\", \"encoding\": \"ascii\"},\n",
    "    {\"file\": \"thai-government-corpus.csv\", \"text_col\": \"text\", \"label\": \"thai\", \"encoding\": \"utf-8\"},\n",
    "    {\"file\": \"toba_batak.csv\", \"text_col\": \"toba_batak\", \"label\": \"toba_batak\", \"encoding\": \"ascii\"}\n",
    "]\n",
    "\n",
    "dataset_dir = \"datasets\"\n",
    "dataframes = []\n",
    "berhasil = []\n",
    "gagal = []\n",
    "\n",
    "# Poin 1, 2 & 9: Pemuatan Data dan Kolom\n",
    "print(\"--- STATUS PEMUATAN DATA ---\")\n",
    "for info in datasets_info:\n",
    "    file_path = os.path.join(dataset_dir, info[\"file\"])\n",
    "    try:\n",
    "        df_temp = pd.read_csv(file_path, encoding=info[\"encoding\"])\n",
    "        # Simpan kolom teks asli untuk log\n",
    "        original_cols = list(df_temp.columns)\n",
    "        \n",
    "        # Standardize\n",
    "        df_temp = df_temp[[info[\"text_col\"]]].copy()\n",
    "        df_temp.columns = [\"text\"]\n",
    "        df_temp[\"label\"] = info[\"label\"]\n",
    "        \n",
    "        dataframes.append(df_temp)\n",
    "        berhasil.append(info['file'])\n",
    "        print(f\"[SUKSES] File: {info['file']} | Kolom asli: {original_cols} -> Diekstrak: '{info['text_col']}' (Target label: {info['label']})\")\n",
    "    except Exception as e:\n",
    "        gagal.append((info['file'], str(e)))\n",
    "        print(f\"[GAGAL]  File: {info['file']} | Error: {e}\")\n",
    "\n",
    "print(\"\\n--- POIN 1: DAFTAR FILE BERHASIL ---\")\n",
    "for f in berhasil: print(f\"- {f}\")\n",
    "\n",
    "print(\"\\n--- POIN 9: DAFTAR FILE GAGAL ---\")\n",
    "if not gagal: print(\"Tidak ada file yang gagal dibaca.\")\n",
    "for f, err in gagal: print(f\"- {f}: {err}\")\n"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": None,
   "metadata": {},
   "outputs": [],
   "source": [
    "# Penggabungan\n",
    "df_all = pd.concat(dataframes, ignore_index=True)\n",
    "\n",
    "dist_awal = df_all['label'].value_counts().to_dict()\n",
    "baris_awal = len(df_all)\n",
    "\n",
    "# Pembersihan Null & Duplikat\n",
    "df_all = df_all.dropna(subset=['text'])\n",
    "baris_setelah_null = len(df_all)\n",
    "\n",
    "df_all = df_all.drop_duplicates(subset=['text'])\n",
    "baris_setelah_duplikat = len(df_all)\n",
    "\n",
    "print(\"--- POIN 3: STATISTIK BARIS ---\")\n",
    "print(f\"Total baris awal                : {baris_awal}\")\n",
    "print(f\"Total baris (setelah drop null) : {baris_setelah_null} (Dihapus: {baris_awal - baris_setelah_null})\")\n",
    "print(f\"Total baris (setelah drop dup)  : {baris_setelah_duplikat} (Dihapus: {baris_setelah_null - baris_setelah_duplikat})\")\n"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": None,
   "metadata": {},
   "outputs": [],
   "source": [
    "def clean_text(t):\n",
    "    if not isinstance(t, str):\n",
    "        return \"\"\n",
    "    t = unicodedata.normalize('NFKC', t)\n",
    "    t = t.strip()\n",
    "    return t\n",
    "\n",
    "df_all['text'] = df_all['text'].apply(clean_text)\n",
    "\n",
    "# Poin 8: Pemeriksaan teks kosong atau terlalu pendek (< 3 char)\n",
    "terlalu_pendek = df_all[df_all['text'].str.len() < 3]\n",
    "print(\"--- POIN 8: TEKS TERLALU PENDEK (< 3 KARAKTER) ---\")\n",
    "print(f\"Ditemukan {len(terlalu_pendek)} baris teks yang terlalu pendek.\")\n",
    "if len(terlalu_pendek) > 0:\n",
    "    print(\"Contoh data yang dihapus:\")\n",
    "    print(terlalu_pendek.head())\n",
    "\n",
    "# Buang teks terlalu pendek\n",
    "df_all = df_all[df_all['text'].str.len() >= 3]\n",
    "print(f\"\\nBaris setelah pembersihan akhir: {len(df_all)}\")\n"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": None,
   "metadata": {},
   "outputs": [],
   "source": [
    "print(\"--- POIN 4: DISTRIBUSI JUMLAH TEKS SEBELUM & SESUDAH ---\")\n",
    "dist_akhir = df_all['label'].value_counts().to_dict()\n",
    "\n",
    "print(f\"{'Bahasa':<15} | {'Sebelum':<10} | {'Sesudah':<10} | {'Selisih (Dibuang)'}\")\n",
    "print(\"-\"*60)\n",
    "for lang in dist_awal.keys():\n",
    "    awal = dist_awal.get(lang, 0)\n",
    "    akhir = dist_akhir.get(lang, 0)\n",
    "    print(f\"{lang:<15} | {awal:<10} | {akhir:<10} | {awal - akhir}\")\n"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": None,
   "metadata": {},
   "outputs": [],
   "source": [
    "print(\"--- POIN 5: LIMA CONTOH TEKS DARI SETIAP BAHASA ---\")\n",
    "for lang in df_all['label'].unique():\n",
    "    print(f\"\\n>>> Bahasa: {lang}\")\n",
    "    sampel = df_all[df_all['label'] == lang]['text'].sample(n=min(5, dist_akhir[lang]), random_state=RANDOM_SEED)\n",
    "    for i, teks in enumerate(sampel):\n",
    "        print(f\"  {i+1}. {teks[:100]}{'...' if len(teks)>100 else ''}\")\n"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": None,
   "metadata": {},
   "outputs": [],
   "source": [
    "# Pembagian Data (70/15/15)\n",
    "df_temp, df_test = train_test_split(\n",
    "    df_all, test_size=0.15, random_state=RANDOM_SEED, stratify=df_all['label']\n",
    ")\n",
    "df_train, df_val = train_test_split(\n",
    "    df_temp, test_size=(0.15/0.85), random_state=RANDOM_SEED, stratify=df_temp['label']\n",
    ")\n",
    "\n",
    "print(\"--- POIN 6: JUMLAH DATA TRAIN, VAL, TEST ---\")\n",
    "train_dist = df_train['label'].value_counts()\n",
    "val_dist = df_val['label'].value_counts()\n",
    "test_dist = df_test['label'].value_counts()\n",
    "\n",
    "print(f\"{'Bahasa':<15} | {'Train':<8} | {'Val':<8} | {'Test':<8}\")\n",
    "print(\"-\"*50)\n",
    "for lang in dist_akhir.keys():\n",
    "    print(f\"{lang:<15} | {train_dist.get(lang, 0):<8} | {val_dist.get(lang, 0):<8} | {test_dist.get(lang, 0):<8}\")\n"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": None,
   "metadata": {},
   "outputs": [],
   "source": [
    "print(\"--- POIN 7: PEMERIKSAAN KEBOCORAN TEKS (DATA LEAKAGE) ---\")\n",
    "set_train = set(df_train['text'])\n",
    "set_val = set(df_val['text'])\n",
    "set_test = set(df_test['text'])\n",
    "\n",
    "bocor_train_val = set_train.intersection(set_val)\n",
    "bocor_train_test = set_train.intersection(set_test)\n",
    "bocor_val_test = set_val.intersection(set_test)\n",
    "\n",
    "if bocor_train_val or bocor_train_test or bocor_val_test:\n",
    "    print(\"[PERINGATAN] Ada kebocoran data antar partisi!\")\n",
    "    print(f\"Train & Val overlap: {len(bocor_train_val)} baris\")\n",
    "    print(f\"Train & Test overlap: {len(bocor_train_test)} baris\")\n",
    "    print(f\"Val & Test overlap: {len(bocor_val_test)} baris\")\n",
    "else:\n",
    "    print(\"[AMAN] Tidak ada teks yang tumpang tindih antara Train, Validation, dan Test.\")\n"
   ]
  },
  {
   "cell_type": "markdown",
   "metadata": {},
   "source": [
    "## 6. Model Baseline (Logistic Regression / Linear SVM)\n",
    "> *Catatan: Placeholder. Algoritma pelatihan model tidak ditambahkan terlebih dahulu.*"
   ]
  }
 ],
 "metadata": {
  "kernelspec": {
   "display_name": "Python 3",
   "language": "python",
   "name": "python3"
  }
 },
 "nbformat": 4,
 "nbformat_minor": 4
}

with open('language_detection_pipeline.ipynb', 'w', encoding='utf-8') as f:
    json.dump(notebook, f, indent=1, ensure_ascii=False)
