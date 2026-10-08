# Deteksi Bahasa Asia (Asian Language Detection)

Proyek ini bertujuan untuk membangun model Machine Learning ringan (berbasis LinearSVC dengan *Character N-grams TF-IDF*) yang mampu mendeteksi bahasa-bahasa di wilayah Asia dari masukan teks. Model ini dirancang agar efisien untuk di-*deploy* ke aplikasi *mobile* Android di masa depan.

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
2. Buat environment baru menggunakan Python 3.12+.
3. Instal library yang dibutuhkan:
```bash
pip install -r requirements.txt
```

Untuk menjalankan test dan alat pengembangan:
```bash
pip install -r requirements-dev.txt
pytest
```

## Menjalankan API
Atur token admin melalui environment, lalu jalankan API dari root repository:
```powershell
$env:ADMIN_API_TOKEN = "ganti-dengan-token-acak-yang-panjang"
uvicorn api.main:app --host 0.0.0.0 --port 8000
```

Endpoint prediksi tetap dapat digunakan tanpa token. Token hanya diperlukan
untuk endpoint administratif `/feedback/stats`. Lokasi database dapat diubah
dengan environment variable `FEEDBACK_DB_PATH`.

## Dataset
Dataset gabungan (*Verified Dataset*) berisi 35.000 teks (masing-masing 5.000 kalimat per bahasa), tetapi file lengkapnya tidak disertakan di repository:
> Dataset terverifikasi belum dipublikasikan di repository ini. Atur sumber
> dataset internal Anda sebagai `data/verified_dataset.csv` atau gunakan path
> lain melalui argumen `--data-path`/`--base-data`.

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

## Menjalankan Aplikasi Android
Build debug memakai `http://127.0.0.1:8000/` melalui ADB reverse. Setelah
emulator aktif, jalankan `adb reverse tcp:8000 tcp:8000`. Untuk perangkat
fisik, atur alamat IP komputer pada Gradle properties lokal:
```properties
DEBUG_API_BASE_URL=http://192.168.1.10:8000/
```

Untuk build release, wajib siapkan URL HTTPS yang sebenarnya:
```properties
RELEASE_API_BASE_URL=https://api.example.com/
```

Riwayat deteksi disimpan lokal dan Android cloud backup dinonaktifkan untuk
menghindari pencadangan teks pengguna. Implementasi on-device dengan TFLite
masih dapat menjadi tahap lanjutan agar aplikasi tidak bergantung pada server.

## Fitur Active Learning (Pembelajaran Semi-Otomatis)
Aplikasi Android sekarang mendukung fitur pengumpulan koreksi (feedback) dari pengguna ketika prediksi dirasa kurang akurat.

### Alur Koreksi Pengguna
1. Ketika pengguna mendeteksi bahasa, Android menampilkan tombol "Koreksi hasil".
2. Pengguna dapat memilih bahasa yang benar dari dropdown (diperoleh otomatis dari backend `/languages`) atau mengetik nama bahasa baru.
3. Feedback dikirimkan ke `/feedback` dan disimpan di database lokal SQLite (`data/feedback.db`) dengan status `PENDING`.
4. *Penting: Teks yang dikoreksi disimpan secara persisten di server untuk keperluan evaluasi dan retraining. Data ini dapat bersifat privasi tergantung pada teks yang dimasukkan pengguna.*

### Cara Menyetujui/Menolak Feedback
Administrator atau kurator data dapat mengupdate status feedback pada database `feedback.db` secara langsung (mengubah `PENDING` menjadi `APPROVED` atau `REJECTED`).

### Cara Menjalankan Retraining
Model baru dapat dilatih ulang menggunakan gabungan dataset utama dan feedback berstatus `APPROVED` menggunakan perintah:
```bash
python -m src.retrain --base-data data/verified_dataset.csv --output-dir artifacts_candidate
```
*Catatan: Bahasa baru yang memiliki sampel kurang dari batas minimum (default 50) tidak akan diikutsertakan.*

### Evaluasi dan Promosi Model Kandidat
Skrip `src.retrain` secara otomatis melakukan perhitungan Macro F1 Score dan metrik evaluasi lainnya, kemudian menyimpannya ke `artifacts_candidate/config.json`.
Untuk mempromosikan model kandidat menjadi model aktif, jalankan:
```bash
python -m src.model_manager
```
Skrip ini memvalidasi kelengkapan artifact, memastikan tidak ada label aktif
yang hilang, dan membandingkan model lama serta kandidat pada test set yang
sama. Promosi dilakukan dengan pergantian direktori atomik dan model lama
disimpan di `artifacts_backup/`. Restart proses API setelah promosi agar artifact
baru dimuat.

### Mekanisme Rollback
Jika model yang baru dipromosikan mengalami kegagalan saat digunakan, jalankan perintah berikut untuk mengembalikannya ke versi backup:
```bash
python -m src.model_manager rollback
```
