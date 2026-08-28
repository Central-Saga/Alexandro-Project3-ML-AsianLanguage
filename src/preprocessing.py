import unicodedata
import re

def clean_text(text: str) -> str:
    """
    Membersihkan teks dan melakukan normalisasi Unicode NFKC.
    """
    if not isinstance(text, str):
        return ""
    text = unicodedata.normalize('NFKC', text).strip()
    if re.match(r'^[\W\d_]+$', text):
        return ""
    return text
