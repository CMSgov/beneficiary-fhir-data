#!/usr/bin/env bash

uv run compile_resources.py \
    -m maps/patient.map \
    -i sample-data/Beneficiary-Sample.json \
    -o out/Patient.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/Patient \
    --test

uv run compile_resources.py \
    -m maps/Coverage-Base.map \
    -i sample-data/Coverage-FFS-Sample.json \
    -o out/Coverage-FFS.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base \
    --test

uv run compile_resources.py \
    -m maps/Coverage-Base.map \
    -i sample-data/Coverage-PartC-Sample.json \
    -o out/Coverage.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base \
    --test

uv run compile_resources.py \
    -m maps/EOB-Base.map \
    -i sample-data/EOB-Base-Sample.json \
    -o out/ExplanationOfBenefit.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base \
    --test

uv run compile_resources.py \
    -m maps/EOB-Pharmacy.map \
    -i sample-data/EOB-Pharmacy-Sample.json \
    -o out/ExplanationOfBenefit-Pharmacy.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Pharmacy \
    --test
