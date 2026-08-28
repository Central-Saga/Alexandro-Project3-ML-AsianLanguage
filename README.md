# Deteksi Bahasa Asia Tenggara (Southeast Asian Language Detection)

Proyek ini bertujuan untuk membangun model Machine Learning ringan (berbasis LinearSVC dengan *Character N-grams TF-IDF*) yang mampu mendeteksi bahasa-bahasa di wilayah Asia Tenggara dari masukan teks. Model ini dirancang agar efisien untuk di-*deploy* ke aplikasi *mobile* Android di masa depan.

## Bahasa yang Didukung
Model v2 saat ini mendukung 7 bahasa:
- Indonesian (`indonesian`)
- Malay (`malay`)
- Thai (`thai`)
- Vietnamese (`vietnamese`)
- Tagalog / Filipino (`fil_tl`)
- Khmer (`khmer`)
- Lao (`lao`)

> **Catatan**: Bahasa-bahasa lain seperti Burmese, Tetum, dan bahasa daerah Indonesia belum didukung pada versi ini karena belum masuk dalam tahap pelatihan (data tidak mencukupi untuk kriteria *sentence-level*).

## Struktur Repository
```
├── artifacts/              # Berisi model.pkl, preprocessor.pkl, config.json, labels.json
├── data/                   # Data sampel (contoh dataset)
├── notebooks/              # Jupyter Notebook berisi proses eksperimen dan EDA
├── src/                    # Source code utama (modul Python)
│   ├── __init__.py
│   ├── predict.py          # Script untuk memprediksi teks baru
│   ├── preprocessing.py    # Modul pembersihan teks
│   └── train.py            # Script pelatihan ulang model
├── tests/                  # Unit test
├── .gitignore
├── requirements.txt
└── README.md
```

## Performa Model
Berdasarkan evaluasi akhir pada *Test Set* yang sepenuhnya dipisahkan menggunakan *group-aware split* (70% Train, 15% Validation, 15% Test):
- **Test Macro F1 Score**: 0.9830
- **Test Accuracy**: 98.27%
- **Inference Time**: ~0.40 ms / teks

## Instalasi
1. Clone repository ini.
2. Buat environment baru (disarankan menggunakan Python 3.10+).
3. Instal library yang dibutuhkan:
```bash
pip install -r requirements.txt
```

## Dataset
Dataset gabungan (*Verified Dataset*) berisi 35.000 teks (masing-masing 5.000 kalimat per bahasa) dapat diunduh melalui link berikut:
> **Download Dataset**: [URL_GOOGLE_DRIVE_DATASET_PLACEHOLDER]

Setelah diunduh, simpan dataset tersebut di direktori lokal Anda (misalnya `data/verified_dataset.csv`). Sumber dataset mentah berasal dari publik domain dan kompilasi sumber *open-source* yang dikurasi.

## Cara Pelatihan (Training)
Untuk melatih ulang model dari awal, jalankan perintah berikut dari direktori root:
```bash
python src/train.py --data-path data/verified_dataset.csv --output-dir artifacts
```
Ini akan otomatis melatih *LinearSVC* dan menyimpan *TF-IDF Vectorizer* ke folder `artifacts/`.

## Cara Penggunaan (Inference)
Gunakan modul `predict.py` untuk mendeteksi bahasa dari *DataFrame*:
```python
import pandas as pd
from src.predict import predict, predict_with_details

# Siapkan input DataFrame (harus memiliki kolom 'text')
df = pd.DataFrame({
    "text": [
        "Cuaca hari ini sangat cerah.",
        "Tôi đang học tiếng Việt."
    ]
})

# Prediksi label saja
predictions = predict(df)
print(predictions) 
# Output: ['indonesian', 'vietnamese']

# Prediksi dengan detail (menampilkan margin_score dari LinearSVC)
detailed = predict_with_details(df)
print(detailed)
# Output: [{'label': 'indonesian', 'margin_score': 1.8}, ...]
```

## Keterbatasan Model
- **Kata Serapan & Kalimat Sangat Pendek**: Akurasi dapat menurun drastis pada teks berukuran di bawah 50 karakter (khususnya antara *Indonesian* dan *Malay*) akibat kesamaan leksikal.
- **Teks Out-of-Distribution**: Model belum dikonfigurasi sepenuhnya untuk memblokir karakter khusus secara eksplisit di level prapemrosesan (misal: *Korean Hangul*). Sangat direkomendasikan menambahkan blok deteksi Unicode sebelum melempar teks ke model TF-IDF ini.

## Rencana Implementasi Android
Fase selanjutnya dari proyek ini adalah melakukan konversi model Scikit-Learn (*LinearSVC* dan *TF-IDF*) menjadi format **TensorFlow Lite (TFLite)** atau porting komputasi bobot matriks liniernya secara langsung ke dalam Kotlin/Java agar dapat berjalan *on-device* dengan latensi rendah tanpa memerlukan API server.
