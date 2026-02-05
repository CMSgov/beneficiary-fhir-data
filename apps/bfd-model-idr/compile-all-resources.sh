#!/usr/bin/env bash

uv run compile_resources.py \
    -m maps/patient.map \
    -i sample-data/Beneficiary-Sample.json \
    -o out/Patient.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/Patient \
    --sushi

uv run compile_resources.py \
    -m maps/Coverage-Base.map \
    -i sample-data/Coverage-FFS-Sample.json \
    -o out/Coverage-FFS.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base

uv run compile_resources.py \
    -m maps/Coverage-Base.map \
    -i sample-data/Coverage-FFS-Sample-PartB.json \
    -o out/Coverage-FFS-PartB.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base  \
    --skip-structure-map-generation

uv run compile_resources.py \
    -m maps/Coverage-Base.map \
    -i sample-data/Coverage-PartC-Sample.json \
    -o out/Coverage-PartC.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base \
    --skip-structure-map-generation \
    --test

uv run compile_resources.py \
    -m maps/Coverage-Base.map \
    -i sample-data/Coverage-PartD-Sample.json \
    -o out/Coverage-PartD.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base \
    --skip-structure-map-generation
    
uv run compile_resources.py \
    -m maps/Coverage-Base.map \
    -i sample-data/Coverage-Dual-Sample.json \
    -o out/Coverage-Dual.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base \
    --skip-structure-map-generation

uv run compile_resources.py \
    -m maps/ExplanationOfBenefit-Base.map \
    -i sample-data/EOB-Base-Sample.json \
    -o out/ExplanationOfBenefit.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base \
    --test

uv run compile_resources.py \
    -m maps/ExplanationOfBenefit-Base.map \
    -i sample-data/EOB-SNF-Sample.json \
    -o out/ExplanationOfBenefit-SNF.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base \
    --skip-structure-map-generation

uv run compile_resources.py \
    -m maps/ExplanationOfBenefit-Base.map \
    -i sample-data/EOB-HHA-Sample.json \
    -o out/ExplanationOfBenefit-HHA.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base \
    --skip-structure-map-generation

uv run compile_resources.py \
    -m maps/ExplanationOfBenefit-Base.map \
    -i sample-data/EOB-Hospice-Sample.json \
    -o out/ExplanationOfBenefit-Hospice.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base \
    --skip-structure-map-generation

uv run compile_resources.py \
    -m maps/ExplanationOfBenefit-Base.map \
    -i sample-data/EOB-Institutional-Outpatient-Sample.json \
    -o out/ExplanationOfBenefit-Outpatient.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base \
    --skip-structure-map-generation

uv run compile_resources.py \
    -m maps/ExplanationOfBenefit-Base.map \
    -i sample-data/EOB-Carrier-MCS-Sample.json \
    -o out/ExplanationOfBenefit-MCS.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base \
    --test \
    --skip-structure-map-generation

uv run compile_resources.py \
    -m maps/ExplanationOfBenefit-Base.map \
    -i sample-data/EOB-Carrier-Sample.json \
    -o out/ExplanationOfBenefit-Carrier.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base \
    --skip-structure-map-generation

uv run compile_resources.py \
    -m maps/ExplanationOfBenefit-Base.map \
    -i sample-data/EOB-DME-Sample.json \
    -o out/ExplanationOfBenefit-DME.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base \
    --skip-structure-map-generation

uv run compile_resources.py \
    -m maps/ExplanationOfBenefit-Pharmacy.map \
    -i sample-data/EOB-Pharmacy-Sample.json \
    -o out/ExplanationOfBenefit-Pharmacy.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Pharmacy \
    --test 
