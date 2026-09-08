import sys
from pathlib import Path

# Add project root to path
BASE_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(BASE_DIR))

from src.database import save_feedback, update_feedback_status

indo_texts = [
    "Halo semuanya, hari ini saya sedang menguji kalimat bahasa Indonesia.",
    "halo dunia nama saya budi dan saya tinggal di jakarta",
    "Saya sedang mempelajari machine learning untuk deteksi bahasa.",
    "Coba tes bahasa indonesia yang ini apakah dikenali atau tidak.",
    "Budi pergi ke pasar membeli buku dan sayuran.",
    "Bapak presiden sedang memberikan pidato di istana negara.",
    "Kami sedang mengerjakan proyek kecerdasan buatan.",
    "kamu lagi ngapain sekarang? aku lagi makan nasi goreng.",
    "cuaca hari ini sangat panas, lebih baik minum es teh manis.",
    "saya suka membaca buku di perpustakaan nasional.",
    "kok bahasa indonesia tidak di kenali",
    "hari ini saya pergi ke kantor naik kereta api.",
    "jalan tol jakarta cikampek sedang macet total.",
    "selamat pagi semuanya, semoga hari ini menyenangkan.",
    "mari kita belajar bersama sama di kelas ini.",
    "saya lahir di kota bandung provinsi jawa barat.",
    "jangan lupa membawa payung karena nanti sore akan hujan.",
    "pesawat terbang dari bandara soekarno hatta menuju bali.",
    "ibu memasak rendang daging sapi yang sangat lezat.",
    "anak anak sedang bermain layang layang di lapangan merah."
]

malay_texts = [
    "Helo semua, hari ini saya sedang menguji ayat bahasa Melayu.",
    "helo dunia nama saya budi dan saya tinggal di kuala lumpur",
    "Saya sedang mempelajari pembelajaran mesin untuk pengesanan bahasa.",
    "Cuba uji bahasa melayu ini sama ada ia dikenali atau tidak.",
    "Budi pergi ke pasar membeli buku dan sayur-sayuran.",
    "Tuan perdana menteri sedang memberikan ucapan di putrajaya.",
    "Kami sedang mengusahakan projek kecerdasan buatan.",
    "awak tengah buat apa sekarang? saya tengah makan nasi goreng.",
    "cuaca hari ini sangat panas, lebih baik minum teh o ais.",
    "saya suka membaca buku di perpustakaan negara.",
    "kenapa bahasa melayu tidak dikenali",
    "hari ini saya pergi ke pejabat naik kereta api.",
    "lebuhraya utara selatan sedang sesak teruk.",
    "selamat pagi semua, semoga hari ini menyeronokkan.",
    "mari kita belajar bersama-sama di kelas ini.",
    "saya lahir di bandar kuala lumpur.",
    "jangan lupa bawa payung kerana nanti petang akan hujan.",
    "kapal terbang berlepas dari lapangan terbang antarabangsa kuala lumpur.",
    "ibu memasak rendang daging lembu yang sangat sedap.",
    "kanak-kanak sedang bermain layang-layang di padang."
]

for text in indo_texts:
    # 5 duplicates each to give it more weight
    for _ in range(5):
        fid = save_feedback(text, "malay", "indonesian", 0.9, False, "1.0")
        update_feedback_status(fid, "APPROVED")

for text in malay_texts:
    for _ in range(5):
        fid = save_feedback(text, "indonesian", "malay", 0.9, False, "1.0")
        update_feedback_status(fid, "APPROVED")

print("Berhasil memasukkan data feedback!")
