import pandas as pd
import warnings
import csv

warnings.filterwarnings("ignore")

# RIF file CSV column
col = 'bene_id'
#column in data frame
dfCol = 'BENE_ID'

# load RIF data into dataframe 
df = pd.read_csv('<PATH TO BENEFICIARY INTERIM FILE>', sep='|', keep_default_na=False)
df.head()

# CSV file header
csvHeader = (list(df.columns.values))

# Creating a list of lists of records for each bene_id
idList= (df[dfCol].unique().tolist())
beneRows = []
for id in idList:
    beneRows.append(pd.DataFrame(df.loc[df['BENE_ID'] == id]).values.tolist())

# Create n number of lists with a unique record for a given bene_id
zipped_list = list(zip(*beneRows))

# export CSV files
for x in zipped_list:
    with open('bene_' + str(zipped_list.index(x)) +'.csv','w') as f:
        writer = csv.writer(f, delimiter='|')
        writer.writerow(csvHeader)
        writer.writerows(x)


