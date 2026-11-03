#!/usr/bin/env python

import json
from typing import Any


def check_type(key:str, base:dict,value:Any, cls:type):
    return key in base and isinstance(base[key], cls) and isinstance(value, cls)

def deep_merge(base: dict, override: dict) -> dict:
    for key, value in override.items():
        # For dicts, recursively merge items
        if check_type(key, base,value, dict):
            deep_merge(base[key], value)
        # For lists, add any items that don't exist in the base
        elif check_type(key, base, value, list):
            for v in value:
                if v not in base[key]:
                    base[key].append(v)
        else:
            base[key] = value
    return base


def safe_read_file(name: str) -> str:
    try:
        with open(name) as f:
            return f.read()
    except FileNotFoundError:
        return ""


def safe_read_json(name: str) -> Any:
    return json.loads(safe_read_file(name) or "{}")


def merge_files(main: str, workspace: str) -> None:
    merged = json.dumps(
        deep_merge(safe_read_json(main), safe_read_json(workspace)), indent=2
    )
    with open(main, "w") as f:
        f.write(merged)


# VS Code currently has no built-in way of sharing settings across multiple files
# Some things you might not want to check in to the repo, so here we merge a workspace file
# with the file read by VS Code, allowing us to keep workspace settings separate
# from your personal settings, which remain gitignored.
merge_files("./.vscode/launch.json", "./.vscode/workspace_launch.json")
merge_files("./.vscode/settings.json", "./.vscode/workspace_settings.json")
