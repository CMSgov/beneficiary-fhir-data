# /// script
# requires-python = ">=3.14"
# dependencies = [
#     "pyyaml>=6.0.3",
# ]
# ///

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
COLUMN_RE = re.compile(
    r'(?s)@Column\(.*?name\s*=\s*"([^"]+)".*?\)'
    r'(?!(?:(?!;).)*?@Embedded)'  # bail if an @Embedded annotation sits between here and the field
    r'(?:(?!;).)*?(?:private|protected|public)?\s+([\w<>(),.? ]+)\s+(\w+);'
)
EMBEDDED_RE = re.compile(
    r"(?s)@Embedded(?:(?!;).)*?(?:private|protected|public)?\s+([\w<>(),.? ]+)\s+(\w+);"
)
ATTRIBUTE_OVERRIDE_RE = re.compile(
    r'@AttributeOverride\(\s*(?:'
    r'name\s*=\s*"(?P<name1>[^"]+)"\s*,\s*column\s*=\s*@Column\(\s*name\s*=\s*"(?P<col1>[^"]+)"\s*\)'
    r'|'
    r'column\s*=\s*@Column\(\s*name\s*=\s*"(?P<col2>[^"]+)"\s*\)\s*,\s*name\s*=\s*"(?P<name2>[^"]+)"'
    r')\s*\)'
)

JAVA_ENTITY_RE = re.compile(
    r"Claim(?P<domain>Institutional|Professional|Rx)?(?P<profile>Cms|Basis|Regular)(?P<source>Nch|SharedSystems|Rx)?",
    re.IGNORECASE,
)

DEFAULT_PROFILES = ["Basis", "Regular", "CMS (Default)"]

# Populated during print_tree() with every db_col that had no entry in the
# YAML dictionaries at all. These silently default to "valid for all
# profiles" today -- this set lets main() report them separately so a
# missing/typo'd dictionary entry isn't confused with a genuinely
# unrestricted column.
UNMATCHED_COLUMNS: set[str] = set()


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
                        mappings[source_col.upper()] = [
                            p.split()[0].upper() for p in profiles
                        ]
            except yaml.YAMLError as e:
                print(f"[WARN] Failed to parse {file_path}: {e}")
                continue
    return mappings


def parse_java_graph(src_dir: Path) -> dict:
    graph = {}
    if not src_dir.exists():
        return graph
    for file_path in src_dir.rglob("*.java"):
        content = file_path.read_text(encoding="utf-8")
        if not (class_match := CLASS_RE.search(content)):
            continue

        name = class_match.group("name")
        table_match = TABLE_RE.search(content)

        override_matches = list(ATTRIBUTE_OVERRIDE_RE.finditer(content))
        # Span of each @AttributeOverride(...) match, so we can recognize
        # (and skip) any @Column(...) that COLUMN_RE finds nested inside
        # one of these -- that @Column belongs to the override and is
        # already captured below via its 'name' attribute, not via a
        # field declaration.
        override_spans = [(m.start(), m.end()) for m in override_matches]

        def _inside_override(pos: int) -> bool:
            return any(start <= pos < end for start, end in override_spans)

        plain_columns = [
            {"db_col": m.group(1), "java_var": m.group(3)}
            for m in COLUMN_RE.finditer(content)
            if not _inside_override(m.start())
        ]
        override_columns = [
            {
                "db_col": m.group("col1") or m.group("col2"),
                "java_var": m.group("name1") or m.group("name2"),
            }
            for m in override_matches
        ]

        graph[name] = {
            "type": class_match.group("type").replace("@", ""),
            "parent": class_match.group("parent"),
            "table": table_match.group(1) if table_match else None,
            "columns": plain_columns + override_columns,
            "embeddeds": [
                {
                    "type": re.sub(
                        r"^(?:private|protected|public)\s+", "", e[0]
                    ).strip(),
                    "java_var": e[1],
                }
                for e in EMBEDDED_RE.findall(content)
            ],
        }
    return graph


def get_class_profile(class_name: str) -> str | None:
    """Extracts base profile from class naming conventions."""
    if match := JAVA_ENTITY_RE.search(class_name):
        return match.group("profile").upper()
    return None


def resolve_inherited(graph: dict, class_name: str) -> tuple[list, list]:
    """Walk the parent chain and merge columns/embeddeds from all ancestors.

    Without this, a subclass like ClaimInstitutionalCmsNch is only checked
    against its own directly-declared columns, and never against fields
    declared on ClaimInstitutionalBase / ClaimBase -- which is where most
    real profile mismatches live.

    Each column is tagged with "own" (True only for columns declared
    directly on `class_name` itself) so callers can avoid flagging
    inherited columns as move candidates -- they're already declared at
    whatever level a prior pull-up put them at.
    """
    columns, embeddeds = [], []
    seen = set()
    node_name = class_name
    is_self = True
    while node_name and node_name in graph and node_name not in seen:
        seen.add(node_name)
        node = graph[node_name]
        columns.extend({**c, "own": is_self} for c in node["columns"])
        embeddeds.extend(node["embeddeds"])
        node_name = node["parent"]
        is_self = False
    return columns, embeddeds


def print_tree(
        graph: dict,
        profile_map: dict,
        class_name: str,
        class_profile: str | None,
        indent: str = "",
        visited: set = None,
        check_flags: bool = True,
        is_claim_class: bool | None = None,
        debug_col: str | None = None,
):
    visited = visited or set()
    if class_name in visited or class_name not in graph:
        return
    visited.add(class_name)

    columns, embeddeds = resolve_inherited(graph, class_name)
    # NOTE: this used to be gated on class_name.startswith("Claim"), which
    # silently exempted every non-Claim family (Beneficiary*, Contract*,
    # ...) from push-down detection even though the same profile-scoping
    # logic applies to them. The check is now generic; is_claim_class is
    # kept only as the name propagated through recursion (see the note
    # on the print_tree signature) so the embedded-traversal fix above
    # continues to carry a stable flag down the tree.
    if is_claim_class is None:
        is_claim_class = True

    for col in columns:
        db_col = col["db_col"]
        if db_col.upper() not in profile_map:
            UNMATCHED_COLUMNS.add(db_col.upper())
        allowed_profiles = profile_map.get(db_col.upper(), ["BASIS", "REGULAR", "CMS"])

        if debug_col and db_col.upper() == debug_col:
            print(
                f"[DEBUG] {db_col} found on class={class_name!r} "
                f"class_profile={class_profile!r} is_claim_class={is_claim_class!r} "
                f"check_flags={check_flags!r} col_own={col['own']!r} "
                f"allowed_profiles={allowed_profiles!r} "
                f"in_profile_map={db_col.upper() in profile_map!r}",
                file=sys.stderr,
            )

        action_flag = ""
        if check_flags and col["own"]:
            if class_profile and class_profile not in allowed_profiles:
                action_flag = f" ⚠️  [WARNING] Column not valid for profile '{class_profile}'! Push Down/Move."
            elif class_profile and set(allowed_profiles) >= {"BASIS", "REGULAR", "CMS"}:
                action_flag = " ▲ [PULL UP candidate]"
            elif (
                    not class_profile
                    and is_claim_class
                    and set(allowed_profiles) < {"BASIS", "REGULAR", "CMS"}
            ):
                action_flag = (
                    f" ▼ [PUSH DOWN candidate] Only valid for {allowed_profiles}, "
                    "not all profiles -- move to profile-specific subclass(es)."
                )

        print(
            f"{indent}├── [Column] {db_col} ({col['java_var']}) "
            f"[profiles: {', '.join(allowed_profiles)}]{action_flag}"
        )

    for emb in embeddeds:
        print(f"{indent}└── [Embedded] {emb['java_var']} ──► Type: {emb['type']}")
        print_tree(
            graph,
            profile_map,
            emb["type"],
            class_profile,
            indent + "    │",
            visited.copy(),
            check_flags=check_flags,
            is_claim_class=is_claim_class,
            debug_col=debug_col,
            )


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
        print_tree(
            graph, profile_map, name, get_class_profile(name), indent=" ",
            debug_col=debug_col,
        )
        print(f"\n{'-' * 80}\n")

    if UNMATCHED_COLUMNS:
        print(f"[WARN] {len(UNMATCHED_COLUMNS)} column(s) had no YAML dictionary "
              "entry and defaulted to 'all profiles valid' -- verify these aren't "
              "typos or missing dictionary files:")
        for col in sorted(UNMATCHED_COLUMNS):
            print(f"    - {col}")


if __name__ == "__main__":
    main()
