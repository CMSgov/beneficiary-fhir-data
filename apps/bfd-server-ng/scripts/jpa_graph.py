# /// script
# requires-python = ">=3.14"
# dependencies = [
#     "pyyaml>=6.0.3",
# ]
# ///

# Run using uv jpa_grapy.py

import re
from pathlib import Path

import yaml

JAVA_SRC_DIR = Path("../src/main/java/gov/cms/bfd/server/ng")
YAML_DICTS_DIR = "../src/main/resources/dictionary-support-files"

# Parsing regexes
CLASS_RE = re.compile(r"(?P<anno>@Entity|@MappedSuperclass|@Embeddable)[\s\S]*?class\s+(?P<name>\w+)(?:\s+extends\s+(?P<parent>\w+))?")
TABLE_RE = re.compile(r'@Table\s*\([^)]*?name\s*=\s*"([^"]+)"')
COLUMN_RE = re.compile(r'@Column\([\s\S]*?name\s*=\s*"([^"]+)"[\s\S]*?\)[\s\S]*?(?:private|protected|public)?\s+([\w<>(),.? ]+)\s+(\w+);')
EMBEDDED_RE = re.compile(r"@Embedded\s+[\s\S]*?(?:private|protected|public)?\s+([\w<>(),.? ]+)\s+(\w+);")


DEFAULT_PROFILES = ["Basis", "Regular", "CMS (Implicit Default)"]

def load_profile_mappings(yaml_dir: str) -> dict:
    """Maps uppercase database column names to their configuration profiles."""
    mappings = {}
    base_path = Path(yaml_dir)
    if not base_path.exists():
        return mappings

    for file_path in base_path.rglob("*.y*ml"):
        with open(file_path, "r", encoding="utf-8") as f:
            try:
                for entry in (yaml.safe_load(f) or []):
                    if source_col := entry.get("sourceColumn"):
                        mappings[source_col.upper()] = entry.get("profiles") or DEFAULT_PROFILES
            except yaml.YAMLError:
                continue
    return mappings

def parse_java_graph(src_dir: Path) -> dict:
    """Extracts inheritance structures, field columns, embedded's, and table definitions."""
    graph = {}
    if not src_dir.exists():
        return graph

    for file_path in src_dir.rglob("*.java"):
        content = file_path.read_text(encoding="utf-8")
        if not (class_match := CLASS_RE.search(content)):
            continue

        name = class_match.group("name")
        table_match = TABLE_RE.search(content)

        graph[name] = {
            "type": class_match.group("anno").replace("@", ""),
            "extends": class_match.group("parent"),
            "table": table_match.group(1) if table_match else None,
            "columns": [{"db_col": c[0], "java_var": c[2]} for c in COLUMN_RE.findall(content)],
            "embeddeds": [{"type": e[0], "java_var": e[1]} for e in EMBEDDED_RE.findall(content)],
        }
    return graph

def print_tree(graph: dict, profile_map: dict, class_name: str, indent: str = "", visited: set = None):
    """Recursively traces and formats embedded attributes down the graph."""
    visited = visited or set()
    if class_name in visited or class_name not in graph:
        return
    visited.add(class_name)

    node = graph[class_name]
    for col in node["columns"]:
        profiles = profile_map.get(col["db_col"].upper(), DEFAULT_PROFILES)
        print(f"{indent}├── [Column] {col['db_col']} ({col['java_var']}) ──► [{', '.join(profiles)}]")

    for emb in node["embeddeds"]:
        print(f"{indent}└── [Embedded] {emb['java_var']} ──► Type: {emb['type']}")
        print_tree(graph, profile_map, emb["type"], indent + "    │", visited.copy())

def main():
    profile_map = load_profile_mappings(YAML_DICTS_DIR)
    graph = parse_java_graph(JAVA_SRC_DIR)

    # Filter out pure utility helpers/embeds; map out entry hierarchies
    roots = [name for name, data in graph.items() if data["type"] in ("Entity", "MappedSuperclass")]
    roots.sort(key=lambda x: (graph[x]["extends"] or "", x))

    for name in roots:
        node = graph[name]
        inheritance = f" extends {node['extends']}" if node["extends"] else ""
        table_info = f" ──► Physical Table: {node['table']}" if node["table"] else ""

        print(f"[{node['type']}] {name}{inheritance}{table_info}")
        print_tree(graph, profile_map, name, indent=" ")
        print(f"\n{'-' * 80}\n")

if __name__ == "__main__":
    main()