import json
import subprocess
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
        fsh_directory: str,
        base_definition: str,
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
        self.fsh_directory = fsh_directory
        self.base_definition = base_definition

    def run(self) -> None:
        """Generate StructureDefinition Resource."""
        print(f"Source Directory: {self.source_directory}")
        print(f"Target Directory: {self.output_directory}")
        print(f"FSH Directory: {self.fsh_directory}")

        yaml_path = Path(f"{self.source_directory}/{self.source_yaml}")
        with yaml_path.open(mode="r", encoding="utf-8") as f:
            data = yaml.safe_load(f)

        data = [node for node in data if self.profile in node.get("profiles", [])]

        sd_json_path = self.download_structure_definition(
            self.base_definition,
            Path(f"{self.output_directory}/C4BB-ExplanationOfBenefit-Pharmacy-Basis.json"),
        )

        source_structure_def = StructureDefinition.model_construct(
            **json.loads(sd_json_path.read_text(encoding="UTF-8"))
        )

        print(source_structure_def.id)

        elements = []
        slicing_added = set()
        for node in data:
            struct_def_data = node.get("structureDef")
            if not struct_def_data:
                continue
            el_id = struct_def_data.get("id")
            path = struct_def_data.get("path")
            input_path = node.get("inputPath")
            column_name = node.get("sourceColumn")
            slicing = struct_def_data.get("slicing")
            pattern = struct_def_data.get("pattern")
            max = node.get("max") or "1"

            if not column_name:
                input_path.split(".")[1]

            column_name = str(column_name).replace("_", "-")

            human = node.get("definitionOverride")
            if not human:
                sd_sushi_path = Path(f"{self.fsh_directory}/StructureDefinition-{column_name}.json")
                if sd_sushi_path.exists():
                    gen_structure_def = StructureDefinition.model_construct(
                        **json.loads(sd_sushi_path.read_text(encoding="utf-8"))
                    )
                    human = gen_structure_def.description
                else:
                    human = f"CMS Field: {column_name}"

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

            elements.append(el)

        sorted_elements = sorted(
            elements,
            key=lambda element: (
                element.path,
                element.slicing is None,
                element.id,
                element.sliceName,
            ),
        )

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

        output_file_name = "StructureDef.json"
        output_path = Path(f"{self.output_directory}/{output_file_name}")
        output_path.parent.mkdir(parents=True, exist_ok=True)

        with output_path.open(mode="w", encoding="utf-8") as f:
            f.write(structure_def.model_dump_json(indent=2))

        self.generate_snapshot(input_sd_path=sd_json_path, output_path=output_path)

        print(f"Successfully generated sample JSON: {output_file_name}")

    def download_structure_definition(self, narrative_url: str, output_path: Path) -> Path:
        """Convert an IG narrative page URL into its raw JSON canonical form and download it."""
        json_url = narrative_url.replace("/en/", "/") + ".json"
        resp = requests.get(json_url, headers={"Accept": "application/fhir+json"})
        resp.raise_for_status()
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(resp.text, encoding="utf-8")
        print(f"Downloaded: {json_url} -> {output_path}")
        return output_path

    def generate_snapshot(
        self,
        input_sd_path: Path,
        output_path: Path,
        validator_jar_path: str = "validator_cli.jar",
    ) -> Path:
        output_file_name = "StructureDefNew.json"
        output_path_new = Path(f"{self.output_directory}/{output_file_name}")
        cmd = [
            "java",
            "-jar",
            validator_jar_path,
            str(
                output_path.resolve()
            ),  # existing profile, already has a snapshot — used as context
            "-ig",
            str(
                input_sd_path.resolve()
            ),  # the one you're creating, differential only, no snapshot yet
            "-snapshot",
            "-output",
            str(output_path_new.resolve()),
        ]
        print(f"Running: {' '.join(cmd)}")
        result = subprocess.run(cmd, check=False, capture_output=True, text=True)

        if result.returncode != 0:
            print(result.stdout)
            print(result.stderr)
            raise RuntimeError(f"Snapshot generation failed (exit {result.returncode})")

        print(f"Snapshot written to: {output_path}")
        return output_path
