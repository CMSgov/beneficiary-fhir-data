import json
import re
import sys

from prompt_toolkit import HTML, PromptSession, choice
from prompt_toolkit.formatted_text import FormattedText
from prompt_toolkit.output import create_output

resources = [
    {
        "name": "Beneficiary",
        "map": "maps/patient.map",
        "sample": "sample-data/Beneficiary-Sample.json",
        "output": "out/Patient.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/Patient",
    },
    {
        "name": "Coverage - Part A",
        "map": "maps/Coverage-Base.map",
        "sample": "sample-data/Coverage-FFS-Sample.json",
        "output": "out/Coverage-FFS.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base",
    },
    {
        "name": "Coverage - Part B",
        "map": "maps/Coverage-Base.map",
        "sample": "sample-data/Coverage-FFS-Sample-PartB.json",
        "output": "out/Coverage-FFS-PartB.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base",
    },
    {
        "name": "Coverage - Part C",
        "map": "maps/Coverage-Base.map",
        "sample": "sample-data/Coverage-PartC-Sample.json",
        "output": "out/Coverage-PartC.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base",
    },
    {
        "name": "Coverage - Part D",
        "map": "maps/Coverage-Base.map",
        "sample": "sample-data/Coverage-PartD-Sample.json",
        "output": "out/Coverage-PartD.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base",
    },
    {
        "name": "Coverage - Dual Enrollment",
        "map": "maps/Coverage-Base.map",
        "sample": "sample-data/Coverage-Dual-Sample.json",
        "output": "out/Coverage-Dual.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base",
    },
    {
        "name": "EOB - Base",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-Base-Sample.json",
        "output": "out/ExplanationOfBenefit.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - SNF",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-SNF-Sample.json",
        "output": "out/ExplanationOfBenefit-SNF.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - HHA",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-HHA-Sample.json",
        "output": "out/ExplanationOfBenefit-HHA.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - Hospice",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-Hospice-Sample.json",
        "output": "out/ExplanationOfBenefit-Hospice.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - Outpatient",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-Institutional-Outpatient-Sample.json",
        "output": "out/ExplanationOfBenefit-Outpatient.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - MCS",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-Carrier-MCS-Sample.json",
        "output": "out/ExplanationOfBenefit-MCS.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - Carrier",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-Carrier-Sample.json",
        "output": "out/ExplanationOfBenefit-Carrier.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - DME",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/EOB-DME-Sample.json",
        "output": "out/ExplanationOfBenefit-DME.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base",
    },
    {
        "name": "EOB - Pharmacy",
        "map": "maps/ExplanationOfBenefit-Pharmacy.map",
        "sample": "sample-data/EOB-Pharmacy-Sample.json",
        "output": "out/ExplanationOfBenefit-Pharmacy.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Pharmacy",
    },
    {
        "name": "Audit Event",
        "map": "maps/ExplanationOfBenefit-Base.map",
        "sample": "sample-data/BFDAuditLog-Sample.json",
        "output": "out/AuditEvent.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/BFDAuditLog",
    },
    {
        "name": "Prior Auth",
        "map": "maps/ExplanationOfBenefit-PriorAuth.map",
        "sample": "sample-data/EOB-PriorAuth-Sample.json",
        "output": "out/ExplanationOfBenefit-PriorAuth.json",
        "resource": "https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-PriorAuth",
    },
]


def normalize(s: str) -> str:
    return re.sub("[^a-zA-Z]", "", s.lower())


if len(sys.argv) > 1 and sys.argv[1]:
    print(json.dumps(next(r for r in resources if normalize(r["name"]) == normalize(sys.argv[1]))))
else:
    prev_stdout = sys.stdout
    sys.stdout = sys.stderr
    result = choice(
        message=HTML("<bold><cyan>Choose a resource</cyan></bold>\n"),
        options=[(r["name"], r["name"]) for r in resources],
        show_frame=True,
    )
    sys.stdout = prev_stdout
    print(json.dumps(next(r for r in resources if r["name"] == result)))
