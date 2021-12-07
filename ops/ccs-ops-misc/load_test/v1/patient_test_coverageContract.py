import urllib3
import common.test_setup as setup
import common.config as config
import common.read_contract_cursors as cursors
import locust.exception as locust_exception
from locust import HttpUser, task

server_public_key = setup.loadServerPublicKey()

'''
If there is no server cert, the warnings are disabled because thousands will appear in the logs and make it difficult
to see anything else.
'''
if not server_public_key:
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

## Read cursors from the file
cursor_list = cursors.loaddata("v1")
client_cert = setup.getClientCert()
setup.set_locust_env(config.load())

class BFDUser(HttpUser):
    @task
    def batch_patient_by_contract(self):
        if len(cursor_list) == 0:
            print("Ran out of data, stopping test...")
            raise locust_exception.StopUser()

        cursor_url = cursor_list.pop()
        self.client.get(cursor_url,
                cert=client_cert,
                verify=server_public_key,
                headers={"IncludeIdentifiers": "mbi"},
                name='/v1/fhir/Patient search by coverage contract (all pages)')

