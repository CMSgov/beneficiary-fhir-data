Placeholder directories + structures for mapping between IDR and FHIR.

To download the FHIR Validator:
https://github.com/hapifhir/org.hl7.fhir.core/releases/latest/download/validator_cli.jar

To execute FHIR Mapping language transforms against the sample data.
To compile the StructureMap:
```sh
java -jar validator_cli.jar -ig maps/CoverageBase.map -compile https://bfd.cms.gov/MappingLanguage/maps/CoverageBase -version 4.0.1 -output StructureMaps/BFDCoverageBaseStructureMap.json 
```

To execute:
Part A Example:
```sh
java -jar validator_cli.jar sample-data/CoverageFFSSample.json -output outputs/CoverageFFS.json -transform https://bfd.cms.gov/MappingLanguage/maps/CoverageBase -version 4.0.1 -ig StructureMaps/BFDCoverageBaseStructureMap.json -ig StructureDefinitions/Source/CoverageBase.json -ig maps/CoverageHelper.map
```

Validating sample resources against self-defined StructureDefinitions:
```sh
java -jar validator_cli.jar sample-data/<input>.json -ig StructureDefinitions/Source/<applicable structure definition>.json
```

Validate against C4BB:
```sh
java -jar validator_cli.jar outputs/<output_json>.json -ig hl7.fhir.us.carin-bb#2.1.0
```

Coverage FFS example:
```sh
java -jar validator_cli.jar outputs/CoverageFFS.json -ig hl7.fhir.us.carin-bb#2.1.0
```

Of note for the MDCR_ENTLMT information, ensure that we're looking at both the IDR_LTST_TRANS_FLG + IDR_TRANS_OBSLT_TS = end of time (9999-12-31). Otherwise, there could be indeterminate results.


Tables used:
//V2_MDCR_BENE_TP.BENE_BUYIN_CD + V2_MDCR_BENE_TP.BENE_TP_TYPE_CD (and associated values to get the "latest version" of a given concept.)
//V2_MDCR_BENE_MDCR_ENTLMT.
//V2_MDCR_BENE_MDCR_STUS.
//V2_BENE_MDCR_ENTLMT_RSN
