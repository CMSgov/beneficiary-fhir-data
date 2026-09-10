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
# A @OneToMany association to another @Entity (e.g. claim line items) is a
# different JPA relationship than @Embedded, but for graph-walking
# purposes it's the same idea: something else whose fields should show up
# when tracing this class's tree. Same defensive skip-zone as EMBEDDED_RE.
ONE_TO_MANY_RE = re.compile(
    r"(?s)@OneToMany"
    r"(?:(?!;|\{|@Column|@AttributeOverride|@Embedded).)*?"
    r"(?:private|protected|public)?\s+\w+<\s*(\w+)\s*>\s+(\w+);"
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
    """Blank out each @AttributeOverride(...) span so COLUMN_RE/EMBEDDED_RE
    can't see (or backtrack into) the @Column nested inside it."""
    for match in override_matches:
        start, end = match.span()
        content = content[:start] + " " * (end - start) + content[end:]
    return content


def _override_dict(match: re.Match) -> dict:
    return {
        "db_col": match.group("col1") or match.group("col2"),
        "java_var": match.group("name1") or match.group("name2"),
    }


def _extract_columns_and_embeds(content: str) -> tuple[list, list]:
    """Splits a class's annotations into (plain columns, embeds). An
    @AttributeOverride immediately preceding an @Embedded field is
    attached to that embed's own "overrides" list instead of becoming a
    disconnected top-level column -- it's overriding one of the embedded
    type's own properties, so it belongs nested under that embed in the
    display. An override with no adjacent local @Embedded (e.g.
    overriding a field inherited from a MappedSuperclass, with no local
    re-declaration) falls back to a plain top-level column."""
    override_matches = list(ATTRIBUTE_OVERRIDE_RE.finditer(content))
    clean_content = _hide_attribute_overrides(content, override_matches)

    plain_columns = [
        {"db_col": match.group(1), "java_var": match.group(3)}
        for match in COLUMN_RE.finditer(clean_content)
    ]

    embeds = []
    claimed: set[int] = set()
    for m in EMBEDDED_RE.finditer(clean_content):
        clean_type = re.sub(r"^(?:private|protected|public)\s+", "", m.group(1)).strip()
        own_overrides = []
        for i, override_match in enumerate(override_matches):
            if i in claimed:
                continue
            if override_match.end() <= m.start() and ";" not in content[override_match.end() : m.start()]:
                own_overrides.append(_override_dict(override_match))
                claimed.add(i)
        embeds.append({"type": clean_type, "java_var": m.group(2), "overrides": own_overrides})

    for target_type, java_var in ONE_TO_MANY_RE.findall(clean_content):
        embeds.append({"type": target_type, "java_var": java_var, "overrides": []})

    leftover_columns = [
        _override_dict(override_match) for i, override_match in enumerate(override_matches) if i not in claimed
    ]

    return plain_columns + leftover_columns, embeds


def _record_duplicate_bindings(class_name: str, columns: list, embeds: list) -> None:
    all_column_like = columns + [override for embed in embeds for override in embed["overrides"]]
    java_vars_by_column: dict[str, set[str]] = {}
    for col in all_column_like:
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
        columns, embeds = _extract_columns_and_embeds(content)
        _record_duplicate_bindings(name, columns, embeds)

        graph[name] = {
            "type": class_match.group("type").replace("@", ""),
            "parent": class_match.group("parent"),
            "table": table_match.group(1) if table_match else None,
            "columns": columns,
            "embeddeds": embeds,
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
    just whether the entity exposes it somewhere. Includes each embed's
    own attached overrides -- those are real columns too, just declared
    at the point of embedding rather than inside the embedded type."""
    visited = visited if visited is not None else set()
    if class_name in visited or class_name not in graph:
        return set()
    visited.add(class_name)

    node = graph[class_name]
    cols = {col["db_col"].upper() for col in node["columns"]}
    for embed in node["embeddeds"]:
        cols |= {o["db_col"].upper() for o in embed["overrides"]}
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


def _print_entity_column(profile_map: dict, col: dict, declaring_class: str, profile: str, indent: str, debug_col: str | None) -> None:
    db_col = col["db_col"].upper()
    in_dict = db_col in profile_map
    if not in_dict:
        UNMATCHED_COLUMNS.add(db_col)
    allowed_profiles = profile_map.get(db_col, ALL_PROFILES_ORDERED)

    if debug_col and db_col == debug_col:
        _debug_field(col, declaring_class, profile, allowed_profiles, in_dict)

    flag = ""
    if profile not in allowed_profiles:
        flag = f"  ❌ MISMATCH: entity is {profile}, column only valid for {allowed_profiles}"

    print(
        f"{indent}├── [Column] {col['db_col']} ({col['java_var']}) "
        f"[profiles: {', '.join(allowed_profiles)}]{flag}"
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
    owning entity throughout, since that's what actually gets exposed.
    An embed's own @AttributeOverride columns print directly under its
    [Embedded] pointer, since they're overriding that embedded type's
    own property, not something declared on this class."""
    visited = visited if visited is not None else set()
    if class_name in visited or class_name not in graph:
        return
    visited.add(class_name)

    node = graph[class_name]

    for col in node["columns"]:
        _print_entity_column(profile_map, col, class_name, profile, indent, debug_col)

    for embed in node["embeddeds"]:
        print(f"{indent}└── [Embedded] {embed['java_var']} ──► Type: {embed['type']}")
        for override_col in embed["overrides"]:
            _print_entity_column(profile_map, override_col, class_name, profile, indent + FORMAT_SPACING, debug_col)
        print_entity_tree(
            graph, profile_map, embed["type"], profile, indent + FORMAT_SPACING, visited.copy(), debug_col
        )

    parent = node["parent"]
    if parent:
        if parent in graph:
            print(f"{indent}└── extends {parent}")
            print_entity_tree(graph, profile_map, parent, profile, indent + FORMAT_SPACING, visited.copy(), debug_col)
        else:
            print(f"{indent}└── extends {parent}  ⚠️  not resolved -- interface, or a missing/unparsed class")


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


def _print_shared_column(profile_map: dict, col: dict, governing_profile: str | None, indent: str, ambiguous: bool) -> None:
    db_col = col["db_col"].upper()
    if db_col not in profile_map:
        UNMATCHED_COLUMNS.add(db_col)
    allowed_profiles = profile_map.get(db_col, ALL_PROFILES_ORDERED)
    flag = _shared_class_flag(governing_profile, allowed_profiles, ambiguous)
    print(
        f"{indent}├── [Column] {col['db_col']} ({col['java_var']}) "
        f"[profiles: {', '.join(allowed_profiles)}]{flag}"
    )


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
        _print_shared_column(profile_map, col, governing_profile, indent, ambiguous)

    for embed in node["embeddeds"]:
        print(f"{indent}└── [Embedded] {embed['java_var']} ──► Type: {embed['type']}")
        for override_col in embed["overrides"]:
            _print_shared_column(profile_map, override_col, governing_profile, indent + FORMAT_SPACING, ambiguous)
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
    An embed's own @AttributeOverride columns always print directly
    under its pointer regardless of expand_embeds, since they're
    declared on THIS class, not the embedded type. Flags are suppressed
    for any class demonstrably reused across more than one governing
    profile elsewhere in the codebase (see reachable)."""
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


def _entity_path_key(class_name: str) -> str | None:
    """Strips the profile token out of an entity's name to get its
    domain+source 'path', e.g. ClaimInstitutionalCmsSharedSystems and
    ClaimInstitutionalRegularSharedSystems both collapse to
    ClaimInstitutionalSharedSystems -- the same path, different profile.
    Returns None for entities with no profile in the name at all; they
    have no real sibling to pair against."""
    match = JAVA_ENTITY_RE.search(class_name)
    if not match:
        return None
    start, end = match.span("profile")
    return class_name[:start] + class_name[end:]


def _group_entities_by_path(graph: dict) -> dict[str, dict[str, str]]:
    """path -> {profile: entity_name}, built only from entities whose
    name actually encodes a profile (the CMS-default fallback doesn't
    apply here -- there's no real sibling to compare a defaulted entity
    against)."""
    groups: dict[str, dict[str, str]] = {}
    for name, node in graph.items():
        if node["type"] != "Entity":
            continue
        profile = get_class_profile(name)
        if not profile:
            continue
        path = _entity_path_key(name)
        if path is None:
            continue
        groups.setdefault(path, {})[profile] = name
    return groups


SOURCE_SUFFIXES = ("Nch", "SharedSystems")


def _entity_domain_and_source(class_name: str) -> tuple[str, str] | None:
    """Splits the trailing source suffix (Nch/SharedSystems) off an
    entity's already profile-stripped path, e.g.
    ClaimInstitutionalSharedSystems -> ('ClaimInstitutional',
    'SharedSystems'). Returns None if the path doesn't end in a
    recognized source suffix -- some domains (Rx) don't split by
    source at all and shouldn't be forced into this grouping."""
    path = _entity_path_key(class_name)
    if path is None:
        return None
    for suffix in SOURCE_SUFFIXES:
        if path.endswith(suffix):
            return path[: -len(suffix)], suffix
    return None


def _group_entities_by_domain_and_profile(graph: dict) -> dict[tuple[str, str], dict[str, str]]:
    """(domain, profile) -> {source: entity_name}, e.g.
    ('ClaimInstitutional', 'REGULAR') -> {'Nch': 'ClaimInstitutionalRegularNch',
    'SharedSystems': 'ClaimInstitutionalRegularSharedSystems'}."""
    groups: dict[tuple[str, str], dict[str, str]] = {}
    for name, node in graph.items():
        if node["type"] != "Entity":
            continue
        profile = get_class_profile(name)
        if not profile:
            continue
        split = _entity_domain_and_source(name)
        if split is None:
            continue
        domain, source = split
        groups.setdefault((domain, profile), {})[source] = name
    return groups


def check_source_split_consistency(graph: dict, entity_index: dict) -> list[dict]:
    """Within a single profile, a domain with both an Nch and a
    SharedSystems entity should split source-specific columns the same
    way CMS already does. If CMS keeps a column on only one of its two
    sources but another profile has that column on BOTH of its sources
    at once, the column is almost certainly sitting on a Base class
    shared between Nch and SharedSystems when it should be split per
    source instead, mirroring the structure CMS already has."""
    by_domain_profile = _group_entities_by_domain_and_profile(graph)

    cms_cols_by_domain_source: dict[str, dict[str, set]] = {}
    for (domain, profile), by_source in by_domain_profile.items():
        if profile == "CMS":
            cms_cols_by_domain_source[domain] = {
                source: entity_index.get(name, (None, set()))[1] for source, name in by_source.items()
            }

    findings = []
    for (domain, profile), by_source in by_domain_profile.items():
        if profile == "CMS":
            continue
        cms_sources = cms_cols_by_domain_source.get(domain)
        if not cms_sources or "Nch" not in cms_sources or "SharedSystems" not in cms_sources:
            continue
        if "Nch" not in by_source or "SharedSystems" not in by_source:
            continue

        nch_cols = entity_index.get(by_source["Nch"], (None, set()))[1]
        ss_cols = entity_index.get(by_source["SharedSystems"], (None, set()))[1]
        if not nch_cols or not ss_cols:
            continue

        shared_in_this_profile = nch_cols & ss_cols
        cms_nch_only = cms_sources["Nch"] - cms_sources["SharedSystems"]
        cms_ss_only = cms_sources["SharedSystems"] - cms_sources["Nch"]

        for db_col in sorted(shared_in_this_profile & cms_nch_only):
            findings.append(
                {
                    "domain": domain,
                    "profile": profile,
                    "db_col": db_col,
                    "cms_source": "Nch",
                    "nch_entity": by_source["Nch"],
                    "ss_entity": by_source["SharedSystems"],
                }
            )
        for db_col in sorted(shared_in_this_profile & cms_ss_only):
            findings.append(
                {
                    "domain": domain,
                    "profile": profile,
                    "db_col": db_col,
                    "cms_source": "SharedSystems",
                    "nch_entity": by_source["Nch"],
                    "ss_entity": by_source["SharedSystems"],
                }
            )

    return findings


def _print_source_split_findings(findings: list) -> None:
    if not findings:
        return
    print(
        "\n[WARN] These columns are source-specific on CMS (only Nch or only "
        "SharedSystems) but show up on BOTH sources for another profile -- "
        "likely pushed onto a Base shared between the two sources when it "
        "should be split per source instead, matching CMS's layout:"
    )
    for f in sorted(findings, key=lambda x: (x["domain"], x["profile"], x["db_col"])):
        print(
            f"    - {f['db_col']}: CMS keeps this {f['cms_source']}-only, but {f['profile']} "
            f"has it on both {f['nch_entity']} and {f['ss_entity']}"
        )


def check_cms_superset(graph: dict, profile_map: dict, entity_index: dict) -> tuple[list[dict], list[tuple[str, str]]]:
    """CMS is meant to be the fullest profile for each domain+source
    path: (1) every column a Basis/Regular sibling exposes should also
    exist on its CMS counterpart, and (2) every CMS column the
    dictionary tags for that sibling's profile should actually show up
    on that specific sibling -- not just somewhere else for that profile
    (check_profile_completeness already covers that more general case),
    but on the exact counterpart for this path.

    A sibling with zero exposed columns is treated as an unbuilt stub
    and skipped rather than compared -- otherwise a fully-built CMS
    entity produces one finding per column against a placeholder class
    that just hasn't been implemented yet, which is noise, not signal."""
    findings = []
    skipped_stubs: list[tuple[str, str]] = []
    for path, by_profile in _group_entities_by_path(graph).items():
        cms_name = by_profile.get("CMS")
        if not cms_name:
            continue
        cms_cols = entity_index.get(cms_name, (None, set()))[1]
        if not cms_cols:
            continue

        for profile, sibling_name in by_profile.items():
            if profile == "CMS":
                continue
            sibling_cols = entity_index.get(sibling_name, (None, set()))[1]
            if not sibling_cols:
                skipped_stubs.append((sibling_name, profile))
                continue

            for db_col in sorted(sibling_cols - cms_cols):
                findings.append(
                    {
                        "kind": "missing_from_cms",
                        "path": path,
                        "profile": profile,
                        "cms_entity": cms_name,
                        "sibling_entity": sibling_name,
                        "db_col": db_col,
                    }
                )

            for db_col in sorted(cms_cols):
                allowed = profile_map.get(db_col, ALL_PROFILES_ORDERED)
                if profile in allowed and db_col not in sibling_cols:
                    findings.append(
                        {
                            "kind": "missing_from_sibling",
                            "path": path,
                            "profile": profile,
                            "cms_entity": cms_name,
                            "sibling_entity": sibling_name,
                            "db_col": db_col,
                        }
                    )

    return findings, skipped_stubs


def _print_cms_superset_findings(findings: list, skipped_stubs: list = None) -> None:
    skipped_stubs = skipped_stubs or []
    if skipped_stubs:
        print(
            f"\n[INFO] {len(skipped_stubs)} sibling(s) skipped in the CMS-superset check -- "
            "zero exposed columns, likely an unbuilt stub rather than a genuine gap:"
        )
        for name, profile in sorted(skipped_stubs):
            print(f"    - {name} ({profile})")

    if not findings:
        return

    missing_from_cms = [f for f in findings if f["kind"] == "missing_from_cms"]
    missing_from_sibling = [f for f in findings if f["kind"] == "missing_from_sibling"]

    if missing_from_cms:
        print(
            f"\n[ERROR] {len(missing_from_cms)} column(s) violate the CMS-superset "
            "invariant -- CMS was the original, only-served profile and every "
            "Basis/Regular value must trace back to a CMS class. These exist on a "
            "non-CMS sibling with no CMS counterpart, which isn't possible if the "
            "model is correct:"
        )
        for f in sorted(missing_from_cms, key=lambda x: (x["path"], x["profile"], x["db_col"])):
            print(
                f"    - {f['db_col']}: in {f['sibling_entity']} ({f['profile']}) "
                f"but not in {f['cms_entity']} (CMS)"
            )

    if missing_from_sibling:
        print(
            "\n[WARN] The dictionary tags these CMS columns for another profile, but the "
            "profile-specific sibling for this exact path doesn't expose them:"
        )
        for f in sorted(missing_from_sibling, key=lambda x: (x["path"], x["profile"], x["db_col"])):
            print(
                f"    - {f['db_col']}: in {f['cms_entity']} (CMS, tagged {f['profile']}) "
                f"but not in {f['sibling_entity']} ({f['profile']})"
            )


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


def find_unresolved_parents(graph: dict) -> dict[str, set[str]]:
    """Every 'extends' target that never got parsed as its own class.
    Unlike find_unresolved_embeds, this can't cleanly separate a
    harmless plain interface (Comparable, ClaimLineBase) from a
    genuinely missing/unparsed MappedSuperclass -- but an unresolved
    parent silently empties out the ENTIRE tree of anything that
    extends it (see print_entity_tree), which is a real problem
    whichever case it turns out to be, so it's reported either way."""
    unresolved: dict[str, set[str]] = {}
    for class_name, node in graph.items():
        parent = node["parent"]
        if parent and parent not in graph:
            unresolved.setdefault(parent, set()).add(class_name)
    return unresolved


def _print_unresolved_parents(unresolved: dict) -> None:
    if not unresolved:
        return
    print(
        f"\n[WARN] {len(unresolved)} 'extends' target(s) never parsed as their own class -- "
        "could be a plain interface (harmless), or a missing/unparsed/misnamed class, which "
        "silently empties out the tree of anything that extends it:"
    )
    for parent in sorted(unresolved):
        referenced_by = ", ".join(sorted(unresolved[parent]))
        print(f"    - {parent} (extended by: {referenced_by})")


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
    _print_unresolved_parents(find_unresolved_parents(graph))
    findings, skipped_stubs = check_cms_superset(graph, profile_map, entity_index)
    _print_cms_superset_findings(findings, skipped_stubs)
    _print_source_split_findings(check_source_split_consistency(graph, entity_index))


if __name__ == "__main__":
    main()