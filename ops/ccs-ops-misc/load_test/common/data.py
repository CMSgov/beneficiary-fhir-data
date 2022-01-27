import os
import datetime

from . import config, db, read_contract_cursors as cursors, test_setup as setup

'''
Gets the cursor data and either returns all the data in a list if not a distributed test,
or takes a percentage of the data to distribute to the current worker thread. The percentage
of the data in distributed mode depends on the total number of workers and the index of the
data is dependant on which worker index calls this method.
'''
def load_cursors(version):
    if setup.is_master_thread():
        ## Don't bother loading data for the master thread, it doenst run a test
        return
    elif setup.is_worker_thread():
        worker_number = str(os.environ['LOCUST_WORKER_NUM'])
        num_workers = os.environ['LOCUST_NUM_WORKERS']
        print(f"Worker {worker_number} starting...")
        full_cursor_list = cursors.load_data(version)
        data_per_user = len(full_cursor_list) // int(num_workers)
        start_index = int(worker_number) * data_per_user
        end_index = start_index + data_per_user - 1
        print(f"Worker {worker_number} using data from indexes {start_index} to {end_index}")
        return full_cursor_list[start_index:end_index]
    else:
        return cursors.load_data(version)

'''
Gets the bene id data and either returns all the data in a list if not a distributed test,
or takes a percentage of the data to distribute to the current worker thread. The percentage
of the data in distributed mode depends on the total number of workers and the index of the
data is dependant on which worker index calls this method.
'''
def load_bene_ids():
    if setup.is_master_thread():
        ## Don't bother loading data for the master thread, it doenst run a test
        return
    elif setup.is_worker_thread():
            worker_number = str(os.environ['LOCUST_WORKER_NUM'])
            num_workers = os.environ['LOCUST_NUM_WORKERS']
            print(f"Worker {worker_number} starting...")
            configFile = config.load()
            full_eob_list = db.get_bene_ids(configFile['dbUri'])
            data_per_user = len(full_eob_list) // int(num_workers)
            start_index = int(worker_number) * data_per_user
            end_index = start_index + data_per_user - 1
            print(f"Worker {worker_number} using data from indexes {start_index} to {end_index}")
            return full_eob_list[start_index:end_index]
    else:
        configFile = config.load()
        return db.get_bene_ids(configFile['dbUri'])

'''
Gets the hashed mbi data and either returns all the data in a list if not a distributed test,
or takes a percentage of the data to distribute to the current worker thread. The percentage
of the data in distributed mode depends on the total number of workers and the index of the
data is dependant on which worker index calls this method.
'''
def load_mbis():
    if setup.is_master_thread():
        ## Don't bother loading data for the master thread, it doenst run a test
        return
    elif setup.is_worker_thread():
        worker_number = str(os.environ['LOCUST_WORKER_NUM'])
        num_workers = os.environ['LOCUST_NUM_WORKERS']
        print(f"Worker {worker_number} starting...")
        configFile = config.load()
        full_mbi_list = db.get_hashed_mbis(configFile['dbUri'])
        data_per_user = len(full_mbi_list) // int(num_workers)
        start_index = int(worker_number) * data_per_user
        end_index = start_index + data_per_user - 1
        print(f"Worker {worker_number} using data from indexes {start_index} to {end_index}")
        return full_mbi_list[start_index:end_index]
    else:
        configFile = config.load()
        return db.get_hashed_mbis(configFile['dbUri'])

'''
Gets the hashed partially adjudicated mbi data and either returns all the data in a list if
not a distributed test, or takes a percentage of the data to distribute to the current worker
thread. The percentage of the data in distributed mode depends on the total number of workers
and the index of the data is dependant on which worker index calls this method.
'''
def load_pa_mbis():
    if setup.is_master_thread():
        ## Don't bother loading data for the master thread, it doenst run a test
        return
    elif setup.is_worker_thread():
        worker_number = str(os.environ['LOCUST_WORKER_NUM'])
        num_workers = os.environ['LOCUST_NUM_WORKERS']
        print(f"Worker {worker_number} starting...")
        configFile = config.load()
        full_mbi_list = db.get_partially_adj_hashed_mbis(configFile['dbUri'])
        data_per_user = len(full_mbi_list) // int(num_workers)
        start_index = int(worker_number) * data_per_user
        end_index = start_index + data_per_user - 1
        print(f"Worker {worker_number} using data from indexes {start_index} to {end_index}")
        return full_mbi_list[start_index:end_index]
    else:
        configFile = config.load()
        return db.get_partially_adj_hashed_mbis(configFile['dbUri'])


'''
Gets a sample last_updated field for testing.

Uses a date two weeks before when the script is run.
'''
def get_last_updated():
    today = datetime.datetime.now()
    delta = datetime.timedelta(weeks = 2)
    prior_date = today - delta
    return prior_date.strftime('%Y-%m-%d')
