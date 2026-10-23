from typing import Any, Self


class RowAdapter:
    def __init__(self, kv: dict[str | int, Any], loaded_from_file: bool = False):
        self.kv = kv
        self.loaded_from_file = loaded_from_file

    def __getitem__(self, key: str | int):
        return self.kv[key]

    def __setitem__(self, key: str | int, new_value: Any):
        if key not in self.kv:
            self.kv[key] = new_value

    def __contains__(self, key: str | int) -> bool:
        return key in self.kv

    def get(self, key: str | int, default: Any | None = None) -> Any:
        return self.kv.get(key, default)

    def extend(self, other: dict[str | int, Any] | Self, overwrite: bool = False):
        cur = self if not overwrite else self.kv
        other_dict = other if isinstance(other, dict) else other.kv

        for k, v in other_dict.items():
            cur[k] = v
