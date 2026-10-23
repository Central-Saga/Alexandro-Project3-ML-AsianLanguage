import pytest
from src.script_detection import is_unknown_script

def test_script_detection_unknown():
    # Cyrillic harus unknown
    assert is_unknown_script("Здравствуйте, сегодня я тестирую русские предложения.") == True

def test_script_detection_supported():
    assert is_unknown_script("안녕하세요, 저는 오늘 한국어 문장을 테스트하고 있습니다.") == False
    assert is_unknown_script("こんにちは、今日は日本語の文章をテストしています。") == False
    assert is_unknown_script("مرحباً، أنا أختبر الجمل العربية اليوم.") == False
    # Indonesia tidak boleh ditolak
    assert is_unknown_script("Halo, hari ini saya sedang menguji kalimat bahasa Indonesia.") == False
    # Vietnam tidak boleh ditolak
    assert is_unknown_script("Xin chào, hôm nay tôi đang thử nghiệm các câu tiếng Việt.") == False
    # Thai tidak boleh ditolak
    assert is_unknown_script("สวัสดี วันนี้ฉันกำลังทดสอบประโยคภาษาไทย") == False
    # Lao tidak boleh ditolak
    assert is_unknown_script("ສະບາຍດີ ມື້ນີ້ຂ້ອຍກຳລັງທົດສອບປະໂຫຍກພາສາລາວ") == False
    # Khmer tidak boleh ditolak
    assert is_unknown_script("សួស្តី ថ្ងៃនេះខ្ញុំកំពុងសាកល្បងប្រយោគភាសាខ្មែរ") == False
    
def test_script_detection_emoji():
    # kalimat Indonesia dengan emoji tidak boleh ditolak
    assert is_unknown_script("Halo semuanya! 🚀 Mari belajar machine learning 🤖 bersama-sama! ❤️") == False
    
def test_script_detection_short_mixed():
    # Pastikan campuran asing tapi dominan huruf yang didukung tidak ditolak
    assert is_unknown_script("Halo ini tes campuran 한국어 sedikit.") == False
