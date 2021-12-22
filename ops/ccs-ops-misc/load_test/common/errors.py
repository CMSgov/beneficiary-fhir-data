import os

'''
Logs a statement that the test has no more data, and then
shuts down the user.
'''
def no_data_stop_test(userInstance):
    if 'LOCUST_WORKER_NUM' in os.environ:
        num = str(os.environ['LOCUST_WORKER_NUM'])
        print(f"Worker {num} ran out of data.")
    else:
        print("Ran out of data, stopping test...")
    userInstance.environment.runner.quit()
