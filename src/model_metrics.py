import numpy as np


def decision_confidence_scores(model, features) -> np.ndarray:
    """Return the winning-class score used by the API for rejection."""
    if hasattr(model, "decision_function"):
        scores = np.asarray(model.decision_function(features))
        if scores.ndim == 1:
            return np.abs(scores)
        return np.max(scores, axis=1)

    probabilities = np.asarray(model.predict_proba(features))
    return np.max(probabilities, axis=1)


def calibrate_confidence_threshold(
    model,
    validation_features,
    percentile: float = 1.0,
) -> float:
    """Calibrate the rejection floor from held-out in-distribution samples."""
    scores = decision_confidence_scores(model, validation_features)
    if scores.size == 0:
        raise ValueError("Validation set kosong; threshold tidak dapat dikalibrasi.")
    return float(np.percentile(scores, percentile))
