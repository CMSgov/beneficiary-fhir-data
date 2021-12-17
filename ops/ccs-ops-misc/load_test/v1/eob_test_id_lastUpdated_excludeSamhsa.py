import urllib3
import common.config as config
import common.data as data
import common.errors as errors
import common.test_setup as setup
from locust import HttpUser, task

server_public_key = setup.loadServerPublicKey()
setup.disable_no_cert_warnings(server_public_key, urllib3)

eob_ids = data.load_bene_ids()
client_cert = setup.getClientCert()
setup.set_locust_env(config.load())
last_updated = data.get_last_updated()

class BFDUser(HttpUser):
    @task
    def explanation_of_benefit(self):
        if len(eob_ids) == 0:
            errors.no_data_stop_test(self)

        id = eob_ids.pop()
        response = self.client.get(f'/v1/fhir/ExplanationOfBenefit?_lastUpdated=gt{last_updated}&excludeSAMHSA=true&patient={id}&_format=json',
                cert=client_cert,
                verify=server_public_key,
                name='/v1/fhir/ExplanationOfBenefit search by lastUpdated excludeSAMHSA=true patient={id}')
    

