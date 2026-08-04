# /// script
# requires-python = ">=3.14"
# dependencies = [
#     "pyyaml>=6.0.3",
# ]
# ///

import difflib
import re
import sys
from pathlib import Path

import yaml

JAVA_SRC_DIR = Path(
    "/Users/lewellync/Projects/BFD/git/beneficiary-fhir-data/apps/bfd-server-ng/src/main/java/gov/cms/bfd/server/ng"
)
YAML_DICTS_DIR = Path(
    "/Users/lewellync/Projects/BFD/git/beneficiary-fhir-data/apps/bfd-server-ng/src/main/resources/dictionary-support-files"
)

CLASS_RE = re.compile(
    r"(?s)(?P<type>@Entity|@MappedSuperclass|@Embeddable).*?class\s+(?P<name>\w+)(?:\s+(?:extends|implements)\s+(?P<parent>\w+))?"
)
TABLE_RE = re.compile(r'@Table\(.*?name\s*=\s*"([^"]+)"', re.DOTALL)

# The two negative lookaheads keep this from binding a column to the wrong
# field: one bails if an @Embedded sits between the annotation and the
# field, the other refuses to skip past a semicolon, brace, or another
# Column/override annotation (otherwise it can tunnel clean through a
# class boundary and grab an unrelated field further down the file).
COLUMN_RE = re.compile(
    r'(?s)@Column\(.*?name\s*=\s*"([^"]+)".*?\)'
    r"(?!(?:(?!;).)*?@Embedded)"
    r"(?:(?!;|\{|@Column|@AttributeOverride).)*?"
    r"(?:private|protected|public)?\s+([\w<>(),.? ]+)\s+(\w+);"
)
EMBEDDED_RE = re.compile(
    r"(?s)@Embedded"
    r"(?:(?!;|\{|@Column|@AttributeOverride|@Embedded).)*?"
    r"(?:private|protected|public)?\s+([\w<>(),.? ]+)\s+(\w+);"
)
# Java lets name= and column= appear in either order, so both are matched.
ATTRIBUTE_OVERRIDE_RE = re.compile(
    r'@AttributeOverride\(\s*(?:'
    r'name\s*=\s*"(?P<name1>[^"]+)"\s*,\s*column\s*=\s*@Column\(\s*name\s*=\s*"(?P<col1>[^"]+)"\s*\)'
    r"|"
    r'column\s*=\s*@Column\(\s*name\s*=\s*"(?P<col2>[^"]+)"\s*\)\s*,\s*name\s*=\s*"(?P<name2>[^"]+)"'
    r")\s*\)"
)

JAVA_ENTITY_RE = re.compile(
    r"Claim(?P<domain>Institutional|Professional|Rx)?(?P<profile>Cms|Basis|Regular)(?P<source>Nch|SharedSystems|Rx)?",
    re.IGNORECASE,
)

DEFAULT_PROFILES = ["Basis", "Regular", "CMS (Default)"]
ALL_PROFILES = {"BASIS", "REGULAR", "CMS"}
ALL_PROFILES_ORDERED = ["BASIS", "REGULAR", "CMS"]

# Columns with no YAML entry at all (silently treated as valid for every
# profile). Reported separately at the end so a missing dictionary entry
# isn't confused with a genuinely unrestricted column.
UNMATCHED_COLUMNS: set[str] = set()

# (class, db_col) -> the distinct java_vars it's bound to, when a class's
# own columns bind the same db_col to more than one field. Sometimes
# intentional (one column exposed under two names); also the exact
# signature of a mis-parsed @AttributeOverride, so both get surfaced for
# review rather than trusted either way.
DUPLICATE_COLUMN_BINDINGS: dict[tuple[str, str], set[str]] = {}


def parse_yaml_map(yaml_dir: Path) -> dict:
    mappings = {}
    if not yaml_dir.exists():
        return mappings
    for file_path in yaml_dir.rglob("*.y*ml"):
        with open(file_path, "r", encoding="utf-8") as f:
            try:
                for entry in yaml.safe_load(f) or []:
                    if source_col := entry.get("sourceColumn"):
                        profiles = entry.get("profiles") or DEFAULT_PROFILES
                        mappings[source_col.upper()] = [p.split()[0].upper() for p in profiles]
            except yaml.YAMLError as e:
                print(f"[WARN] Failed to parse {file_path}: {e}")
    return mappings


def _hide_attribute_overrides(content: str, override_matches: list) -> str:
    """Blank out each @AttributeOverride(...) span so COLUMN_RE can't see
    (or backtrack into) the @Column nested inside it."""
    for match in override_matches:
        start, end = match.span()
        content = content[:start] + " " * (end - start) + content[end:]
    return content


def _extract_columns(content: str) -> tuple[list, str]:
    override_matches = list(ATTRIBUTE_OVERRIDE_RE.finditer(content))
    clean_content = _hide_attribute_overrides(content, override_matches)

    plain_columns = [
        {"db_col": match.group(1), "java_var": match.group(3)}
        for match in COLUMN_RE.finditer(clean_content)
    ]
    override_columns = [
        {
            "db_col": match.group("col1") or match.group("col2"),
            "java_var": match.group("name1") or match.group("name2"),
        }
        for match in override_matches
    ]
    return plain_columns + override_columns, clean_content


def _extract_embeddeds(content: str) -> list:
    embeds = []
    for type_blob, java_var in EMBEDDED_RE.findall(content):
        clean_type = re.sub(r"^(?:private|protected|public)\s+", "", type_blob).strip()
        embeds.append({"type": clean_type, "java_var": java_var})
    return embeds


def _record_duplicate_bindings(class_name: str, columns: list) -> None:
    java_vars_by_column: dict[str, set[str]] = {}
    for col in columns:
        java_vars_by_column.setdefault(col["db_col"].upper(), set()).add(col["java_var"])
    for db_col, java_vars in java_vars_by_column.items():
        if len(java_vars) > 1:
            DUPLICATE_COLUMN_BINDINGS[(class_name, db_col)] = java_vars


def parse_java_graph(src_dir: Path) -> dict:
    graph = {}
    if not src_dir.exists():
        return graph

    for file_path in src_dir.rglob("*.java"):
        content = file_path.read_text(encoding="utf-8")
        class_match = CLASS_RE.search(content)
        if not class_match:
            continue

        name = class_match.group("name")
        table_match = TABLE_RE.search(content)
        columns, clean_content = _extract_columns(content)
        _record_duplicate_bindings(name, columns)

        graph[name] = {
            "type": class_match.group("type").replace("@", ""),
            "parent": class_match.group("parent"),
            "table": table_match.group(1) if table_match else None,
            "columns": columns,
            "embeddeds": _extract_embeddeds(clean_content),
        }
    return graph


def get_class_profile(class_name: str) -> str | None:
    """Profile implied by naming convention, e.g. ClaimProfessionalCmsBase -> CMS."""
    if match := JAVA_ENTITY_RE.search(class_name):
        return match.group("profile").upper()
    return None


def resolve_inherited(graph: dict, class_name: str) -> tuple[list, list]:
    """Merge columns/embeddeds up the parent chain, so a subclass is checked
    against inherited fields too, not just its own. Each column is tagged
    declared_here=True only where it's actually written, so callers don't
    re-flag something a previous pull-up already moved."""
    columns, embeds = [], []
    seen = set()
    current = class_name
    is_declaring_class = True
    while current and current in graph and current not in seen:
        seen.add(current)
        node = graph[current]
        columns.extend({**col, "declared_here": is_declaring_class} for col in node["columns"])
        embeds.extend(node["embeddeds"])
        current = node["parent"]
        is_declaring_class = False
    return columns, embeds


def _column_flag(class_profile: str | None, allowed_profiles: list, declared_here: bool) -> str:
    if not declared_here:
        return ""
    if class_profile and class_profile not in allowed_profiles:
        return f" ⚠️  [WARNING] Column not valid for profile '{class_profile}'! Push Down/Move."
    if class_profile and set(allowed_profiles) >= ALL_PROFILES:
        return " ▲ [PULL UP candidate]"
    if not class_profile and set(allowed_profiles) < ALL_PROFILES:
        return (
            f" ▼ [PUSH DOWN candidate] Only valid for {allowed_profiles}, "
        )
    return ""


def _debug_column(col: dict, class_name: str, class_profile, allowed_profiles, in_dict: bool) -> None:
    print(
        f"[DEBUG] {col['db_col']} found on class={class_name!r} "
        f"class_profile={class_profile!r} declared_here={col['declared_here']!r} "
        f"allowed_profiles={allowed_profiles!r} in_profile_map={in_dict!r}",
        file=sys.stderr,
    )


def print_tree(
        graph: dict,
        profile_map: dict,
        class_name: str,
        class_profile: str | None,
        indent: str = "",
        visited: set = None,
        debug_col: str | None = None,
):
    visited = visited or set()
    if class_name in visited or class_name not in graph:
        return
    visited.add(class_name)

    columns, embeds = resolve_inherited(graph, class_name)

    for col in columns:
        db_col = col["db_col"].upper()
        in_dict = db_col in profile_map
        if not in_dict:
            UNMATCHED_COLUMNS.add(db_col)
        allowed_profiles = profile_map.get(db_col, ALL_PROFILES_ORDERED)

        if debug_col and db_col == debug_col:
            _debug_column(col, class_name, class_profile, allowed_profiles, in_dict)

        flag = _column_flag(class_profile, allowed_profiles, col["declared_here"])
        print(
            f"{indent}├── [Column] {col['db_col']} ({col['java_var']}) "
            f"[profiles: {', '.join(allowed_profiles)}]{flag}"
        )

    for embed in embeds:
        print(f"{indent}└── [Embedded] {embed['java_var']} ──► Type: {embed['type']}")
        print_tree(
            graph,
            profile_map,
            embed["type"],
            class_profile,
            indent + "    │",
            visited.copy(),
            debug_col=debug_col,
            )


def _print_unmatched_columns(profile_map: dict) -> None:
    if not UNMATCHED_COLUMNS:
        return
    known_columns = list(profile_map.keys())
    print(
        f"[WARN] {len(UNMATCHED_COLUMNS)} column(s) had no YAML dictionary entry "
        "and defaulted to 'all profiles valid' -- check for typos or missing "
        "dictionary files. A suggested match means it's likely a naming "
        "mismatch rather than a real gap:"
    )
    for col in sorted(UNMATCHED_COLUMNS):
        close = difflib.get_close_matches(col, known_columns, n=3, cutoff=0.6)
        suggestion = f" (possible match: {', '.join(close)})" if close else ""
        print(f"    - {col}{suggestion}")


def _print_duplicate_bindings() -> None:
    if not DUPLICATE_COLUMN_BINDINGS:
        return
    print(
        f"\n[WARN] {len(DUPLICATE_COLUMN_BINDINGS)} class/column pair(s) bind the "
        "same db column to multiple field names. Could be intentional aliasing, "
        "or a parsing mis-attribution -- review each:"
    )
    for (cls, db_col), java_vars in sorted(DUPLICATE_COLUMN_BINDINGS.items()):
        print(f"    - {cls}.{db_col}: {', '.join(sorted(java_vars))}")


def main():
    debug_col = None
    if len(sys.argv) >= 3 and sys.argv[1] == "--debug-column":
        debug_col = sys.argv[2].upper()

    profile_map = parse_yaml_map(Path(YAML_DICTS_DIR))
    graph = parse_java_graph(JAVA_SRC_DIR)

    roots = [n for n, d in graph.items() if d["type"] in ("Entity", "MappedSuperclass")]
    roots.sort(key=lambda x: (graph[x]["parent"] or "", x))

    for name in roots:
        node = graph[name]
        inheritance = f" extends {node['parent']}" if node["parent"] else ""
        table_info = f" ──► Table: {node['table']}" if node["table"] else ""

        print(f"[{node['type']}] {name}{inheritance}{table_info}")
        print_tree(graph, profile_map, name, get_class_profile(name), indent=" ", debug_col=debug_col)
        print(f"\n{'-' * 80}\n")

    _print_unmatched_columns(profile_map)
    _print_duplicate_bindings()


if __name__ == "__main__":
    main()
