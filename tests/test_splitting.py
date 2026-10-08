import pandas as pd
import pytest

from src.splitting import stratified_group_split


def test_stratified_group_split_has_no_leakage_and_all_labels():
    frame = pd.DataFrame(
        {
            "group_id": [f"id-{language}-{index}" for language in ("a", "b") for index in range(20)],
            "language": [language for language in ("a", "b") for _ in range(20)],
        }
    )

    train, validation, test = stratified_group_split(frame, "group_id")

    assert train.isdisjoint(validation)
    assert train.isdisjoint(test)
    assert validation.isdisjoint(test)
    assert train | validation | test == set(frame["group_id"])

    for split in (train, validation, test):
        labels = set(frame.loc[frame["group_id"].isin(split), "language"])
        assert labels == {"a", "b"}


def test_stratified_group_split_rejects_mixed_label_group():
    frame = pd.DataFrame(
        {
            "group_id": ["shared", "shared"] + [f"a-{i}" for i in range(4)] + [f"b-{i}" for i in range(4)],
            "language": ["a", "b"] + ["a"] * 4 + ["b"] * 4,
        }
    )

    with pytest.raises(ValueError, match="lebih dari satu label"):
        stratified_group_split(frame, "group_id")
