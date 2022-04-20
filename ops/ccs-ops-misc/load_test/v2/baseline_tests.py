import urllib3
import common.config as config
import common.data as data
import common.errors as errors
import common.test_setup as setup
import common.validation as validation
from locust import HttpUser, task, events, tag

server_public_key = setup.loadServerPublicKey()
setup.disable_no_cert_warnings(server_public_key, urllib3)

'''
If there is no server cert, the warnings are disabled because thousands will
appear in the logs and make it difficult to see anything else.
'''
if not server_public_key:
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# mbis = data.load_mbis()
eob_ids = data.load_bene_ids()
client_cert = setup.getClientCert()
server_public_key = setup.loadServerPublicKey()
last_updated = data.get_last_updated()
cursor_list = data.load_cursors("v2")

setup.set_locust_env(config.load())

class BFDUser(HttpUser):
    def __init__(self, *args, **kwargs):
        self.eob_ids = eob_ids.copy()
        super().__init__(*args, **kwargs)

    @task
    def coverage_test_id_count(self):
        if len(self.eob_ids) == 0:
            errors.no_data_stop_test(self)

        id = self.eob_ids.pop()
        self.client.get(f'/v2/fhir/Coverage?beneficiary={id}&_count=10',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/Coverage search by id / count=10')

    @task
    def coverage_test_id_lastUpdated(self):
        if len(eob_ids) == 0:
            errors.no_data_stop_test(self)

        id = eob_ids.pop()
        self.client.get(f'/v2/fhir/Coverage?_lastUpdated=gt{last_updated}&beneficiary={id}',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/Coverage search by id / lastUpdated (2 weeks)')
    
    @task
    def coverage_test_id(self):
        if len(self.eob_ids) == 0:
            errors.no_data_stop_test(self)

        id = self.eob_ids.pop()
        self.client.get(f'/v2/fhir/Coverage?beneficiary={id}',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/Coverage search by id')

    @task
    def eob_test_id_count(self):
        if len(self.eob_ids) == 0:
            errors.no_data_stop_test(self)

        id = self.eob_ids.pop()
        self.client.get(f'/v2/fhir/ExplanationOfBenefit?patient={id}&_count=10&_format=application%2Ffhir%2Bjson',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/ExplanationOfBenefit search by id / count=10')
    
    @task
    def eob_test_id_includeTaxNumber(self):
        if len(self.eob_ids) == 0:
            errors.no_data_stop_test(self)

        id = self.eob_ids.pop()
        self.client.get(f'/v1/fhir/ExplanationOfBenefit?_lastUpdated=gt{last_updated}&patient={id}&_IncludeTaxNumbers=true&_format=json',
                cert=client_cert,
                verify=server_public_key,
                name='/v1/fhir/ExplanationOfBenefit search by id / lastUpdated / includeTaxNumbers = true')

    @task
    def eob_test_id(self):
        if len(self.eob_ids) == 0:
            errors.no_data_stop_test(self)

        id = self.eob_ids.pop()
        self.client.get(f'/v2/fhir/ExplanationOfBenefit?patient={id}&_format=application%2Ffhir%2Bjson',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/ExplanationOfBenefit search by id')
  
    # @task
    # def patient_test_coverageContract(self):
    #     if len(cursor_list) == 0:
    #         errors.no_data_stop_test(self)

    #     cursor_url = cursor_list.pop()

    #     response = self.client.get(cursor_url,
    #             cert=client_cert,
    #             verify=server_public_key,
    #             headers={"IncludeIdentifiers": "mbi"},
    #             name='/v2/fhir/Patient search by coverage contract (all pages)')
    
    # @task
    # def patient_test_hashedMbi(self):
    #     if len(mbis) == 0:
    #         errors.no_data_stop_test(self)

    #     hashed_mbi = mbis.pop()
    #     self.client.get(f'/v2/fhir/Patient?identifier=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fidentifier%2Fmbi-hash%7C{hashed_mbi}&_IncludeIdentifiers=mbi',
    #             cert=client_cert,
    #             verify=server_public_key,
    #             name='/v2/fhir/Patient search by hashed mbi / _IncludeIdentifiers=mbi')

    @task
    def patient_test_id_lastUpdated(self):
        if len(self.eob_ids) == 0:
            errors.no_data_stop_test(self)

        id = self.eob_ids.pop()
        self.client.get(f'/v2/fhir/Patient?_id={id}&_format=application%2Ffhir%2Bjson&_IncludeIdentifiers=mbi&_lastUpdated=gt{last_updated}',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/Patient search by id / _IncludeIdentifiers=mbi / last updated (2 weeks)')

    @task
    def patient_test_id(self):
        if len(self.eob_ids) == 0:
            errors.no_data_stop_test(self)

        id = self.eob_ids.pop()
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
    validation.setup_failsafe_event(environment, validation.SLA_COVERAGE)

'''
Adds a listener that will run when the test ends which checks the various
response time percentiles against the SLA for this endpoint.
'''
@events.test_stop.add_listener
def on_locust_quit(environment, **_kwargs):
    validation.check_sla_validation(environment, validation.SLA_COVERAGE)