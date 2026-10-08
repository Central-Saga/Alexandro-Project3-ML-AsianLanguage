import json
import os
import shutil
from pathlib import Path

import joblib


BASE_DIR = Path(__file__).resolve().parent.parent
ACTIVE_ARTIFACTS_DIR = BASE_DIR / "artifacts"
CANDIDATE_ARTIFACTS_DIR = BASE_DIR / "artifacts_candidate"
BACKUP_ARTIFACTS_DIR = BASE_DIR / "artifacts_backup"
STAGING_ARTIFACTS_DIR = BASE_DIR / "artifacts_staging"
PREVIOUS_ARTIFACTS_DIR = BASE_DIR / "artifacts_previous"

REQUIRED_ARTIFACTS = {
    "model.pkl",
    "preprocessor.pkl",
    "labels.json",
    "config.json",
    "confidence_threshold.json",
}


def _load_json(path: Path):
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def _validate_artifact_directory(path: Path) -> tuple[dict, list[str]]:
    missing = sorted(name for name in REQUIRED_ARTIFACTS if not (path / name).is_file())
    if missing:
        raise ValueError(f"Artifact tidak lengkap: {', '.join(missing)}")

    config = _load_json(path / "config.json")
    labels = _load_json(path / "labels.json")
    if not isinstance(labels, list) or not labels or len(labels) != len(set(labels)):
        raise ValueError("labels.json harus berisi daftar label unik yang tidak kosong.")

    model = joblib.load(path / "model.pkl")
    joblib.load(path / "preprocessor.pkl")
    model_labels = set(getattr(model, "classes_", []))
    if model_labels != set(labels):
        raise ValueError("Label model tidak cocok dengan labels.json.")

    threshold_config = _load_json(path / "confidence_threshold.json")
    if not isinstance(threshold_config.get("threshold"), (int, float)):
        raise ValueError("confidence_threshold.json tidak memiliki threshold numerik.")

    return config, labels


def _candidate_passes_quality_gate(
    candidate_config: dict,
    candidate_labels: list[str],
    active_labels: list[str],
    f1_tolerance: float,
    max_class_drop: float,
) -> bool:
    missing_active_labels = sorted(set(active_labels) - set(candidate_labels))
    if missing_active_labels:
        print(
            "TOLAK: Kandidat menghilangkan label aktif: "
            + ", ".join(missing_active_labels)
        )
        return False

    candidate_f1 = candidate_config.get("macro_f1")
    baseline_f1 = candidate_config.get("baseline_macro_f1")
    baseline_report = candidate_config.get("baseline_classification_report")
    candidate_report = candidate_config.get("classification_report")
    if not isinstance(candidate_f1, (int, float)):
        print("TOLAK: Kandidat tidak memiliki macro_f1 yang valid.")
        return False
    if not isinstance(baseline_f1, (int, float)) or not isinstance(
        baseline_report,
        dict,
    ):
        print(
            "TOLAK: Kandidat belum dievaluasi berdampingan dengan model aktif. "
            "Jalankan ulang src.retrain."
        )
        return False
    if not isinstance(candidate_report, dict):
        print("TOLAK: Kandidat tidak memiliki classification_report.")
        return False

    print(
        f"Baseline Macro F1: {baseline_f1:.4f} | "
        f"Candidate Macro F1: {candidate_f1:.4f}"
    )
    if candidate_f1 < baseline_f1 - f1_tolerance:
        print(f"TOLAK: Macro F1 turun lebih dari {f1_tolerance:.4f}.")
        return False

    for language in active_labels:
        baseline_metrics = baseline_report.get(language, {})
        candidate_metrics = candidate_report.get(language, {})
        support = baseline_metrics.get("support", 0)
        if not support:
            continue
        old_f1 = baseline_metrics.get("f1-score")
        new_f1 = candidate_metrics.get("f1-score")
        if not isinstance(old_f1, (int, float)) or not isinstance(
            new_f1,
            (int, float),
        ):
            print(f"TOLAK: Metrik kelas '{language}' tidak lengkap.")
            return False
        if new_f1 < old_f1 - max_class_drop:
            print(
                f"TOLAK: F1 '{language}' turun terlalu jauh "
                f"({old_f1:.4f} -> {new_f1:.4f})."
            )
            return False

    return True


def _remove_internal_temp(path: Path) -> None:
    if path.exists():
        shutil.rmtree(path)


def _atomic_activate(source: Path) -> None:
    _remove_internal_temp(STAGING_ARTIFACTS_DIR)
    _remove_internal_temp(PREVIOUS_ARTIFACTS_DIR)
    shutil.copytree(source, STAGING_ARTIFACTS_DIR)
    _validate_artifact_directory(STAGING_ARTIFACTS_DIR)

    if not ACTIVE_ARTIFACTS_DIR.exists():
        os.replace(STAGING_ARTIFACTS_DIR, ACTIVE_ARTIFACTS_DIR)
        return

    os.replace(ACTIVE_ARTIFACTS_DIR, PREVIOUS_ARTIFACTS_DIR)
    try:
        os.replace(STAGING_ARTIFACTS_DIR, ACTIVE_ARTIFACTS_DIR)
    except Exception:
        os.replace(PREVIOUS_ARTIFACTS_DIR, ACTIVE_ARTIFACTS_DIR)
        raise

    if BACKUP_ARTIFACTS_DIR.exists():
        shutil.rmtree(BACKUP_ARTIFACTS_DIR)
    os.replace(PREVIOUS_ARTIFACTS_DIR, BACKUP_ARTIFACTS_DIR)


def promote_candidate(
    f1_tolerance: float = 0.02,
    max_class_drop: float = 0.10,
) -> bool:
    """Promote a complete candidate only after same-holdout quality checks."""
    if not CANDIDATE_ARTIFACTS_DIR.exists():
        print("Tidak ada model kandidat.")
        return False

    try:
        candidate_config, candidate_labels = _validate_artifact_directory(
            CANDIDATE_ARTIFACTS_DIR
        )
    except Exception as exc:
        print(f"TOLAK: Model kandidat tidak valid: {exc}")
        return False

    if ACTIVE_ARTIFACTS_DIR.exists():
        try:
            _, active_labels = _validate_artifact_directory(ACTIVE_ARTIFACTS_DIR)
        except Exception as exc:
            print(f"TOLAK: Model aktif tidak valid: {exc}")
            return False

        if not _candidate_passes_quality_gate(
            candidate_config,
            candidate_labels,
            active_labels,
            f1_tolerance,
            max_class_drop,
        ):
            return False
    else:
        print("Model aktif tidak ditemukan; kandidat valid akan dipromosikan.")

    print("Mempromosikan kandidat secara atomik...")
    try:
        _atomic_activate(CANDIDATE_ARTIFACTS_DIR)
    except Exception as exc:
        print(f"Promosi gagal; model aktif dipertahankan: {exc}")
        return False

    print("Kandidat berhasil dipromosikan. Restart API untuk memuat model baru.")
    return True


def rollback() -> bool:
    """Restore the last backup without consuming it."""
    if not BACKUP_ARTIFACTS_DIR.exists():
        print("Backup tidak ditemukan.")
        return False

    try:
        _validate_artifact_directory(BACKUP_ARTIFACTS_DIR)
        _atomic_activate(BACKUP_ARTIFACTS_DIR)
    except Exception as exc:
        print(f"Rollback gagal; model aktif dipertahankan: {exc}")
        return False

    print("Rollback berhasil. Restart API untuk memuat model lama.")
    return True


if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1 and sys.argv[1] == "rollback":
        rollback()
    else:
        promote_candidate()
