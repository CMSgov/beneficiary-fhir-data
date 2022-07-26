import sys
import psycopg2

from multiprocessing import Process

def fix_legacy_synthetic_data(args):
    """
    Gathers all clm_grp_ids for all the legacy synthetic data in a target database
    and replaces their ids such that they are in a contiguous block in a high range
    that will not be in the way for synthetic data generation.
    """
    db_string = args[0]
    # grab all unique clm_grp_ids in each problem table
    unique_id_dict = gather_unique_grp_ids_for_legacy_synthetic_data(db_string)
    # map the unique problem grp_ids to an empty upper bound area
    mapped_lists = map_grp_ids(unique_id_dict)
    # take the new maps and iterate over them doing an update from old to new on claim and claim_line tables
    replace_grp_ids(unique_id_dict, mapped_lists, db_string)

def gather_unique_grp_ids_for_legacy_synthetic_data(db_string):
    """
    Counts all the unique claim group ids for each of the problem tables and saves the ids into a dictionary
    keyed by a string containing the claim type.
    """
    print("Gathering group id lists...")
    db_items = {}
    
    db_items["carrier_grp_ids"] = _execute(db_string, "select distinct clm_grp_id from carrier_claims where bene_id < -19990000000001 and bene_id != '-88888888888888' order by clm_grp_id asc")
    db_items["inpatient_grp_ids"] = _execute(db_string, "select distinct clm_grp_id from inpatient_claims where bene_id < -19990000000001 and bene_id != '-88888888888888' order by clm_grp_id asc")
    db_items["outpatient_grp_ids"] = _execute(db_string, "select distinct clm_grp_id from outpatient_claims where bene_id < -19990000000001 and bene_id != '-88888888888888' order by clm_grp_id asc")
    db_items["pde_grp_ids"] = _execute(db_string, "select distinct clm_grp_id from partd_events where bene_id < -19990000000001 and bene_id != '-88888888888888' order by clm_grp_id asc")
    
    print("Carrier grp_ids: " + str(len(db_items["carrier_grp_ids"])))
    print("Inpatient grp_ids: " + str(len(db_items["inpatient_grp_ids"])))
    print("Outpatient grp_ids: " + str(len(db_items["outpatient_grp_ids"])))
    print("Pde grp_ids: " + str(len(db_items["pde_grp_ids"])))
    
    return db_items
    
def map_grp_ids(unique_id_dict):
    """
    Takes the dictionary of unique problem claim group ids and maps them
    to a new range starting at -99999999999.
    """
    print("Mapping group id lists to new values...")
    
    new_id = -99999999999
    mapped_lists = {}
    mapped_carrier = {}
    
    print("Mapping carrier ids...")
    for id in unique_id_dict["carrier_grp_ids"]:
        mapped_carrier[id] = new_id
        new_id = new_id + 1
    
    mapped_lists["carrier_grp_ids"] = mapped_carrier
    
    mapped_inpatient = {}
    print("Mapping inpatient ids...")
    for id in unique_id_dict["inpatient_grp_ids"]:
        mapped_inpatient[id] = new_id
        new_id = new_id + 1
    
    mapped_lists["inpatient_grp_ids"] = mapped_inpatient
    
    mapped_outpatient = {}
    print("Mapping outpatient ids...")
    for id in unique_id_dict["outpatient_grp_ids"]:
        mapped_outpatient[id] = new_id
        new_id = new_id + 1
    
    mapped_lists["outpatient_grp_ids"] = mapped_outpatient
    
    mapped_pde = {}
    print("Mapping part D ids...")
    for id in unique_id_dict["pde_grp_ids"]:
        mapped_pde[id] = new_id
        new_id = new_id + 1
    
    mapped_lists["pde_grp_ids"] = mapped_pde
    
    print(f"Set up mapped groups from range -99999999999 to {new_id}")
        
    return mapped_lists
    
def replace_grp_ids(table_ids, mapped_lists, db_string):
    """
    Takes the new group id mapping and updates each of the problem entries in each table with the
    newly remapped value. This is run as a single transaction, so no action is taken unless all queries succeed.
    """
    
    process = Process(target=replace_ids, args=(table_ids["carrier_grp_ids"], mapped_lists["carrier_grp_ids"], "carrier_claims", db_string,))
    process.start()
    
    process2 = Process(target=replace_ids, args=(table_ids["inpatient_grp_ids"], mapped_lists["inpatient_grp_ids"], "inpatient_claims", db_string,))
    process2.start()
    
    process3 = Process(target=replace_ids, args=(table_ids["outpatient_grp_ids"], mapped_lists["outpatient_grp_ids"], "outpatient_claims", db_string,))
    process3.start()
    
    process4 = Process(target=split_pde, args=(table_ids["pde_grp_ids"], mapped_lists["pde_grp_ids"], "partd_events", db_string,))
    process4.start()
    
    process.join()
    process2.join()
    process3.join()
    process4.join()
    
    print("All tasks are complete")
        
def split_pde(ids_to_replace, replacement_ids, table_name, conn):
    
    pde_threads = []
    batch_size = 10000
    ids_to_replace_split_list = [ids_to_replace[i * batch_size:(i + 1) * batch_size] for i in range((len(ids_to_replace) + batch_size - 1) // batch_size )]
    
    for i in range(len(ids_to_replace_split_list)):
        process = Process(target=replace_ids, args=(ids_to_replace_split_list[i], replacement_ids, table_name, conn,))
        pde_threads.append(process)
        process.start()
        this_list = ids_to_replace_split_list[i]
        range_start = this_list[0]
        range_end = this_list[len(this_list)-1]
        print(f"Starting pde thread {i} for ids {range_start} - {range_end}")
    
    for thread in pde_threads:
        thread.join()
    print("All pde threads complete")
        
def replace_ids(ids_to_replace, replacement_id_lookup_dict, table_name, db_string):
    conn = None
    try:
        with psycopg2.connect(db_string) as conn:
            ## everything within the "with" clause is a transaction
            with conn.cursor() as cursor:
                """
                Runs update queries for the ids for the given table_name, using the given ids_to_replace and pulling the new
                ids from the replacement_ids dictionary.
                """
                print(f"Replacing {table_name} ids...")
                numReplaced = 0
                numTotal = len(ids_to_replace)
                for old_id in ids_to_replace:
                    new_id = replacement_id_lookup_dict[old_id]
                    query = "update " + table_name + "  set clm_grp_id = " + str(new_id) + " where clm_grp_id = " + str(old_id)
                    cursor.execute(query)
                    numReplaced = numReplaced + 1
                    if numReplaced % 500 == 0:
                        print(f"{table_name} replacement queries run: {numReplaced} / {numTotal}")
                print(f"{table_name}: {numReplaced} replacement queries run")
    finally:
        conn.close()
    
def _execute(uri: str, query: str):
    """
    Execute a PSQL select statement and return its results.
    """
    conn = None
    finalResults = []
    try:
        with psycopg2.connect(uri) as conn:
            with conn.cursor() as cursor:
                cursor.execute(query)
                results = cursor.fetchall()
                for result in results:
                    finalResults.append(str(result[0]))
    finally:
        conn.close()

    return finalResults
    

## Runs the program via run args when this file is run
if __name__ == "__main__":
    """
    Takes one arg, the database string.
    """
    fix_legacy_synthetic_data(sys.argv[1:])
