import pandas as pd
import warnings
import csv
import sys

warnings.filterwarnings("ignore")

def split_file(filename):
    '''Splits beneficiary-interim RIF file to n number of RIF files 
     with only one beneficiary record per bene_id in each file.
    '''

    with open(filename) as infile:
        col = 'bene_id' # RIF file CSV column
        dfCol = 'BENE_ID' # column in data frame
        
        # load RIF data into dataframe 
        df = pd.read_csv(infile, sep='|', keep_default_na=False)
        df.head()
        csvHeader = (list(df.columns.values)) # CSV file header

        idList= (df[dfCol].unique().tolist()) # list of unique bene_ids
        beneRows = [] # list of lists of records for each bene_id
        for id in idList:
            beneRows.append(pd.DataFrame(df.loc[df['BENE_ID'] == id]).values.tolist())

        zipped_list = list(zip(*beneRows)) # list of n lists with a unique record for a given bene_id

        # export CSV files
        for x in zipped_list:
            with open('bene_' + str(zipped_list.index(x)) +'.csv','w') as f:
                writer = csv.writer(f, delimiter='|')
                writer.writerow(csvHeader)
                writer.writerows(x)

## Runs the program via run args when this file is run
if __name__ == "__main__":
    split_file(sys.argv[1])
