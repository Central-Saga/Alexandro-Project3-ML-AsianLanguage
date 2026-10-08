import unicodedata

def is_supported_letter(c: str) -> bool:
    """
    Memeriksa apakah sebuah karakter berada dalam blok Unicode aksara yang didukung:
    - Latin (Basic, Supplement, Extended A/B, IPA, Extended Additional)
    - Thai
    - Lao
    - Khmer
    """
    code = ord(c)
    # Basic Latin, Latin-1, Latin Extended A and B, IPA Extensions
    if 0x0041 <= code <= 0x005A or 0x0061 <= code <= 0x007A:
        return True
    if 0x00C0 <= code <= 0x02AF: 
        return True
    if 0x1E00 <= code <= 0x1EFF: # Latin Extended Additional (untuk Vietnam)
        return True
    
    # Thai
    if 0x0E00 <= code <= 0x0E7F:
        return True
    # Lao
    if 0x0E80 <= code <= 0x0EFF:
        return True
    # Khmer
    if 0x1780 <= code <= 0x17FF or 0x19E0 <= code <= 0x19FF:
        return True
        
    # Japanese (Hiragana, Katakana, Kanji)
    if 0x3040 <= code <= 0x309F: # Hiragana
        return True
    if 0x30A0 <= code <= 0x30FF: # Katakana
        return True
    if 0x4E00 <= code <= 0x9FFF: # CJK Unified Ideographs (Kanji)
        return True
        
    # Korean (Hangul)
    if 0x1100 <= code <= 0x11FF: # Jamo
        return True
    if 0x3130 <= code <= 0x318F: # Compatibility Jamo
        return True
    if 0xAC00 <= code <= 0xD7AF: # Syllables
        return True

    # Hindi (Devanagari)
    if 0x0900 <= code <= 0x097F:
        return True

    # Arabic, Persian, Urdu
    if 0x0600 <= code <= 0x06FF or 0x0750 <= code <= 0x077F or 0x08A0 <= code <= 0x08FF:
        return True

    # Hebrew
    if 0x0590 <= code <= 0x05FF:
        return True
        
    return False

def is_unknown_script(text: str) -> bool:
    """
    Mengembalikan True jika teks didominasi oleh aksara yang tidak didukung
    (misal Hangul, Hiragana, Cyrillic, Arabic).
    Syarat "unknown": 
    - Minimal ada 2 karakter huruf unsupported
    - Rasio huruf unsupported minimal 50% dari total karakter kategori huruf
    """
    total_letters = 0
    unsupported_letters = 0
    
    for c in text:
        cat = unicodedata.category(c)
        if cat.startswith('L'):
            total_letters += 1
            if not is_supported_letter(c):
                unsupported_letters += 1
                
    if total_letters == 0:
        return False
        
    if unsupported_letters >= 2 and (unsupported_letters / total_letters) >= 0.5:
        return True
        
    return False
