import pandas as pd
from sklearn.model_selection import train_test_split


def stratified_group_split(
    dataframe: pd.DataFrame,
    group_column: str,
    label_column: str = "language",
    random_state: int = 42,
) -> tuple[set, set, set]:
    """Create 70/15/15 splits without group leakage and with label balance."""
    labels_per_group = dataframe.groupby(group_column)[label_column].nunique()
    mixed_groups = labels_per_group[labels_per_group > 1]
    if not mixed_groups.empty:
        raise ValueError(
            f"Ditemukan {len(mixed_groups)} grup dengan lebih dari satu label."
        )

    grouped = dataframe[[group_column, label_column]].drop_duplicates(group_column)
    group_counts = grouped[label_column].value_counts()
    if (group_counts < 4).any():
        too_small = ", ".join(
            f"{label}={count}" for label, count in group_counts[group_counts < 4].items()
        )
        raise ValueError(
            "Setiap bahasa memerlukan minimal 4 grup untuk split terstratifikasi: "
            + too_small
        )

    train_groups, holdout_groups = train_test_split(
        grouped,
        test_size=0.30,
        random_state=random_state,
        stratify=grouped[label_column],
    )
    validation_groups, test_groups = train_test_split(
        holdout_groups,
        test_size=0.50,
        random_state=random_state,
        stratify=holdout_groups[label_column],
    )

    return (
        set(train_groups[group_column]),
        set(validation_groups[group_column]),
        set(test_groups[group_column]),
    )
