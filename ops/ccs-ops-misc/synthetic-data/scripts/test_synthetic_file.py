import sys

def validate_file(filename):
    '''Validates synthea rif csv files and ensures that the file is valid for loading
    into the pipeline. This is primarily designed to check synthea files for minor
    errors post-generation. Validations can be added here as needed.
    '''

    fail = 'False'
    
    with open(filename) as infile:
        lineCount = 0
        expectedColumns = 0
        for line in infile:
            multiInsertCount = line.count("INSERT")
            if multiInsertCount > 1:
                print("Multiple INSERTs on line " + str(lineCount))
                fail = 'True'
            multiUpdateCount = line.count("UPDATE")
            if multiUpdateCount > 1:
                print("Multiple UPDATES on line " + str(lineCount))
                fail = 'True'
            doubleSpaceCount = line.count("  ")
            if doubleSpaceCount > 0:
                print("Multiple spaces on line " + str(lineCount))
                fail = 'True'
            columnCount = line.count("|")
            if expectedColumns == 0:
                expectedColumns = columnCount
            if columnCount != expectedColumns:
                print("Bad column count on line " + str(lineCount))
                fail = 'True'
            lineCount = lineCount + 1
            
    if fail == 'True':
        print("Validation failed")
    else:
        print("Validation passed")

## Runs the program via run args when this file is run
if __name__ == "__main__":
    validate_file(sys.argv[1])