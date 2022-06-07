import pandas as pd
import warnings
import csv
import sys
from multiprocessing import Process

warnings.filterwarnings("ignore")

def split_file(filename):
    '''Splits beneficiary-interim RIF file to n number of RIF files 
     with only one beneficiary record per bene_id in each file.
    '''

    print("Reading csv...")
    with open(filename) as infile:
        col = 'bene_id' # RIF file CSV column
        dfCol = 'BENE_ID' # column in data frame
        
        # load RIF data into dataframe 
        csv_dataframe = pd.read_csv(infile, sep='|', dtype=str, keep_default_na = False)
        print("File rows/columns: " + str(csv_dataframe.shape))
        
        years = ['2013', '2014', '2015', '2016', '2017', '2018', '2019', '2020', '2021']
        
        for i in range(len(years)):
            year = years[i]
            process = Process(target=write_split_file, args=(csv_dataframe,year,))
            process.start()
        

def write_split_file(csv_dataframe, year):
    
    query = '`RFRNC_YR` == "' + year + '"';
    fileName = 'bene_' + year + '.csv'

    query_result_dataframe = csv_dataframe.query(query)
    print(f"#{year} query results: " + str(query_result_dataframe.shape))
    query_result_dataframe.to_csv(fileName, sep='|', index=False)
    print(f"Wrote file for bene_{year}")
    

## Runs the program via run args when this file is run
if __name__ == "__main__":
    split_file(sys.argv[1])
