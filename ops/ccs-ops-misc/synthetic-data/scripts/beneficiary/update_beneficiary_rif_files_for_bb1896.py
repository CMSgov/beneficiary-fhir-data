import csv


# Set the following to the location of the target Column file to be used
# for setting the expected headers LIST (header_enum_order_list) for
# header reference and validation:
COLUMN_HEADER_ENUM_FILE = "./BeneficiaryColumn.java"

'''
This tool was used for remapping and formatting the files provided for
synthetic beneficiary data for the Jira BLUEBUTTON-1896 story (AB2D
request: generate synthetic part D enrollment data).

See https://jira.cms.gov/browse/BLUEBUTTON-1896 for more information.

The following RIF files were used for input:

rif_filenames_in = [
                    "synthetic_data/synthetic-beneficiary-1999.rif",
                    "synthetic_data/synthetic-beneficiary-2000.rif",
                    "synthetic_data/synthetic-beneficiary-2014.rif"]


It performs the following:

1. Loops through the files in the rif_filenames_in LIST.  Appends a ".new"
   to the newly mapped file.

2. Uses a column header ordering list: header_enum_order_list
   This is taken from the BeneficiaryColumn ENUM list in the following BFD
   code:
   ./apps/bfd-model/bfd-model-rif/target/generated-sources/annotations/
                       gov/cms/bfd/model/rif/BeneficiaryColumn.java

   The RIF loader expects the columns in the files to be in this order.

   UPDATE: This now generates the header_enum_order_list LIST from the actual
           Column Java file. This code can be reused for future mapping tools
           that work with other tables/RIF files.

           Set the global

3. Verifies all column headers from the input CSV file's top header row are
   all in the expected headers list: header_enum_order_list.


4. Verifies BENE_ID's are for sythetic.  Throws an exception if not.

5  Validates that the count of fields output is the same
   as header_enum_order_list.

6. If a column header from header_enum_order_list is not included in the file,
   throws an exception.

7. Strip leading and trailing whitespace for field entries.

     Was getting this error from the RIF processor:
           gov.cms.bfd.model.rif.parse.RifParsingUtils.parseDecimal
       Caused by: gov.cms.bfd.model.rif.parse.InvalidRifValueException:
            Unable to parse decimal value: ' 2267800412'.

8. Uses the BENE_ID ranges to identify which mapping transform to perform.

9. Map fields using the following information and rules:

   Data mapping info:

    This is the CCW codebook name mapping to the Beneficiary RIF file mapping:

    BENE RIF              CCW Codebook
    -----------------     ------------

    PTD_CNTRCT_JAN_ID     PTDCNTRCT01
    PTD_CNTRCT_FEB_ID     PTDCNTRCT02
    PTD_CNTRCT_MAR_ID     PTDCNTRCT03
    PTD_CNTRCT_APR_ID     PTDCNTRCT04
    PTD_CNTRCT_MAY_ID     PTDCNTRCT05
    PTD_CNTRCT_JUN_ID     PTDCNTRCT06
    PTD_CNTRCT_JUL_ID     PTDCNTRCT07
    PTD_CNTRCT_AUG_ID     PTDCNTRCT08
    PTD_CNTRCT_SEPT_ID    PTDCNTRCT09
    PTD_CNTRCT_OCT_ID     PTDCNTRCT10
    PTD_CNTRCT_NOV_ID     PTDCNTRCT11
    PTD_CNTRCT_DEC_ID     PTDCNTRCT12

   Mapping rules:

       Out of the 30K benes available:

       Using the BENE_ID to identify the ranges.

          synthetic_data/synthetic-beneficiary-1999.rif:
              -19990000000001 thru -19990000010000

          synthetic_data/synthetic-beneficiary-2000.rif:
              -20000000000001 thru -20000000010000

          synthetic_data/synthetic-beneficiary-2014.rif:
              -20140000000001 thru -20140000010000

       contract_z0 range: -19990000000001 thru -19990000000100
           contract_z0_count = 100
           Assign 100 benes to contract Z0000
          (i.e. write Z0000 into ptdcntrct01...ptdcntrct12 fields
                for 100 first benes)

       contract_z1 range: -19990000000101 thru -19990000001100
           contract_z1_count = 1000
           Assign 1000 benes to Z0001

       contract_z2 range: -19990000001101 thru -19990000003100
           contract_z2_count = 2000
           Assign 2000 benes to Z0002

       contract_z5 range: -19990000003101 thru -19990000008100
           contract_z5_count = 5000
           Assign 5000 benes to Z0005

       contract_z10 range: -19990000008101 thru -19990000010000
                           -20000000000001 thru -20000000008100
           contract_z10_count = 10000
           Assign 10,000 benes to Z0010

       contract_z12 range: -20000000008101 thru -20000000010000
                           -20140000000001 thru -20140000010000
           contract_z12_count = 11900
           Assign the remaining 11,900 benes to Z0012.

'''

# LIST of input files. Output files with have ".new" appended.
rif_filenames_in = [
                    "synthetic_data/synthetic-beneficiary-1999.rif",
                    "synthetic_data/synthetic-beneficiary-2000.rif",
                    "synthetic_data/synthetic-beneficiary-2014.rif"]

in_delimiter = "|"

# These are the target PTD columns to be updated
ptd_contract_list = ["PTD_CNTRCT_JAN_ID",
                     "PTD_CNTRCT_FEB_ID",
                     "PTD_CNTRCT_MAR_ID",
                     "PTD_CNTRCT_APR_ID",
                     "PTD_CNTRCT_MAY_ID",
                     "PTD_CNTRCT_JUN_ID",
                     "PTD_CNTRCT_JUL_ID",
                     "PTD_CNTRCT_AUG_ID",
                     "PTD_CNTRCT_SEPT_ID",
                     "PTD_CNTRCT_OCT_ID",
                     "PTD_CNTRCT_NOV_ID",
                     "PTD_CNTRCT_DEC_ID"]


''' Mapping dictionary for columns that are updated:

             Dict item:  [ start_id, end_id, new_contract_id,
                           expected_count, updated_counter ]

             NOTE: Will compare ID's as integer values for ranges.
'''
ptd_contract_map_dict = {"Z0": [-19990000000100,
                                -19990000000001,
                                "Z0000",
                                100,
                                0],
                         "Z1": [-19990000001100,
                                -19990000000101,
                                "Z0001",
                                1000,
                                0],
                         "Z2": [-19990000003100,
                                -19990000001101,
                                "Z0002",
                                2000,
                                0],
                         "Z5": [-19990000008100,
                                -19990000003101,
                                "Z0005",
                                5000,
                                0],
                         "Z10a": [-19990000010000,
                                  -19990000008101,
                                  "Z0010",
                                  1900,
                                  0],
                         "Z10b": [-20000000008100,
                                  -20000000000001,
                                  "Z0010",
                                  8100,
                                  0],
                         "Z12a": [-20000000010000,
                                  -20000000008101,
                                  "Z0012",
                                  1900,
                                  0],
                         "Z12b": [-20140000010000,
                                  -20140000000001,
                                  "Z0012",
                                  10000,
                                  0], }

header_enum_order_list = ["DML_IND"]

print("")
print("")
print("--------CONFIGURATION SUMMARY--------")
print("")
print("")
print("BENE_ID MAPPING DICTIONARY:  ", ptd_contract_map_dict)
print("")
print("PROCESSING FILES FROM rif_filenames_in LIST: ", rif_filenames_in)
print("")
print("")
print("UPDATING COLUMNS IN THE ptd_contract_list:  ", ptd_contract_list)
print("")
print("")
print("--------RUNNING--------")

# Generate  header_enum_order_list headers LIST from *Column.java source file
f = open(COLUMN_HEADER_ENUM_FILE, 'r')

parse_items_flag = False

for line in f:
    if "}" in line:
        parse_items_flag = False

    if parse_items_flag:
        # Does line contain any non space chars?
        if not line.isspace():
            line = line.replace(" ", "").replace(",", "").replace("\n", "")
            header_enum_order_list.append(line)

    if "{" in line:
        parse_items_flag = True

f.close()

# process rif files:
for filename_in in rif_filenames_in:

    filename_out = filename_in + ".new"

    with open(filename_in, 'r') as file_in:

        file_out = open(filename_out, 'w')

        print("PROCESSING FILE: IN={}  OUT={}".format(filename_in,
                                                      filename_out))

        # creating a csv reader object
        csvreader = csv.DictReader(file_in, delimiter=in_delimiter)

        # Output header line
        out_line = '|'.join(header_enum_order_list)
        print(out_line, file=file_out)

        row_count = 0
        # Iterate over all rows
        for row in csvreader:
            row_count = row_count + 1

            # 2. Verify all header keys are in the header_enum_order_list.
            if row_count == 1:
                for key in row:
                    if key not in header_enum_order_list:
                        raise SystemExit("ERROR: Input file header key " +
                                         "is not in the " +
                                         "header_enum_order_list list: " +
                                         key)

            # Output row fields based on enum mapping
            row_list = []
            for col_name in header_enum_order_list:
                # Is the col_name included in the input file?
                if col_name in row:
                    if col_name == "BENE_ID" and int(row["BENE_ID"]) > 0:
                        raise SystemExit("ERROR: Input file header key " +
                                         " is not in the " +
                                         "header_enum_order_list list: " +
                                         key)

                    # Map PDE CONTRACT fields
                    # Get map_list for bene_id
                    if col_name == "BENE_ID":
                        bene_id = int(row["BENE_ID"])
                        new_contract_map_list = None
                        # Find the new id to update to via map dict.
                        for map_name in ptd_contract_map_dict:
                            map_list = ptd_contract_map_dict[map_name]
                            start_id = map_list[0]
                            end_id = map_list[1]
                            if bene_id >= start_id and bene_id <= end_id:
                                new_contract_map_list = map_list
                                # increment counter
                                ptd_contract_map_dict[map_name][4] += 1
                                break

                        if not new_contract_map_list:
                            raise SystemExit("ERROR: Does not have a mapping" +
                                             " entry in dictionary for " +
                                             "bene_id:  " + str(bene_id))

                    # Is this a contract field that gets updated?
                    if col_name in ptd_contract_list:
                        # Update with the contract ID from list
                        row_list.append(new_contract_map_list[2])
                    else:
                        # Copy field as is.
                        row_list.append(row[col_name].strip())

                else:
                    # col_name is not in row, so provide empty value
                    raise SystemExit("ERROR: col_name entry is not in " +
                                     " row for:  " + col_name)

            # Validate row_list count is same as header_enum_order_list.
            if len(row_list) != len(header_enum_order_list):
                raise SystemExit("ERROR: Was expecting: " +
                                 str(len(header_enum_order_list)) +
                                 "  field count but got: " +
                                 str(len(row_list)))

            # Output row to stdout
            print('|'.join(row_list), file=file_out)

        file_out.close()

print("")
print("")
print("--------COMPLETION SUMMARY--------")
print("")
print("")
print("BENE_ID MAPPING DICTIONARY:  ", ptd_contract_map_dict)
print("")
print("")
# Verify counts for updates
for map_name in ptd_contract_map_dict:
    map_list = ptd_contract_map_dict[map_name]
    start_id = map_list[0]
    end_id = map_list[1]
    contract_id = map_list[2]
    expected_count = map_list[3]
    updated_count = map_list[4]

    msg = "MAPPING {}:  Updated {} of {} expected records with new "\
          " contract ID = {}"
    print(msg.format(map_name, updated_count, expected_count, contract_id))
    print("")
    if updated_count != expected_count:
        raise SystemExit("ERROR: Was expecting a record count of: " +
                         str(expected_count))

print("")
print("")
print("SUCCESS:  Processing of all files was successful!!!")
