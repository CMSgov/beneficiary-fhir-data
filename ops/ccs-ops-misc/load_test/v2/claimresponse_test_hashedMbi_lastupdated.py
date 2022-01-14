import urllib3
import common.config as config
import common.data as data
import common.errors as errors
import common.test_setup as setup
from locust import HttpUser, task

server_public_key = setup.loadServerPublicKey()

'''
If there is no server cert, the warnings are disabled because thousands will appear in the logs and make it difficult
to see anything else.
'''
if not server_public_key:
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

mbis = data.load_pa_mbis()
client_cert = setup.getClientCert()
setup.set_locust_env(config.load())


class BFDUser(HttpUser):
    @task
    def patient(self):
        if len(mbis) == 0:
            errors.no_data_stop_test(self)

        hashed_mbi = mbis.pop()
        self.client.get(f'/v2/fhir/ClaimResponse?mbi={hashed_mbi}&_lastUpdated=gt2020-05-05',
                        cert=client_cert,
                        verify=server_public_key,
                        name='/v2/fhir/Claim')
