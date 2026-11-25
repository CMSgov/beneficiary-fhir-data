import json
import subprocess
import tempfile
from datetime import UTC, datetime
from pathlib import Path
from subprocess import CompletedProcess
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
from requests.models import Response

from gen_structure_def.fhir_def import FHIRDefinitions
from install_fhir_validator import get_validator_jar_path
from install_fhir_validator import main as install_validator

JSON_FILE_TYPE: str = ".json"


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
        applies_to: list[str],
    ) -> None:
        self.source_directory: str = source_directory
        self.output_directory: str = output_directory
        self.url: str = url
        self.profile: str = profile
        self.source_yaml: str = source_yaml
        self.title: str = title
        self.version: str = version
        self.name: str = name
        self.description: str = description
        self.base_definition: str = base_definition
        self.resource_type: str = resource_type
        self.output_file_name: str = output_file_name
        self.applies_to: list[str] = applies_to
        self.sources: list[str] = []

    @property
    def delete_tmp_files(self) -> bool:
        return self._delete_tmp_files

    @delete_tmp_files.setter
    def delete_tmp_files(self, value: bool) -> None:
        self._delete_tmp_files: bool = value

    def run(self) -> None:
        """Generate StructureDefinition Resource."""
        print(f"Title: {self.title}")
        print(f"Source Directory: {self.source_directory}")
        print(f"Target Directory: {self.output_directory}")
        print(f"File: {self.output_file_name}")

        yaml_path: Path = Path(f"{self.source_directory}/{self.source_yaml}")
        with yaml_path.open(mode="r", encoding="utf-8") as f:
            yaml_data: list[Any] = yaml.safe_load(stream=f)

        data: list[Any] = [
            node
            for node in yaml_data
            if self.profile in node.get("profiles", [])
            and node.get("structureDef")
            and (
                not self.applies_to
                or any(apply in node.get("appliesTo", []) for apply in self.applies_to)
            )
            and (
                not self.sources
                or any(source in node.get("sources", []) for source in self.sources)
            )
        ]

        output_dir: Path = Path(self.output_directory)
        output_dir.mkdir(parents=True, exist_ok=True)
        with (
            tempfile.NamedTemporaryFile(
                mode="w", suffix=JSON_FILE_TYPE, delete=self.delete_tmp_files, dir=output_dir
            ) as sd_json_file,
            tempfile.NamedTemporaryFile(
                mode="w", suffix=JSON_FILE_TYPE, delete=self.delete_tmp_files, dir=output_dir
            ) as differential_file,
        ):
            sd_json_path: Path = Path(sd_json_file.name)
            self.download_structure_definition(
                narrative_url=self.base_definition,
                output_path=sd_json_path,
            )

            source_structure_def: StructureDefinition = StructureDefinition.model_construct(
                **json.loads(s=sd_json_path.read_text(encoding="UTF-8"))
            )

            elements: list[ElementDefinition] = []
            slicing_added: set[Any] = set[Any]()
            for node in data:
                self.process_node(
                    node=node,
                    elements=elements,
                    slicing_added=slicing_added,
                )

            structure_def: StructureDefinition = StructureDefinition(
                url=self.url,
                name=self.name,
                status="active",
                kind="resource",
                abstract=False,
                type="ExplanationOfBenefit",
                differential=StructureDefinitionDifferential(
                    element=self.sort_elements(elements=elements)
                ),
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

    def process_node(
        self, node: dict[str, Any], elements: list[ElementDefinition], slicing_added: set[Any]
    ) -> None:
        struct_def_data: Any = node.get("structureDef")
        column_name: Any | None = node.get("sourceColumn")
        if any(applies in struct_def_data.get("suppress", []) for applies in self.applies_to):
            print(f"Suppressing node: {column_name}")
            return
        el_id = struct_def_data.get("id")
        path = struct_def_data.get("path")
        input_path: Any | None = node.get("inputPath")
        slicing = struct_def_data.get("slicing")
        pattern = struct_def_data.get("pattern")
        max_value: str = struct_def_data.get("max") or "1"
        type_node = struct_def_data.get("type")
        constraint = struct_def_data.get("constraint")
        binding = struct_def_data.get("binding")
        mapping = struct_def_data.get("mapping")

        if not column_name and not input_path:
            input_path = self.parse_path(input_path=str(input_path))

        column_name = str(column_name).replace("_", "-")

        if slicing:
            self.process_slicing(
                slicing=slicing, path=path, elements=elements, slicing_added=slicing_added
            )

        if struct_def_data.get("proceedingEd"):
            elements.extend(struct_def_data.get("proceedingEd"))

        el: ElementDefinition = ElementDefinition(
            path=path,
            id=el_id,
            short=input_path,
            min=struct_def_data.get("min") or 0,
            max=max_value,
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

    def parse_path(self, input_path: str) -> str:
        return input_path.split(sep=".")[1]

    def process_slicing(
        self,
        slicing: dict[str, Any],
        path: str,
        elements: list[ElementDefinition],
        slicing_added: set[Any],
    ) -> None:
        if path in slicing_added:
            raise ValueError(
                f"Slice path {path} appears on more than one node."
                + " Please consildate slicing to a single node"
            )
        slicing_el: ElementDefinition = ElementDefinition(
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

    def sort_elements(self, elements: list[ElementDefinition]) -> list[Any]:
        fhir_defs: FHIRDefinitions = FHIRDefinitions()
        sorted_elements: list[Any] = []
        for path in fhir_defs.expand_type_paths(self.resource_type):
            els: list[ElementDefinition] = [
                el for el in elements if get_field(el, field="path") == path
            ]
            slice_names: list[object | None] = [
                get_field(el, field="sliceName") for el in els if get_field(el, field="sliceName")
            ]
            if slice_names:
                self.sort_slices(
                    path=path,
                    slice_names=slice_names,
                    els=els,
                    elements=elements,
                    sorted_elements=sorted_elements,
                )
            else:
                for el in els:
                    self.add_to_sorted(sorted_elements=sorted_elements, el=el)
        return sorted_elements

    def sort_slices(
        self,
        path: str,
        slice_names: list[object | None],
        els: list[ElementDefinition],
        elements: list[ElementDefinition],
        sorted_elements: list[Any],
    ) -> None:
        slices: list[ElementDefinition] = [el for el in els if get_field(el, field="slicing")]
        if slices:
            for el in slices:
                self.add_to_sorted(sorted_elements=sorted_elements, el=el)
        for slice_name in slice_names:
            for el in [el for el in els if get_field(el, field="sliceName") == slice_name]:
                self.add_to_sorted(sorted_elements=sorted_elements, el=el)
            for el in [
                el
                for el in elements
                if self.path_starts_with(el=el, path=path)
                and f":{slice_name}" in str(get_field(el, field="id"))
            ]:
                self.add_to_sorted(sorted_elements=sorted_elements, el=el)

    def path_starts_with(self, el: ElementDefinition, path: str) -> bool:
        return str(get_field(el, field="path")).startswith(path)

    def add_to_sorted(self, sorted_elements: list[Any], el: ElementDefinition) -> None:
        if el not in sorted_elements:
            sorted_elements.append(el)

    def download_structure_definition(self, narrative_url: str, output_path: Path) -> None:
        """Convert an IG narrative page URL into its raw JSON canonical form and download it."""
        json_url = narrative_url.replace("/en/", "/") + ".json"
        resp: Response = requests.get(url=json_url, headers={"Accept": "application/fhir+json"})
        resp.raise_for_status()
        output_path.write_text(data=resp.text, encoding="utf-8")
        print(f"Downloaded: {json_url} -> {output_path}")

    def generate_snapshot(
        self,
        input_sd_path: Path,
        differential_path: Path,
    ) -> None:
        output_path: Path = Path(f"{self.output_directory}/{self.output_file_name}")
        validator_jar_path = get_validator_jar_path()
        if not validator_jar_path.exists():
            install_validator()
        cmd: list[str] = [
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
        result: CompletedProcess[str] = subprocess.run(
            cmd, check=False, capture_output=True, text=True
        )

        if result.returncode != 0:
            print(result.stdout)
            print(result.stderr)
            raise RuntimeError(f"Snapshot generation failed (exit {result.returncode})")

        print(f"Snapshot written to: {output_path}")


def get_field(el: object, field: str) -> object | None:
    if isinstance(el, dict):
        return cast("dict[str, object]", el).get(field)
    return getattr(el, field, None)
