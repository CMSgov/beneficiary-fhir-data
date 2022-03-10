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
last_updated = data.get_last_updated()

class BFDUser(HttpUser):
    @task
    def explanation_of_benefit(self):
        if len(eob_ids) == 0:
            errors.no_data_stop_test(self)

        id = eob_ids.pop()
        self.client.get(f'/v2/fhir/ExplanationOfBenefit?_IncludeTaxNumbers=true&patient={id}&_format=application%2Ffhir%2Bjson&_lastUpdated=gt{last_updated}',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/ExplanationOfBenefit search by id / lastUpdated (2 weeks) / _IncludeTaxNumbers=true')

'''
Adds a global failsafe check to ensure that if this test overwhelms the
database, we bail out and stop hitting the server.
'''
@events.init.add_listener
def on_locust_init(environment, **_kwargs):
    validation.setup_failsafe_event(environment, validation.SLA_EOB_WITHOUT_SINCE)

'''
Adds a listener that will run when the test ends which checks the various
response time percentiles against the SLA for this endpoint.
'''
@events.test_stop.add_listener
def on_locust_quit(environment, **_kwargs):
    validation.check_sla_validation(environment, validation.SLA_EOB_WITHOUT_SINCE)