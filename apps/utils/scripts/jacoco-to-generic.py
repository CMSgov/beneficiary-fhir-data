#!/usr/bin/env python3

import argparse
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Dict, Optional, Tuple


def java_map(source_root: Path) -> Dict[Tuple[str, str], str]:
    """
    Return map of (vm_class_name, sourcefilename) -> normalized_relative_path where:
      - `vm_class_name` like 'gov/cms/bfd/migrator/app/AppConfiguration'
      - `source_file_name` like 'AppConfiguration.java'
      - `normalized_relative_path` is a POSIX-style, relative path like
          'bfd-db-migrator/src/main/java/gov/cms/bfd/migrator/app/AppConfiguration.java'
    """
    index: Dict[Tuple[str, str], str] = {}

    for java_file in source_root.rglob("**/main/**/*.java"):
        relative_path = java_file.relative_to(source_root)

        # bfd-db-migrator/src/main/java/gov/cms/bfd/migrator/app/AppConfiguration ->  gov/cms/bfd/migrator/app/AppConfiguration
        vm_class_name = relative_path.with_suffix("").as_posix().split("main/java/")[1]

        source_file_name = relative_path.name

        key = (vm_class_name, source_file_name)

        index.setdefault(key, relative_path.as_posix())

    return index


def path_from_map(
    index: Dict[Tuple[str, str], str],
    class_vm_name: str,
    source_file_name: str,
    pkg_name: str,
) -> Optional[str]:
    """
    Attempt a best match for a given class/sourcefile name in the computed java index

    Priority:
      1. Exact VM class name + sourcefilename
      2. Package + sourcefilename (constructed VM name)
    """
    if not source_file_name:
        return None

    # Exact match
    key_exact = (class_vm_name, source_file_name)

    if key_exact in index:
        return index[key_exact]
    else:
        # Constructed VM name from package + sourcefilename
        simple_name = source_file_name.replace(".java", "")
        vm_from_pkg = f"{pkg_name}/{simple_name}"
        key_pkg = (vm_from_pkg, source_file_name)

        if key_pkg in index:
            return index[key_pkg]
        else:
            return None


def build_output_tree(
    jacoco_root: ET.Element, version: int, source_root: Path
) -> ET.Element:
    coverage_root = ET.Element("coverage")
    coverage_root.set("version", str(version))
    java_index = java_map(source_root)

    for pkg in jacoco_root.findall(".//package"):
        pkg_name = (pkg.get("name") or "").strip()  # e.g. 'gov/cms/bfd/migrator/app'

        for cls in pkg.findall("./class"):
            class_vm_name = (
                cls.get("name") or ""
            ).strip()  # e.g. 'gov/cms/bfd/migrator/app/AppConfiguration'
            source_file_name = (
                cls.get("sourcefilename") or ""
            ).strip()  # 'AppConfiguration.java'

            # Try to resolve the class to a real path
            path = path_from_map(java_index, class_vm_name, source_file_name, pkg_name)

            if path is None:
                parts = [p for p in [pkg_name, source_file_name] if p]
                path = "/".join(parts) if parts else source_file_name or "UNKNOWN.java"

            file_element = ET.SubElement(coverage_root, "file")
            file_element.set("path", path)

            # Collect line coverage from the corresponding <sourcefile> node
            # (JaCoCo ties lines to <sourcefile>, not <class>, so we need to find that node.)
            source_files = pkg.findall("./sourcefile")
            for sfile in source_files:
                if (sfile.get("name") or "").strip() != source_file_name:
                    continue

                for ln in sfile.findall("./line"):
                    nr = int(ln.get("nr", "0") or 0)
                    if nr == 0:
                        continue

                    # From JaCoCo Format
                    # https://github.com/jacoco/jacoco/blob/56ee20ac243c01059d8d69783e1e94f21071e20c/org.jacoco.report/src/org/jacoco/report/xml/report.dtd#L67-L76
                    ci = int(ln.get("ci", "0") or 0)  # covered instruction
                    mb = int(ln.get("mb", "0") or 0)  # missed branches
                    cb = int(ln.get("cb", "0") or 0)  # covered branches

                    # To SonarQube Generic Format
                    # https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/test-coverage/generic-test-data
                    covered_bool = ci > 0
                    branches_to_cover = mb + cb
                    covered_branches = cb

                    line_element = ET.SubElement(file_element, "lineToCover")
                    line_element.set("lineNumber", str(nr))
                    line_element.set("covered", "true" if covered_bool else "false")
                    line_element.set("branchesToCover", str(branches_to_cover))
                    line_element.set("coveredBranches", str(covered_branches))

    return coverage_root


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--in", dest="inp", required=True, help="JaCoCo XML file")
    ap.add_argument(
        "--out",
        dest="out",
        required=True,
        help="Output Generic Sonarqube Coverage XML file",
    )
    ap.add_argument(
        "--src-root",
        dest="src_root",
        required=True,
        help="Root directory Java sources to scan",
    )
    ap.add_argument("--version", type=int, default=1)
    args = ap.parse_args()

    try:
        jacoco_root = ET.parse(args.inp).getroot()
    except Exception as exc:
        print(f"ERROR: Failed to parse input XML: {exc}", file=sys.stderr)
        sys.exit(2)

    coverage_root = build_output_tree(jacoco_root, args.version, Path(args.src_root))
    coverage_tree = ET.ElementTree(coverage_root)

    try:
        coverage_tree.write(args.out, encoding="utf-8", xml_declaration=True)
    except Exception as exc:
        print(f"ERROR: Failed to write output XML: {exc}", file=sys.stderr)


if __name__ == "__main__":
    main()
