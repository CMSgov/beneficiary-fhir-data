import urllib3
import common.test_setup as setup
import common.config as config
import common.data as data
import common.errors as errors
import common.validation as validation
from locust import HttpUser, task, events

server_public_key = setup.loadServerPublicKey()
setup.disable_no_cert_warnings(server_public_key, urllib3)

## Read cursors from the file
cursor_list = data.load_cursors("v1")
client_cert = setup.getClientCert()
setup.set_locust_env(config.load())

class BFDUser(HttpUser):
    @task
    def batch_patient_by_contract(self):
        if len(cursor_list) == 0:
            errors.no_data_stop_test(self)

        cursor_url = cursor_list.pop()
        self.client.get(cursor_url,
                cert=client_cert,
                verify=server_public_key,
                headers={"IncludeIdentifiers": "mbi"},
                name='/v1/fhir/Patient search by coverage contract (all pages)')

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

