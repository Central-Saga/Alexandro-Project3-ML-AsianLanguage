from contextlib import asynccontextmanager
import logging
import os
import secrets
import hashlib
import hmac

import pandas as pd
from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel, Field
from typing import Optional
from fastapi import Header
import re

from src.predict import (
    ARTIFACTS_DIR,
    get_active_languages,
    load_artifacts,
    predict_with_details,
)
from src.preprocessing import clean_text
from src.script_detection import is_unknown_script
from src.database import (
    FeedbackCapacityError,
    FeedbackRateLimitError,
    get_feedback_stats,
    save_feedback,
)


LOGGER = logging.getLogger(__name__)
MINIMUM_CHARACTERS = 20

DISPLAY_NAMES = {
    "fil_tl": "Filipino/Tagalog",
    "indonesian": "Indonesia",
    "khmer": "Khmer",
    "lao": "Lao",
    "malay": "Melayu",
    "thai": "Thai",
    "vietnamese": "Vietnam",
    "japanese": "Jepang",
    "korean": "Korea",
    "chinese": "Mandarin",
    "hindi": "Hindi",
}

LANGUAGE_ALIASES = {
    "filipino": "fil_tl",
    "filipino/tagalog": "fil_tl",
    "indonesia": "indonesian",
    "melayu": "malay",
    "tagalog": "fil_tl",
    "vietnam": "vietnamese",
    "jepang": "japanese",
    "korea": "korean",
    "mandarin": "chinese",
    "tiongkok": "chinese",
}


def canonicalize_language_label(value: str) -> str:
    """Normalize display names and free-form labels into stable model codes."""
    normalized = value.strip().lower()
    if normalized in LANGUAGE_ALIASES:
        return LANGUAGE_ALIASES[normalized]

    normalized = re.sub(r"[ -]+", "_", normalized)
    if not re.fullmatch(r"[a-z0-9_]+", normalized):
        raise ValueError("Format nama bahasa tidak valid.")
    return normalized


class PredictionRequest(BaseModel):
    text: str = Field(
        ...,
        min_length=1,
        max_length=5000,
        description="Teks mentah yang ingin dideteksi.",
    )


class PredictionResponse(BaseModel):
    status: str
    language_code: str
    language_name: str
    margin_score: float
    text_length: int


class HealthResponse(BaseModel):
    status: str
    model_ready: bool
    supported_languages: list[str]

class FeedbackRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=5000)
    predicted_language: str = Field(..., min_length=1, max_length=64)
    corrected_language: str = Field(..., min_length=1, max_length=64)
    margin_score: Optional[float] = Field(None)
    is_correct: bool = Field(...)
    model_version: str = Field(..., min_length=1, max_length=64)

class FeedbackResponse(BaseModel):
    status: str
    message: str

class LanguageResponse(BaseModel):
    languages: list[str]


@asynccontextmanager
async def lifespan(app: FastAPI):
    try:
        load_artifacts()
        app.state.model_ready = True
    except Exception:
        LOGGER.exception("Model artifacts gagal dimuat.")
        app.state.model_ready = False

    # Load confidence threshold
    app.state.threshold = -0.018028416157938577
    try:
        import json
        threshold_path = ARTIFACTS_DIR / "confidence_threshold.json"
        if threshold_path.exists():
            with open(threshold_path, "r", encoding="utf-8") as f:
                threshold_config = json.load(f)
                app.state.threshold = threshold_config.get("threshold", -0.018028416157938577)
    except Exception:
        LOGGER.exception("Gagal membaca confidence_threshold.json, menggunakan fallback.")

    yield


app = FastAPI(
    title="Asian Language Identification API",
    description=(
        "API deteksi bahasa Asia menggunakan "
        "Character TF-IDF dan LinearSVC."
    ),
    version="1.0.0",
    lifespan=lifespan,
)


@app.get("/")
def root():
    return {
        "message": "Asian Language Identification API",
        "docs": "/docs",
    }


@app.get(
    "/health",
    response_model=HealthResponse,
)
def health():
    if not app.state.model_ready:
        raise HTTPException(
            status_code=503,
            detail="Model belum siap digunakan.",
        )

    return HealthResponse(
        status="ok",
        model_ready=True,
        supported_languages=list(DISPLAY_NAMES.values()),
    )


@app.post(
    "/predict",
    response_model=PredictionResponse,
)
def predict_language(
    payload: PredictionRequest,
):
    if not app.state.model_ready:
        raise HTTPException(
            status_code=503,
            detail="Model belum siap digunakan.",
        )

    cleaned_text = clean_text(payload.text)

    if not cleaned_text:
        raise HTTPException(
            status_code=422,
            detail="Teks kosong atau hanya berisi angka/simbol.",
        )

    if len(cleaned_text) < MINIMUM_CHARACTERS:
        raise HTTPException(
            status_code=422,
            detail=(
                "Masukkan teks minimal "
                f"{MINIMUM_CHARACTERS} karakter."
            ),
        )

    if is_unknown_script(cleaned_text):
        return PredictionResponse(
            status="unknown",
            language_code="unknown",
            language_name="Bahasa tidak dikenali",
            margin_score=0.0,
            text_length=len(cleaned_text),
        )

    try:
        input_df = pd.DataFrame(
            {
                "text": [cleaned_text],
            }
        )

        result = predict_with_details(input_df)[0]
        margin_score = result["margin_score"]

        threshold = getattr(
            app.state,
            "threshold",
            -0.018028416157938577,
        )

        if margin_score < threshold:
            return PredictionResponse(
                status="unknown",
                language_code="unknown",
                language_name="Bahasa tidak dikenali",
                margin_score=margin_score,
                text_length=len(cleaned_text),
            )

        language_code = result["label"]

        return PredictionResponse(
            status="success",
            language_code=language_code,
            language_name=DISPLAY_NAMES.get(
                language_code,
                language_code,
            ),
            margin_score=margin_score,
            text_length=len(cleaned_text),
        )
    except HTTPException:
        raise
    except Exception as exc:
        LOGGER.exception("Prediksi bahasa gagal.")

        raise HTTPException(
            status_code=500,
            detail="Terjadi kesalahan saat menjalankan prediksi.",
        ) from exc

@app.get("/languages", response_model=LanguageResponse)
def get_languages():
    if not app.state.model_ready:
        raise HTTPException(status_code=503, detail="Model belum siap.")
    active_langs = get_active_languages()
    return LanguageResponse(languages=active_langs)

@app.post("/feedback", response_model=FeedbackResponse)
def submit_feedback(
    payload: FeedbackRequest,
    request: Request,
    feedback_token: Optional[str] = Header(None, alias="X-Feedback-Token"),
):
    if not app.state.model_ready:
        raise HTTPException(status_code=503, detail="Model belum siap.")

    configured_token = os.getenv("FEEDBACK_API_TOKEN")
    if not configured_token:
        raise HTTPException(status_code=503, detail="Feedback belum dikonfigurasi.")
    if configured_token == os.getenv("ADMIN_API_TOKEN"):
        raise HTTPException(status_code=503, detail="Token feedback dan admin harus berbeda.")
    if not feedback_token or not secrets.compare_digest(
        feedback_token, configured_token
    ):
        raise HTTPException(status_code=401, detail="Unauthorized")

    cleaned_text = clean_text(payload.text)
    if not cleaned_text or len(cleaned_text) < MINIMUM_CHARACTERS:
        raise HTTPException(status_code=422, detail="Teks tidak valid atau terlalu pendek.")

    try:
        predicted_lang = canonicalize_language_label(payload.predicted_language)
        corrected_lang = canonicalize_language_label(payload.corrected_language)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc

    try:
        client_host = request.client.host if request.client else "unknown"
        source_hash = hmac.new(
            configured_token.encode("utf-8"),
            client_host.encode("utf-8"),
            hashlib.sha256,
        ).hexdigest()
        res = save_feedback(
            text=cleaned_text,
            predicted_language=predicted_lang,
            corrected_language=corrected_lang,
            margin_score=payload.margin_score,
            is_correct=payload.is_correct,
            model_version=payload.model_version,
            source_hash=source_hash,
        )
        if res == -1:
            return FeedbackResponse(status="ignored", message="Feedback duplikat.")
        return FeedbackResponse(status="success", message="Feedback berhasil disimpan.")
    except (FeedbackRateLimitError, FeedbackCapacityError) as exc:
        raise HTTPException(status_code=429, detail=str(exc)) from exc
    except Exception as exc:
        LOGGER.exception("Gagal menyimpan feedback.")
        raise HTTPException(status_code=500, detail="Gagal menyimpan feedback.") from exc

@app.get("/feedback/stats")
def feedback_stats(authorization: Optional[str] = Header(None)):
    admin_token = os.getenv("ADMIN_API_TOKEN")
    if not admin_token:
        raise HTTPException(
            status_code=503,
            detail="Admin API belum dikonfigurasi.",
        )
    if admin_token == os.getenv("FEEDBACK_API_TOKEN"):
        raise HTTPException(status_code=503, detail="Token feedback dan admin harus berbeda.")

    expected_authorization = f"Bearer {admin_token}"
    if not authorization or not secrets.compare_digest(
        authorization,
        expected_authorization,
    ):
        raise HTTPException(status_code=401, detail="Unauthorized")
    try:
        stats = get_feedback_stats()
        return stats
    except Exception as exc:
        LOGGER.exception("Gagal mengambil statistik.")
        raise HTTPException(status_code=500, detail="Gagal mengambil statistik.") from exc
