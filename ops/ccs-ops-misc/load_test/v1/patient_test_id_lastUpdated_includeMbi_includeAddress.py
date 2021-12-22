import urllib3
import common.config as config
import common.data as data
import common.errors as errors
import common.test_setup as setup
from locust import HttpUser, task

server_public_key = setup.loadServerPublicKey()
setup.disable_no_cert_warnings(server_public_key, urllib3)

bene_ids = data.load_bene_ids()
client_cert = setup.getClientCert()
setup.set_locust_env(config.load())
last_updated = data.get_last_updated()

class BFDUser(HttpUser):
    @task
    def patient_by_id(self):
        if len(bene_ids) == 0:
            errors.no_data_stop_test(self)

        id = bene_ids.pop()
        self.client.get(f'/v1/fhir/Patient?_id={id}&_lastUpdated=gt{last_updated}&_IncludeIdentifiers=mbi&_IncludeTaxNumbers=true',
                cert=client_cert,
                verify=server_public_key,
                name='/v1/fhir/Patient/{id} search by id / lastUpdated (2 weeks) / includeTaxNumbers = true / includeIdentifiers = mbi')

