from collections.abc import Mapping
from datetime import date, datetime
from decimal import Decimal
from typing import Any, Self

RowValue = str | int | float | Decimal | date | datetime | bool | None


class RowAdapter:
    def __init__(self, kv: dict[str, RowValue], loaded_from_file: bool = False) -> None:
        self.kv = kv
        self.loaded_from_file = loaded_from_file

    def __getitem__(self, key: str) -> Any:  # noqa: ANN401
        return self.kv[key]

    def __setitem__(self, key: str, new_value: RowValue) -> None:
        if key not in self.kv:
            self.kv[key] = new_value

    def __contains__(self, key: str) -> bool:
        return key in self.kv

    def get(self, key: str, default: Any = None) -> Any:  # noqa: ANN401
        return self.kv.get(key, default)

    def extend(self, other: Mapping[str, RowValue] | Self, overwrite: bool = False) -> None:
        other_dict: Mapping[str, RowValue] = other.kv if isinstance(other, RowAdapter) else other

        if overwrite:
            self.kv.update(other_dict)
            return

        for key, value in other_dict.items():
            if key not in self.kv:
                self.kv[key] = value
