Placeholder directories + structures for mapping between IDR and FHIR.

To download the FHIR Validator:
https://github.com/hapifhir/org.hl7.fhir.core/releases/latest/download/validator_cli.jar

To execute FHIR Mapping language transforms against the sample data.
To compile the StructureMap:
```sh
java -jar validator_cli.jar \
  -ig maps/EOB-Base.map \
  -compile https://bfd.cms.gov/MappingLanguage/maps/ExplanationOfBenefitBase \
  -version 4.0.1 \
  -output StructureMaps/bfd_eob_base_structuremap.json
  ```

To execute:
```sh
java -jar validator_cli.jar sample-data/EOBBaseSample.json \
  -output outputs/ExplanationOfBenefit.json \
  -transform https://bfd.cms.gov/MappingLanguage/maps/ExplanationOfBenefitBase \
  -version 4.0.1 \
  -ig StructureMaps/bfd_eob_base_structuremap.json \
  -ig StructureDefinitions/Source/ExplanationOfBenefitInstitutionalBase.json \
  -ig StructureDefinitions/Source/ExplanationOfBenefit_Base.json \
  -ig StructureDefinitions/Source/ProcedureComponent.json \
  -ig StructureDefinitions/Source/DiagnosisComponent.json \
  -ig StructureDefinitions/Source/SupportingInfoComponent.json \
  -ig StructureDefinitions/Source/LineItemComponent.json \
  -ig hl7.fhir.us.carin-bb#2.0.0 \
  -ig maps/EOB-Helper.map \
  -ig maps/EOB-SupportingInfo-Helper.map \
  -ig maps/EOB-Item-Institutional-Helper.map
```

Validating sample resources against self-defined StructureDefinitions:
```sh
java -jar validator_cli.jar sample-data/<input>.json -ig StructureDefinitions/Source/<applicable structure definition>.json
```

Validate against C4BB:
```sh
java -jar validator_cli.jar outputs/<output_json>.json \
  -ig hl7.fhir.us.carin-bb#2.1.0
```

EOB example:
```sh
java -jar validator_cli.jar outputs/ExplanationOfBenefit.json \
  -ig hl7.fhir.us.carin-bb#2.1.0
```
