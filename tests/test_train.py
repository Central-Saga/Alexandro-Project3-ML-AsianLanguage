import json
import subprocess

import pandas as pd


def test_train_creates_complete_artifact_bundle(tmp_path):
    dataset = pd.DataFrame(
        {
            "text": [f"Kalimat Indonesia unik nomor {i}" for i in range(20)]
            + [f"Ayat Melayu unik nombor {i}" for i in range(20)],
            "language": ["indonesian"] * 20 + ["malay"] * 20,
        }
    )
    dataset_path = tmp_path / "training.csv"
    output_path = tmp_path / "artifacts"
    dataset.to_csv(dataset_path, index=False)

    result = subprocess.run(
        [
            "python",
            "src/train.py",
            "--data-path",
            str(dataset_path),
            "--output-dir",
            str(output_path),
        ],
        capture_output=True,
        text=True,
    )

    assert result.returncode == 0, result.stderr
    expected = {
        "model.pkl",
        "preprocessor.pkl",
        "labels.json",
        "config.json",
        "confidence_threshold.json",
    }
    assert expected.issubset(path.name for path in output_path.iterdir())

    with (output_path / "config.json").open(encoding="utf-8") as handle:
        config = json.load(handle)
    assert config["classification_report"]
    assert isinstance(config["confidence_threshold"], float)
