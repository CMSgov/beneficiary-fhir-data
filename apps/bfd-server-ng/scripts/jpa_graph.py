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

# DISCLAIMER : This was generated with the assistance of Claude AI

SCRIPT_DIR = Path(__file__).resolve().parent.parent

JAVA_SRC_DIR = SCRIPT_DIR / "src/main/java/gov/cms/bfd/server/ng"
YAML_DICTS_DIR = SCRIPT_DIR / "../bfd-model-idr/dictionary-support-files"

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
    r"Claim\w*?(?P<profile>Cms|Basis|Regular)\w*",
    re.IGNORECASE,
)

DEFAULT_PROFILES = ["Basis", "Regular", "CMS (Default)"]
ALL_PROFILES_ORDERED = ["BASIS", "REGULAR", "CMS"]
ALL_PROFILES_SET = set(ALL_PROFILES_ORDERED)

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

FORMAT_SPACING = "    │"


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
    """Profile implied by naming convention, e.g. ClaimProfessionalCmsBase -> CMS.
    Returns None if the name doesn't encode a profile at all."""
    if match := JAVA_ENTITY_RE.search(class_name):
        return match.group("profile").upper()
    return None


def entity_profile(class_name: str) -> tuple[str, bool]:
    """(profile, was_inferred). Most entities outside the claim family
    aren't profile-scoped at all, so when the name doesn't tell us,
    default to CMS -- it's the superset profile, and flagging the
    default keeps this honest rather than silently guessing."""
    detected = get_class_profile(class_name)
    if detected:
        return detected, False
    return "CMS", True


def entity_exposed_columns(graph: dict, class_name: str, visited: set = None) -> set[str]:
    """Every db_col reachable from this entity through inheritance and
    nested @Embedded types, flattened into one set. Used only for the
    profile-completeness check, which doesn't care where a column lives,
    just whether the entity exposes it somewhere."""
    visited = visited if visited is not None else set()
    if class_name in visited or class_name not in graph:
        return set()
    visited.add(class_name)

    node = graph[class_name]
    cols = {col["db_col"].upper() for col in node["columns"]}
    for embed in node["embeddeds"]:
        cols |= entity_exposed_columns(graph, embed["type"], visited.copy())
    if node["parent"]:
        cols |= entity_exposed_columns(graph, node["parent"], visited.copy())
    return cols


def _debug_field(col: dict, declaring_class: str, profile: str, allowed_profiles: list, in_dict: bool) -> None:
    print(
        f"[DEBUG] {col['db_col']} declared on {declaring_class!r} entity_profile={profile!r} "
        f"allowed_profiles={allowed_profiles!r} in_profile_map={in_dict!r}",
        file=sys.stderr,
    )


def print_entity_tree(
        graph: dict,
        profile_map: dict,
        class_name: str,
        profile: str,
        indent: str = "",
        visited: set = None,
        debug_col: str | None = None,
) -> None:
    """Print this class's own columns and embeds, then recurse into its
    parent as its own 'extends' branch one level deeper -- so the
    inheritance chain shows up in the tree the same way embeds do,
    instead of being pre-merged and hidden. `profile` stays fixed to the
    owning entity throughout, since that's what actually gets exposed."""
    visited = visited if visited is not None else set()
    if class_name in visited or class_name not in graph:
        return
    visited.add(class_name)

    node = graph[class_name]

    for col in node["columns"]:
        db_col = col["db_col"].upper()
        in_dict = db_col in profile_map
        if not in_dict:
            UNMATCHED_COLUMNS.add(db_col)
        allowed_profiles = profile_map.get(db_col, ALL_PROFILES_ORDERED)

        if debug_col and db_col == debug_col:
            _debug_field(col, class_name, profile, allowed_profiles, in_dict)

        flag = ""
        if profile not in allowed_profiles:
            flag = f"  ❌ MISMATCH: entity is {profile}, column only valid for {allowed_profiles}"

        print(
            f"{indent}├── [Column] {col['db_col']} ({col['java_var']}) "
            f"[profiles: {', '.join(allowed_profiles)}]{flag}"
        )

    for embed in node["embeddeds"]:
        print(f"{indent}└── [Embedded] {embed['java_var']} ──► Type: {embed['type']}")
        print_entity_tree(
            graph, profile_map, embed["type"], profile, indent + FORMAT_SPACING, visited.copy(), debug_col
        )

    parent = node["parent"]
    if parent and parent in graph:
        print(f"{indent}└── extends {parent}")
        print_entity_tree(graph, profile_map, parent, profile, indent + FORMAT_SPACING, visited.copy(), debug_col)


def print_entity(graph: dict, profile_map: dict, entity_name: str, debug_col: str | None = None) -> tuple[str, list]:
    node = graph[entity_name]
    profile, inferred = entity_profile(entity_name)

    table_info = f" ──► Table: {node['table']}" if node["table"] else ""
    profile_label = f"{profile} (default, no profile in name)" if inferred else profile
    print(f"[Entity] {entity_name}{table_info}  [profile: {profile_label}]")

    print_entity_tree(graph, profile_map, entity_name, profile, indent=" ", debug_col=debug_col)
    print()

    return profile, entity_exposed_columns(graph, entity_name)


def _collect_reachable_profiles(graph: dict) -> dict[str, set]:
    """For every class, every entity-level profile it's reachable under
    anywhere in the codebase -- walking both inheritance and @Embedded
    chains from every concrete Entity (the only real usage sites; a
    MappedSuperclass/Embeddable being examined on its own in this review
    isn't a usage context). A type reached by entities of more than one
    distinct profile is genuinely reused across contexts, e.g.
    ClaimPaymentComponent composed both directly by a Basis entity and
    via ClaimPaymentComponentAmount by a Cms entity -- flagging its
    columns against just one of those contexts would be misleading."""
    reachable: dict[str, set] = {}

    def walk(class_name: str, profile, visited: set) -> None:
        if class_name in visited or class_name not in graph:
            return
        visited.add(class_name)
        reachable.setdefault(class_name, set()).add(profile)
        node = graph[class_name]
        for embed in node["embeddeds"]:
            walk(embed["type"], profile, visited.copy())
        if node["parent"]:
            walk(node["parent"], profile, visited.copy())

    for name, node in graph.items():
        if node["type"] != "Entity":
            continue
        profile, _ = entity_profile(name)
        walk(name, profile, set())

    return reachable


def _shared_class_flag(class_profile: str | None, allowed_profiles: list, ambiguous: bool = False) -> str:
    if ambiguous:
        return ""
    if class_profile and class_profile not in allowed_profiles:
        return f"  ⚠️  WARNING: not valid for this class's own profile '{class_profile}'!"
    if class_profile and set(allowed_profiles) >= ALL_PROFILES_SET:
        return "  ▲ PULL UP candidate: valid for all profiles, could move to a shared base"
    if not class_profile and set(allowed_profiles) < ALL_PROFILES_SET:
        return f"  ▼ PUSH DOWN candidate: only valid for {allowed_profiles}, move to profile-specific subclass(es)"
    return ""


def _print_shared_fields(
        graph: dict,
        profile_map: dict,
        class_name: str,
        governing_profile: str | None,
        indent: str,
        expand_embeds: bool,
        reachable: dict,
        visited: set = None,
) -> None:
    visited = visited if visited is not None else set()
    if class_name in visited or class_name not in graph:
        return
    visited.add(class_name)

    node = graph[class_name]
    ambiguous = len(reachable.get(class_name, set())) > 1

    for col in node["columns"]:
        db_col = col["db_col"].upper()
        if db_col not in profile_map:
            UNMATCHED_COLUMNS.add(db_col)
        allowed_profiles = profile_map.get(db_col, ALL_PROFILES_ORDERED)
        flag = _shared_class_flag(governing_profile, allowed_profiles, ambiguous)
        print(
            f"{indent}├── [Column] {col['db_col']} ({col['java_var']}) "
            f"[profiles: {', '.join(allowed_profiles)}]{flag}"
        )

    for embed in node["embeddeds"]:
        print(f"{indent}└── [Embedded] {embed['java_var']} ──► Type: {embed['type']}")
        if expand_embeds:
            _print_shared_fields(
                graph, profile_map, embed["type"], governing_profile, indent + FORMAT_SPACING,
                expand_embeds, reachable, visited.copy(),
                                                                      )


def print_shared_class(
        graph: dict, profile_map: dict, class_name: str, expand_embeds: bool = False, reachable: dict = None
) -> None:
    """Print a MappedSuperclass/Embeddable's own declared columns (not
    resolved through inheritance -- the parent gets its own section) with
    a push-down/pull-up flag: a shared class with a profile-restricted
    column should push it down; a profile-named class with an all-profiles
    column could pull it up. Embedded types print as a pointer by default
    (they get their own section further down); pass expand_embeds to
    recurse into them inline instead -- every nested column is still
    checked against THIS class's own profile, not whatever the embedded
    type's own name (or lack of one) implies in isolation. Any class with
    "base" in its name always expands inline regardless of the flag,
    since that's where push-down/pull-up review actually concentrates.
    Flags are suppressed for any class demonstrably reused across more
    than one governing profile elsewhere in the codebase (see reachable)."""
    reachable = reachable or {}
    node = graph[class_name]
    class_profile = get_class_profile(class_name)
    ambiguous = len(reachable.get(class_name, set())) > 1

    if ambiguous:
        contexts = ", ".join(sorted((p or "no profile") for p in reachable[class_name]))
        profile_label = f"reused across multiple contexts ({contexts}) -- flags suppressed"
    else:
        profile_label = class_profile or "shared, no profile in name"
    inheritance = f" extends {node['parent']}" if node["parent"] else ""

    should_expand = expand_embeds or "base" in class_name.lower()

    print(f"[{node['type']}] {class_name}{inheritance}  [profile: {profile_label}]")
    _print_shared_fields(
        graph, profile_map, class_name, class_profile, indent="    ", expand_embeds=should_expand, reachable=reachable
    )
    print()


def print_shared_classes(graph: dict, profile_map: dict, expand_embeds: bool = False, group_embeds: bool = False) -> None:
    reachable = _collect_reachable_profiles(graph)
    shared = sorted(
        (n for n, d in graph.items() if d["type"] in ("MappedSuperclass", "Embeddable")),
        key=lambda n: (graph[n]["type"], n),
    )

    if not group_embeds:
        for name in shared:
            print_shared_class(graph, profile_map, name, expand_embeds, reachable)
        return

    # Print each embed type right after the class that references it,
    # instead of strict alphabetical order -- makes it easier to trace an
    # embed's contents without scrolling to find its section, without
    # duplicating anything the way expand_embeds does.
    printed: set[str] = set()

    def visit(name: str) -> None:
        if name in printed or name not in graph or graph[name]["type"] not in ("MappedSuperclass", "Embeddable"):
            return
        printed.add(name)
        print_shared_class(graph, profile_map, name, expand_embeds, reachable)
        for embed in graph[name]["embeddeds"]:
            visit(embed["type"])

    for name in shared:
        visit(name)


def check_profile_completeness(profile_map: dict, entity_index: dict) -> dict:
    """For every db_col the dictionary says a profile can see, confirm at
    least one concrete entity for that profile actually exposes it. Flags
    dictionary entries with no matching entity -- either the entity model
    is missing the field, or the dictionary is over-scoped."""
    exposed_by_profile: dict[str, set[str]] = {p: set() for p in ALL_PROFILES_ORDERED}
    for profile, cols in entity_index.values():
        exposed_by_profile.setdefault(profile, set()).update(cols)

    gaps: dict[str, list[str]] = {}
    for db_col, profiles in profile_map.items():
        for profile in profiles:
            if db_col not in exposed_by_profile.get(profile, set()):
                gaps.setdefault(profile, []).append(db_col)
    return gaps


def _print_unmatched_columns(profile_map: dict) -> None:
    if not UNMATCHED_COLUMNS:
        return
    print(
        f"[WARN] {len(UNMATCHED_COLUMNS)} column(s) had no YAML dictionary entry "
        "and defaulted to 'all profiles valid' -- check for typos or missing "
        "dictionary files:"
    )
    for col in sorted(UNMATCHED_COLUMNS):
        print(f"    - {col}")


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


def _print_completeness_gaps(gaps: dict) -> None:
    if not gaps:
        return
    print(
        "\n[WARN] The dictionary says these profiles can see these columns, but "
        "no concrete entity for that profile actually exposes them:"
    )
    for profile in sorted(gaps):
        print(f"  {profile}:")
        for db_col in sorted(gaps[profile]):
            print(f"    - {db_col}")


def find_unresolved_embeds(graph: dict) -> dict[str, set[str]]:
    """Every type referenced via @Embedded that never got parsed as its
    own @Entity/@MappedSuperclass/@Embeddable class. Almost always means
    that class is missing the @Embeddable annotation and is silently
    riding on a JPA provider's implicit-embeddable fallback -- or it's a
    genuinely missing/renamed file. Scoped to @Embedded only: extends/
    implements targets are frequently plain interfaces (Comparable,
    ClaimLineBase) that are correctly unannotated, so checking those
    would be mostly noise."""
    unresolved: dict[str, set[str]] = {}
    for class_name, node in graph.items():
        for embed in node["embeddeds"]:
            embed_type = embed["type"]
            if embed_type not in graph:
                unresolved.setdefault(embed_type, set()).add(class_name)
    return unresolved


def _print_unresolved_embeds(unresolved: dict) -> None:
    if not unresolved:
        return
    print(
        f"\n[WARN] {len(unresolved)} @Embedded type(s) referenced but never parsed as "
        "their own class -- likely missing @Embeddable (relying on an implicit JPA "
        "fallback) or a missing/renamed file:"
    )
    for embed_type in sorted(unresolved):
        referenced_by = ", ".join(sorted(unresolved[embed_type]))
        print(f"    - {embed_type} (referenced by: {referenced_by})")


def main():
    args = sys.argv[1:]
    debug_col = None
    if "--debug-column" in args:
        idx = args.index("--debug-column")
        if idx + 1 < len(args):
            debug_col = args[idx + 1].upper()
    expand_embeds = "--expand-embeds" in args
    group_embeds = "--group-embeds" in args

    profile_map = parse_yaml_map(YAML_DICTS_DIR)
    graph = parse_java_graph(JAVA_SRC_DIR)

    entity_names = sorted(n for n, d in graph.items() if d["type"] == "Entity")

    entity_index = {}
    for name in entity_names:
        profile, fields = print_entity(graph, profile_map, name, debug_col)
        entity_index[name] = (profile, fields)

    print("-" * 80)
    print("MappedSuperclass / Embeddable review (push-down / pull-up candidates)")
    print("-" * 80)
    print()
    print_shared_classes(graph, profile_map, expand_embeds=expand_embeds, group_embeds=group_embeds)

    _print_unmatched_columns(profile_map)
    _print_duplicate_bindings()
    _print_completeness_gaps(check_profile_completeness(profile_map, entity_index))
    _print_unresolved_embeds(find_unresolved_embeds(graph))


if __name__ == "__main__":
    main()