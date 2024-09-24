# CSV files

SAMHSA codes to check against are loaded from CSV files.
These files are normalized with the following rules.

## DRG Codes
- Trim whitespace
- Remove "MS-DRG " prefix
- Result is just the numeric value. ex: "897"

## CPT Codes
- Trim whitespace
- Convert to uppercase
- Result is alphanumeric string. ex: "T1006"

## ICD 9 Procedure Codes
- Trim whitespace
- Remove first decimal point
- Convert to uppercase
- Result is numeric string. ex: "9445"

## ICD 9 Diagnosis Codes
- Trim whitespace
- Remove first decimal point
- Convert to uppercase
- Result is numeric string. ex: "30410"

## ICD 10 Procedure Codes
- Trim whitespace
- Remove first decimal point
- Convert to uppercase
- Result is alphanumeric string. ex: "HZ53ZZZ"

## ICD 10 Diagnosis Codes
- Trim whitespace
- Remove first decimal point
- Convert to uppercase
- Result is alphanumeric string. ex: "F16188"

## Coding URLS
These URLs are what it checks against to match the type of code
- ICD 9: `http://hl7.org/fhir/sid/icd-9-cm`
- ICD 9 Medicare: `http://www.cms.gov/Medicare/Coding/ICD9`
- ICD 10: `http://hl7.org/fhir/sid/icd-10`
- ICD 10 CM: `http://hl7.org/fhir/sid/icd-10-cm`
- ICD 10 Medicare: `http://www.cms.gov/Medicare/Coding/ICD10`
- DRG: `https://bluebutton.cms.gov/resources/variables/clm_drg_cd`

# Checks for PAC data

```mermaid
flowchart TD
    id0["For each claim"]
    id1["For each procedure"]
    id2["For each coding system in procedure codeable concept"]
    id3["Normalize code (follows ICD normalization described above)"]
    id4{"Is code system ICD 10 CM?"}
    id5["Throw error"]
    id6["Check normalized ICD code against normalized SAMHSA procedure codes for the given system."]

    id7["For each diagnosis"]
    id8["For each coding system in diagnosis codeable concept"]
    id9["Normalize code (follows ICD normalization described above)"]
    id10{"Is code system ICD 10 Medicare?"}
    id11["Throw error"]
    id12["Check normalized ICD code against normalized SAMHSA diagnosis codes for the given system."]
    id13["For each coding system in package codeable concept"]
    id14["Mark SAMHSA if system is not equal to DRG."]
    id15["Check normalized DRG code against normalized SAMHSA DRG codes"]

    id16["For each line item"]
    id17["Mark NOT SAMHSA if code system is empty"]
    id18["For each code system"]
    id19["Mark NOT SAMHSA if the HCPCS system is not present"]
    id20["Normalize code (trim whitespace and convert to uppercase)"]
    id21["Check normalized HCPCS code against normalized CPT codes"]

    id0 --> id1
    id0 --> id7
    id0 --> id16
    id1 --> id2 --> id3 --> id4
    id4 -->|Yes| id5 
    id4 -->|No| id6

    id7 --> id8 --> id9 --> id10
    id10 -->|Yes| id11
    id10 -->|No| id12
    id7 --> id13
    id13 --> id14
    id13 --> id15

    id16 --> id17
    id16 --> id18
    id18 --> id19
    id18 --> id20 --> id21
```

# Checks for EOB

```mermaid
flowchart TD
    id0["For each claim"]

    id1{"Check claim type"}
    id2((" "))
    id3["For each procedure"]
    
    id4["For each coding system in procedure codeable concept"]
    id5["Normalize code (follows ICD normalization described above)"]
    id6{"Is code system ICD 10 CM?"}
    id7["Throw error"]
    id8["Check normalized ICD code against normalized SAMHSA procedure codes for the given system."]

    id9["For each supporting info"]
    id10["Filter out items that don't have the DRG system"]
    id11["Normalize code (follows ICD normalization described above)"]
    id12["Check normalized DRG code against normalized SAMHSA DRG codes"]

    id13((" "))

    id14["For each diagnosis"]
    id15["For each coding system in diagnosis codeable concept"]
    id16["Normalize code (follows ICD normalization described above)"]
    id17{"Is code system ICD 10 Medicare?"}
    id18["Throw error"]
    id19["Check normalized ICD code against normalized SAMHSA diagnosis codes for the given system."]
    id20["For each coding system in package codeable concept"]
    id21["Mark SAMHSA if system is not equal to DRG."]
    id22["Check normalized DRG code against normalized SAMHSA DRG codes"]

    id23["Do nothing"]

    id24["For each line item"]
    id25["Mark NOT SAMHSA if code system is empty"]
    id26["For each code system"]
    id27["Mark NOT SAMHSA if the HCPCS system is not present"]
    id28["Normalize code (trim whitespace and convert to uppercase)"]
    id29["Check normalized HCPCS code against normalized SAMHSA CPT codes"]

    id0 --> id1
    id1 -->|Inpatient, Outpatient, SNF| id2
    id1 -->|Carrier, DME, HHA, Hospice| id13
    id1 -->|PDE| id23
    id2 --> id3
    id3 --> id4 --> id5 --> id6
    id6 -->|Yes| id7
    id6 -->|No| id8

    id2 --> id13

    id2 --> id9 --> id10 --> id11 --> id12

    id13 --> id14
    id13 --> id24

    id14 --> id15 --> id16 --> id17
    id17 -->|Yes| id18
    id17 -->|No| id19
    id14 --> id20
    id20 --> id21
    id20 --> id22

    id24 --> id25
    id24 --> id26
    id26 --> id27
    id26 --> id28 --> id29
```
