# Address Standardization (Project US@)

This module provides tools to standardize and wrap addresses into the Project US@ format (based on USPS standards) utilizing manual heuristics and `usaddress` parsing.

---

## 🏗️ Usage Context

When standardizing multi-line beneficiary addresses present in `IdrBeneficiary`, the address to format must be **assembled deterministically**, processed, and stored according to strict indexing structure.

### 1. Assembling the Input String
To process the address via `normalize_address`, concatenate the `BENE` address lines followed by the `GEO` geographic metadata line joined with newlines (`\n`).

```python
from matching import normalize_address

# Assemble components from IdrBeneficiary
address_parts = [
    model.bene_line_1_adr,
    model.bene_line_2_adr,
    model.bene_line_3_adr,
    model.bene_line_4_adr,
    model.bene_line_5_adr,
    model.bene_line_6_adr,
    # Assemble the last line formatting City State Zip
    f"{model.geo_zip_plc_name} {model.geo_usps_state_cd} {model.geo_zip5_cd}"
]

# Consolidate, removing any explicit empty lines
formatted_input = "\n".join([line for line in address_parts if line and line.strip()])

# Call standardization function
standardized_result = normalize_address(formatted_input)
```

### 2. Extracting Standardized Fields
`normalize_address` returns a consolidated string containing line-breaks (`\n`) ordered according to Appendix heuristics.

To pull explicitly structured information (e.g. for saving single lines), parse line breaks and read index positions:

```python
# Split the output
output_lines = standardized_result.split("\n")

# Pull Line 1 to save explicitly as the "US@ street line"
street_line = output_lines[0] if output_lines else ""

# The rest of the list accommodates secondary lines, standard boxes, etc.
# Typically, the last line in output_lines will hold the City State ZIP node.
```

---

## 🛠️ Testing

You can verify and test your matching heuristics running the provided test harness:
```bash
uv run pytest matching/test_parser.py
```
