import os
import re

import yaml

# Adjust these paths to match your local module filesystem structure
JAVA_SRC_DIR = "./src/main/java/gov/cms/bfd/server/ng"
YAML_DICTS_DIR = "./src/main/resources/dictionary-support-files"

# Multi-line safe regex patterns for Java parsing
CLASS_PATTERN = re.compile(
    r"(?P<anno>@Entity|@MappedSuperclass|@Embeddable)[\s\S]*?class\s+(?P<name>\w+)(?:\s+extends\s+(?P<parent>\w+))?"
)
COLUMN_PATTERN = re.compile(
    r'@Column\([\s\S]*?name\s*=\s*"([^"]+)"[\s\S]*?\)(?:[\s\S]*?)(?:private|protected|public)?\s+([\w<>(),.? ]+)\s+(\w+);'
)
EMBEDDED_PATTERN = re.compile(
    r"@Embedded\s+(?:[\s\S]*?)(?:private|protected|public)?\s+([\w<>(),.? ]+)\s+(\w+);"
)


def load_yaml_profile_mappings(yaml_dir):
    """
    Parses all YAML dictionaries to map UPPERCASE_COLUMN_NAME -> list of profiles.
    Follows the defaulting rule: if profiles block is missing, it defaults to ALL profiles.
    """
    column_profile_map = {}
    if not os.path.exists(yaml_dir):
        print(f"[Warning] YAML dictionary path not found: {yaml_dir}")
        return column_profile_map

    for root, _, files in os.walk(yaml_dir):
        for file in files:
            if file.endswith((".yaml", ".yml")):
                with open(os.path.join(root, file), "r", encoding="utf-8") as f:
                    try:
                        entries = yaml.safe_load(f) or []
                        for entry in entries:
                            source_col = entry.get("sourceColumn")
                            if source_col:
                                upper_col = source_col.upper()
                                profiles = entry.get("profiles")

                                # Rule: If missing, implicitly defaults to every profile
                                if not profiles:
                                    profiles = [
                                        "Basis",
                                        "Regular",
                                        "CMS (Implicit Default)",
                                    ]

                                column_profile_map[upper_col] = profiles
                    except Exception as e:
                        print(f"[Error] Failed to parse YAML file {file}: {e}")

    return column_profile_map


def parse_java_module(directory):
    """Scans Java classes to capture structure, inheritance, columns, and embeddeds."""
    class_graph = {}
    if not os.path.exists(directory):
        print(f"[Error] Java source directory not found: {directory}")
        return class_graph

    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith(".java"):
                with open(os.path.join(root, file), "r", encoding="utf-8") as f:
                    content = f.read()

                    class_match = CLASS_PATTERN.search(content)
                    if not class_match:
                        continue

                    anno = class_match.group("anno")
                    class_name = class_match.group("name")
                    parent_name = class_match.group("parent")

                    columns = COLUMN_PATTERN.findall(content)
                    embeddeds = EMBEDDED_PATTERN.findall(content)

                    class_graph[class_name] = {
                        "type": anno.replace("@", ""),
                        "extends": parent_name,
                        "columns": [
                            {"db_col": c[0], "java_var": c[2]} for c in columns
                        ],
                        "embeddeds": [
                            {"type": e[0], "java_var": e[1]} for e in embeddeds
                        ],
                    }
    return class_graph


def render_tree(graph, profile_map, class_name, indent="", visited=None):
    """Recursively builds the print tree, cross-referencing columns with their profile whitelists."""
    if visited is None:
        visited = set()
    if class_name in visited or class_name not in graph:
        return
    visited.add(class_name)

    data = graph[class_name]

    # Render columns with matched profile tags appended
    for col in data["columns"]:
        upper_col = col["db_col"].upper()

        # Pull profile list from the YAML dictionary map or apply global default fallback
        profiles = profile_map.get(
            upper_col, ["Basis", "Regular", "CMS (Implicit Default)"]
        )
        profiles_str = ", ".join(profiles)

        print(
            f"{indent}├── [Column] {col['db_col']} ({col['java_var']}) ──► Profiles: [{profiles_str}]"
        )

    # Render nested objects (Composition paths)
    for emb in data["embeddeds"]:
        emb_type = emb["type"]
        print(f"{indent}└── [Embedded] {emb['java_var']} ──► Type: {emb_type}")
        render_tree(graph, profile_map, emb_type, indent + "    │", visited.copy())


def execute_profile_graph_analysis():
    print("=========================================================================")
    print("               JPA ENTITY COMPOSITION & PROFILE TREE GRAPH               ")
    print("=========================================================================\n")

    # 1. Harvest whitelist tracking metadata from configuration documents
    profile_map = load_yaml_profile_mappings(YAML_DICTS_DIR)
    print(
        f"Successfully cached {len(profile_map)} column whitelists from YAML definitions.\n"
    )

    # 2. Extract architectural frameworks from the module
    graph = parse_java_module(JAVA_SRC_DIR)

    # 3. Establish processing targets (Entities and Superclasses)
    root_nodes = [
        name
        for name, data in graph.items()
        if data["type"] in ["Entity", "MappedSuperclass"]
    ]
    root_nodes.sort(key=lambda x: (graph[x]["extends"] or "", x))

    for root in root_nodes:
        meta = graph[root]
        parent_info = f" extends {meta['extends']}" if meta["extends"] else ""
        print(f"[{meta['type']}] {root}{parent_info}")
        render_tree(graph, profile_map, root, indent=" ")
        print("\n" + "─" * 85 + "\n")


if __name__ == "__main__":
    execute_profile_graph_analysis()
