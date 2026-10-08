import json
import re
import sys

import click
from prompt_toolkit import HTML, choice

resources = [
    {
        "name": "Beneficiary",
        "type": "Beneficiary",
        "map": "maps/patient.map",
        "sample": "sample-data/Beneficiary-Sample.json",
        "output": "out/Patient.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/Patient",
    },
    {
        "name": "Coverage - Part A",
        "type": "Coverage",
        "map": "maps/Coverage-Base.map",
        "sample": "sample-data/Coverage-FFS-Sample.json",
        "output": "out/Coverage-FFS.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base",
    },
    {
        "name": "Coverage - Part B",
        "type": "Coverage",
        "map": "maps/Coverage-Base.map",
        "sample": "sample-data/Coverage-FFS-Sample-PartB.json",
        "output": "out/Coverage-FFS-PartB.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base",
    },
    {
        "name": "Coverage - Part C",
        "type": "Coverage",
        "map": "maps/Coverage-Base.map",
        "sample": "sample-data/Coverage-PartC-Sample.json",
        "output": "out/Coverage-PartC.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base",
    },
    {
        "name": "Coverage - Part D",
        "type": "Coverage",
        "map": "maps/Coverage-Base.map",
        "sample": "sample-data/Coverage-PartD-Sample.json",
        "output": "out/Coverage-PartD.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base",
    },
    {
        "name": "Coverage - Dual Enrollment",
        "type": "Coverage",
        "map": "maps/Coverage-Base.map",
        "sample": "sample-data/Coverage-Dual-Sample.json",
        "output": "out/Coverage-Dual.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base",
    },
    {
        "name": "EOB - Base",
        "type": "EOB",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-Base-Sample.json",
        "output": "out/ExplanationOfBenefit.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - SNF",
        "type": "EOB",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-SNF-Sample.json",
        "output": "out/ExplanationOfBenefit-SNF.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - HHA",
        "type": "EOB",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-HHA-Sample.json",
        "output": "out/ExplanationOfBenefit-HHA.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - Hospice",
        "type": "EOB",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-Hospice-Sample.json",
        "output": "out/ExplanationOfBenefit-Hospice.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - Outpatient",
        "type": "EOB",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-Institutional-Outpatient-Sample.json",
        "output": "out/ExplanationOfBenefit-Outpatient.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - MCS",
        "type": "EOB",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-Carrier-MCS-Sample.json",
        "output": "out/ExplanationOfBenefit-MCS.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - Carrier",
        "type": "EOB",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-Carrier-Sample.json",
        "output": "out/ExplanationOfBenefit-Carrier.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - DME",
        "type": "EOB",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-DME-Sample.json",
        "output": "out/ExplanationOfBenefit-DME.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - Pharmacy",
        "type": "EOB Pharmacy",
        "map": "maps/ExplanationOfBenefit-Pharmacy.map",
        "sample": "sample-data/EOB-Pharmacy-Sample.json",
        "output": "out/ExplanationOfBenefit-Pharmacy.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Pharmacy",
    },
    {
        "name": "Audit Event",
        "type": "Audit",
        "map": "maps/AuditEvent.map",
        "sample": "sample-data/BFDAuditLog-Sample.json",
        "output": "out/AuditEvent.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/BFDAuditLog",
    },
    {
        "name": "Prior Auth",
        "type": "Prior Auth",
        "map": "maps/ExplanationOfBenefit-PriorAuth.map",
        "sample": "sample-data/EOB-PriorAuth-Sample.json",
        "output": "out/ExplanationOfBenefit-PriorAuth.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-PriorAuth",
    },
]


def normalize(s: str) -> str:
    return re.sub("[^a-zA-Z]", "", s.lower())


def print_resource(resource: str) -> None:
    if resource == "all":
        print(json.dumps(resources))
    elif resource == "":
        prev_stdout = sys.stdout
        sys.stdout = sys.stderr
        result = choice(
            message=HTML("<bold><cyan>Choose a resource</cyan></bold>\n"),
            options=[(r["name"], r["name"]) for r in resources],
            show_frame=True,
        )
        sys.stdout = prev_stdout
        print(json.dumps(next(r for r in resources if r["name"] == result)))
    else:
        print(json.dumps(next(r for r in resources if normalize(r["name"]) == normalize(resource))))


def print_type(type: str) -> None:
    if type == "all":
        print(json.dumps(list({r["type"] for r in resources})))
        return

    if type == "":
        prev_stdout = sys.stdout
        sys.stdout = sys.stderr
        result = choice(
            message=HTML("<bold><cyan>Choose a resource type</cyan></bold>\n"),
            options=list({(r["type"], r["type"]) for r in resources}),
            show_frame=True,
        )
        sys.stdout = prev_stdout
        found = next(r for r in resources if r["type"] == result)

    else:
        found = next(r for r in resources if normalize(r["type"]) == normalize(type))
    print(json.dumps({"map": found["map"], "resource": found["resource"]}))


@click.command
@click.option("--resource", type=str)
@click.option("--type", type=str)
def main(resource: str | None, type: str | None):
    if resource is not None:
        print_resource(resource)
        return
    if type is not None:
        print_type(type)


if __name__ == "__main__":
    main()
