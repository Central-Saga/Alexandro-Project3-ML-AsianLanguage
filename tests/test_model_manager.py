from src.model_manager import _candidate_passes_quality_gate


def _report(score=0.9, support=10):
    return {
        "indonesian": {"f1-score": score, "support": support},
        "malay": {"f1-score": score, "support": support},
    }


def test_quality_gate_rejects_missing_active_label():
    assert not _candidate_passes_quality_gate(
        {
            "macro_f1": 0.95,
            "baseline_macro_f1": 0.90,
            "classification_report": _report(),
            "baseline_classification_report": _report(),
        },
        candidate_labels=["indonesian"],
        active_labels=["indonesian", "malay"],
        f1_tolerance=0.02,
        max_class_drop=0.10,
    )


def test_quality_gate_requires_same_holdout_baseline():
    assert not _candidate_passes_quality_gate(
        {"macro_f1": 0.95, "classification_report": _report()},
        candidate_labels=["indonesian", "malay"],
        active_labels=["indonesian", "malay"],
        f1_tolerance=0.02,
        max_class_drop=0.10,
    )


def test_quality_gate_rejects_extra_label():
    assert not _candidate_passes_quality_gate(
        {
            "macro_f1": 0.92,
            "baseline_macro_f1": 0.91,
            "classification_report": _report(score=0.92),
            "baseline_classification_report": _report(score=0.91),
        },
        candidate_labels=["indonesian", "malay", "javanese"],
        active_labels=["indonesian", "malay"],
        f1_tolerance=0.02,
        max_class_drop=0.10,
    )


def test_quality_gate_accepts_non_regressing_candidate():
    assert _candidate_passes_quality_gate(
        {
            "macro_f1": 0.92,
            "baseline_macro_f1": 0.91,
            "classification_report": _report(score=0.92),
            "baseline_classification_report": _report(score=0.91),
        },
        candidate_labels=["indonesian", "malay"],
        active_labels=["indonesian", "malay"],
        f1_tolerance=0.02,
        max_class_drop=0.10,
    )
