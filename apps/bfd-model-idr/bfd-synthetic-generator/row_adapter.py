from typing import Any, Self


class RowAdapter:
    def __init__(self, kv: dict[str, Any], loaded_from_file: bool = False):
        self.kv = kv
        self.loaded_from_file = loaded_from_file

    def __getitem__(self, key: str) -> Any:
        return self.kv[key]

    def __setitem__(self, key: str, new_value: Any) -> None:
        if key not in self.kv:
            self.kv[key] = new_value

    def __contains__(self, key: str) -> bool:
        return key in self.kv

    def get(self, key: str, default: Any | None = None) -> Any:
        return self.kv.get(key, default)

    def extend(self, other: dict[str, Any] | Self, overwrite: bool = False) -> None:
        cur = self if not overwrite else self.kv
        other_dict = other if isinstance(other, dict) else other.kv

        for k, v in other_dict.items():
            cur[k] = v
