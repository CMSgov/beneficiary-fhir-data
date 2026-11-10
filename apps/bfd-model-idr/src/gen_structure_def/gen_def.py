import json
import subprocess
import tempfile
from datetime import UTC, datetime
from pathlib import Path
from typing import Any, cast

import requests
import yaml
from fhir.resources.fhirtypes import DateTimeType
from fhir.resources.R4B.codeableconcept import CodeableConcept
from fhir.resources.R4B.coding import Coding
from fhir.resources.R4B.elementdefinition import (
    ElementDefinition,
    ElementDefinitionSlicing,
    ElementDefinitionSlicingDiscriminator,
)
from fhir.resources.R4B.structuredefinition import (
    StructureDefinition,
    StructureDefinitionDifferential,
)

from install_fhir_validator import get_validator_jar_path
from install_fhir_validator import main as install_validator

from .fhir_def import FHIRDefinitions


class StructureDefinitionGenerator:
    def __init__(
        self,
        source_directory: str,
        output_directory: str,
        url: str,
        profile: str,
        source_yaml: str,
        title: str,
        version: str,
        name: str,
        description: str,
        base_definition: str,
        resource_type: str,
        output_file_name: str,
        applies_to: str,
        delete_tmp_files: bool = True,
    ):
        self.source_directory = source_directory
        self.output_directory = output_directory
        self.url = url
        self.profile = profile
        self.source_yaml = source_yaml
        self.title = title
        self.version = version
        self.name = name
        self.description = description
        self.base_definition = base_definition
        self.resource_type = resource_type
        self.output_file_name = output_file_name
        self.applies_to = applies_to
        self.delete_tmp_files = delete_tmp_files

    def run(self) -> None:
        """Generate StructureDefinition Resource."""
        print(f"Title: {self.title}")
        print(f"Source Directory: {self.source_directory}")
        print(f"Target Directory: {self.output_directory}")
        print(f"File: {self.output_file_name}")

        yaml_path = Path(f"{self.source_directory}/{self.source_yaml}")
        with yaml_path.open(mode="r", encoding="utf-8") as f:
            data = yaml.safe_load(f)

        data = [
            node
            for node in data
            if self.profile in node.get("profiles", [])
            and node.get("structureDef")
            and self.applies_to in node.get("appliesTo", [])
        ]

        output_dir = Path(self.output_directory)
        output_dir.mkdir(parents=True, exist_ok=True)
        with (
            tempfile.NamedTemporaryFile(
                mode="w", suffix=".json", delete=self.delete_tmp_files, dir=output_dir
            ) as sd_json_file,
            tempfile.NamedTemporaryFile(
                mode="w", suffix=".json", delete=self.delete_tmp_files, dir=output_dir
            ) as differential_file,
        ):
            sd_json_path = Path(sd_json_file.name)
            self.download_structure_definition(
                self.base_definition,
                sd_json_path,
            )

            source_structure_def = StructureDefinition.model_construct(
                **json.loads(sd_json_path.read_text(encoding="UTF-8"))
            )

            fhir_defs = FHIRDefinitions()

            elements: list[ElementDefinition] = []
            slicing_added = set()
            for node in data:
                struct_def_data = node.get("structureDef")
                el_id = struct_def_data.get("id")
                path = struct_def_data.get("path")
                input_path = node.get("inputPath")
                column_name = node.get("sourceColumn")
                slicing = struct_def_data.get("slicing")
                pattern = struct_def_data.get("pattern")
                max = struct_def_data.get("max") or "1"
                type_node = struct_def_data.get("type")
                constraint = struct_def_data.get("constraint")
                binding = struct_def_data.get("binding")
                mapping = struct_def_data.get("mapping")

                if not column_name:
                    input_path.split(".")[1]

                column_name = str(column_name).replace("_", "-")

                if slicing:
                    if path in slicing_added:
                        raise ValueError(
                            f"Slice path {path} appears on more than one node."
                            + " Please consildate slicing to a single node"
                        )
                    slicing_el = ElementDefinition(
                        path=slicing.get("path") or path,
                        id=slicing.get("id") or path,
                        slicing=ElementDefinitionSlicing(
                            rules=slicing.get("rules") or "open",
                            ordered=slicing.get("ordered") or False,
                            discriminator=cast(
                                list[ElementDefinitionSlicingDiscriminator],
                                slicing.get("discriminator"),
                            ),
                        ),
                        min=slicing.get("min"),
                        max=slicing.get("max"),
                    )

                    elements.append(slicing_el)
                    slicing_added.add(path)

                if struct_def_data.get("proceedingEd"):
                    elements.extend(struct_def_data.get("proceedingEd"))

                el = ElementDefinition(
                    path=path,
                    id=el_id,
                    short=input_path,
                    min=struct_def_data.get("min") or 0,
                    max=max,
                    isSummary=struct_def_data.get("isSummary") or None,
                    mustSupport=struct_def_data.get("mustSupport") or None,
                    sliceName=struct_def_data.get("sliceName") or None,
                )

                if pattern:
                    setattr(el, f"pattern{pattern.get('type')}", pattern.get("detail"))

                if type_node:
                    el.type = type_node

                if constraint:
                    el.constraint = constraint

                if mapping:
                    el.mapping = mapping

                if binding:
                    el.binding = binding

                elements.append(el)

            sorted_elements = []
            for p in fhir_defs.expand_type_paths(self.resource_type):
                els = [el for el in elements if get_field(el, "path") == p]
                slice_names = [
                    get_field(el, "sliceName") for el in els if get_field(el, "sliceName")
                ]
                if slice_names:
                    slices = [el for el in els if get_field(el, "slicing")]
                    if slices:
                        for el in slices:
                            if el not in sorted_elements:
                                sorted_elements.append(el)
                    for slice_name in slice_names:
                        for el in [el for el in els if get_field(el, "sliceName") == slice_name]:
                            if el not in sorted_elements:
                                sorted_elements.append(el)
                        for el in [
                            el
                            for el in elements
                            if str(get_field(el, "path")).startswith(p)
                            and f":{slice_name}" in str(get_field(el, "id"))
                        ]:
                            if el not in sorted_elements:
                                sorted_elements.append(el)
                else:
                    for el in els:
                        if el not in sorted_elements:
                            sorted_elements.append(el)

            structure_def = StructureDefinition(
                url=self.url,
                name=self.name,
                status="active",
                kind="resource",
                abstract=False,
                type="ExplanationOfBenefit",
                differential=StructureDefinitionDifferential(element=sorted_elements),
                version=self.version,
                fhirVersion="4.0.1",
                title=self.title,
                date=cast(DateTimeType, datetime.now(UTC).isoformat()),
                language="en",
                baseDefinition=source_structure_def.url,
                derivation="constraint",
                jurisdiction=[
                    CodeableConcept(
                        coding=[
                            Coding(
                                system="urn:iso:std:iso:3166",
                                code="US",
                                display="United States of America",
                            )
                        ]
                    )
                ],
                description=self.description,
            )

            differential_file.write(structure_def.model_dump_json(indent=2))
            differential_file.flush()

            self.generate_snapshot(
                input_sd_path=sd_json_path, differential_path=Path(differential_file.name)
            )

        print(f"Successfully generated sample JSON: {self.output_file_name}")

    def download_structure_definition(self, narrative_url: str, output_path: Path) -> None:
        """Convert an IG narrative page URL into its raw JSON canonical form and download it."""
        json_url = narrative_url.replace("/en/", "/") + ".json"
        resp = requests.get(json_url, headers={"Accept": "application/fhir+json"})
        resp.raise_for_status()
        output_path.write_text(resp.text, encoding="utf-8")
        print(f"Downloaded: {json_url} -> {output_path}")

    def generate_snapshot(
        self,
        input_sd_path: Path,
        differential_path: Path,
    ) -> None:
        output_path = Path(f"{self.output_directory}/{self.output_file_name}")
        validator_jar_path = get_validator_jar_path()
        if not validator_jar_path.exists():
            install_validator()
        cmd = [
            "java",
            "-jar",
            str(validator_jar_path.resolve()),
            str(
                differential_path.resolve()
            ),  # existing profile, already has a snapshot — used as context
            "-tx",
            "n/a",
            "-ig",
            str(
                input_sd_path.resolve()
            ),  # the one you're creating, differential only, no snapshot yet
            "-snapshot",
            "-output",
            str(output_path.resolve()),
        ]
        print(f"Running: {' '.join(cmd)}")
        result = subprocess.run(cmd, check=False, capture_output=True, text=True)

        if result.returncode != 0:
            print(result.stdout)
            print(result.stderr)
            raise RuntimeError(f"Snapshot generation failed (exit {result.returncode})")

        print(f"Snapshot written to: {output_path}")


def get_field(el: Any, field: str) -> Any | None:
    if isinstance(el, dict):
        return el.get(field)
    return getattr(el, field, None)
