Placeholder directories + structures for mapping between IDR and FHIR.

To download the FHIR Validator:
https://github.com/hapifhir/org.hl7.fhir.core/releases/latest/download/validator_cli.jar

To execute FHIR Mapping language transforms against the sample data.
To compile the StructureMap:
```sh
java -jar validator_cli.jar -ig maps/patient.map \
  -compile https://bfd.cms.gov/MappingLanguage/Resources/Patient \
  -version 4.0.1 -output StructureMaps/bfd_patient_structuremap.json 
```

To execute:
```sh
java -jar validator_cli.jar sample-data/BeneficiarySample.json \
  -output outputs/Patient.json \
  -transform https://bfd.cms.gov/MappingLanguage/Resources/Patient \
  -version 4.0.1 \
  -ig StructureMaps/bfd_patient_structuremap.json \
  -ig StructureDefinitions/Source/Bene-MBI.json \
  -ig StructureDefinitions/Source/Bene.json \
  -ig hl7.fhir.us.carin-bb#2.0.0 \
  -ig maps/Patient-Helper.map
```


Validating sample resources against self-defined StructureDefinitions:
```sh
java -jar validator_cli.jar sample-data/<input>.json \
  -ig StructureDefinitions/Source/<applicable structure definition>.json
```

Validate against C4BB:
```sh
java -jar validator_cli.jar outputs/<output_json>.json \
  -ig hl7.fhir.us.carin-bb#2.0.0
```
