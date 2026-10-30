import json
import subprocess
import tempfile
from datetime import UTC, datetime
from pathlib import Path

import requests
import yaml
from fhir.resources.R4B.codeableconcept import CodeableConcept
from fhir.resources.R4B.coding import Coding
from fhir.resources.R4B.elementdefinition import (
    ElementDefinition,
    ElementDefinitionSlicing,
)
from fhir.resources.R4B.structuredefinition import (
    StructureDefinition,
    StructureDefinitionDifferential,
)

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
        delete_tmp_files: bool = True
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
        self.delete_tmp_files = delete_tmp_files

    def run(self) -> None:
        """Generate StructureDefinition Resource."""
        print(f"Source Directory: {self.source_directory}")
        print(f"Target Directory: {self.output_directory}")

        yaml_path = Path(f"{self.source_directory}/{self.source_yaml}")
        with yaml_path.open(mode="r", encoding="utf-8") as f:
            data = yaml.safe_load(f)

        data = [
            node
            for node in data
            if self.profile in node.get("profiles", []) and node.get("structureDef")
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

            elements = []
            slicing_added = set()
            for p in fhir_defs.expand_type_paths(self.resource_type):
                path_data = [node for node in data if node.get("structureDef").get("path") == p]
                path_data = sorted(
                    path_data,
                    key=lambda node: (node.get("structureDef").get("slicing") is None,),
                )
                for node in path_data:
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
                            path=path,
                            id=path,
                            slicing=ElementDefinitionSlicing(
                                rules=slicing.get("rules") or "open",
                            ),
                        )
                        discriminator = slicing.get("discriminator")
                        if discriminator:
                            slicing_el.slicing.discriminator = discriminator

                        elements.append(slicing_el)
                        slicing_added.add(path)

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
                        print("found type")
                        el.type = type_node

                    if constraint:
                        el.constraint = constraint

                    elements.append(el)

            structure_def = StructureDefinition(
                url=self.url,
                name=self.name,
                status="active",
                kind="resource",
                abstract=False,
                type="ExplanationOfBenefit",
                differential=StructureDefinitionDifferential(element=elements),
                version=self.version,
                fhirVersion="4.0.1",
                title=self.title,
                date=datetime.now(UTC).isoformat(),
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
        validator_jar_path: str = "validator_cli.jar",
    ) -> None:
        output_path = Path(f"{self.output_directory}/{self.output_file_name}")
        cmd = [
            "java",
            "-jar",
            validator_jar_path,
            str(
                differential_path.resolve()
            ),  # existing profile, already has a snapshot — used as context
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
