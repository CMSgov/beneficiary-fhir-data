import urllib3
import common.config as config
import common.data as data
import common.errors as errors
import common.test_setup as setup
import common.validation as validation
from locust import HttpUser, task, events

server_public_key = setup.loadServerPublicKey()
setup.disable_no_cert_warnings(server_public_key, urllib3)

eob_ids = data.load_bene_ids()
client_cert = setup.getClientCert()
setup.set_locust_env(config.load())

class BFDUser(HttpUser):
    @task
    def patient(self):
        if len(eob_ids) == 0:
            errors.no_data_stop_test(self)

        id = eob_ids.pop()
        self.client.get(f'/v2/fhir/Patient?_id={id}&_format=application%2Ffhir%2Bjson',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/Patient search by id')

'''
Adds a global failsafe check to ensure that if this test overwhelms the
database, we bail out and stop hitting the server.
'''
@events.init.add_listener
def on_locust_init(environment, **_kwargs):
    validation.setup_failsafe_event(environment, validation.SLA_PATIENT)

'''
Adds a listener that will run when the test ends which checks the various
response time percentiles against the SLA for this endpoint.
'''
@events.test_stop.add_listener
def on_locust_quit(environment, **_kwargs):
    validation.check_sla_validation(environment, validation.SLA_PATIENT)