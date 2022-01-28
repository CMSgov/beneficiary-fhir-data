import urllib3
import common.config as config
import common.data as data
import common.errors as errors
import common.test_setup as setup
from locust import HttpUser, task, tag

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
    SERVICE_DATE = 'gt2020-01-05'
    LAST_UPDATED = 'gt2020-05-05'

    def hashed_mbi(self):
        if len(mbis) == 0:
            errors.no_data_stop_test(self)
        return mbis.pop()

    def base_query(self, query, name):
        self.client.get(query, cert=client_cert, verify=server_public_key, name=name)

    @tag('claim')
    @task
    def get_claim(self):
        self.base_query(f'/v2/fhir/Claim?mbi={self.hashed_mbi()}', 'claim')

    @tag('claim', 'service-date')
    @task
    def get_claim_with_service_date(self):
        self.base_query(f'/v2/fhir/Claim?mbi={self.hashed_mbi()}&service-date={self.SERVICE_DATE}', 'claimServiceDate')

    @tag('claim', 'last-updated')
    @task
    def get_claim_with_last_updated(self):
        self.base_query(f'/v2/fhir/Claim?mbi={self.hashed_mbi()}&_lastUpdated={self.LAST_UPDATED}', 'claimLastUpdated')

    @tag('claim', 'service-date', 'last-updated')
    @task
    def get_claim_with_service_date_and_last_updated(self):
        self.base_query(f'/v2/fhir/Claim?mbi={self.hashed_mbi()}&_lastUpdated={self.LAST_UPDATED}&service-date={self.SERVICE_DATE}', 'claimServiceDateLastUpdated')

    @tag("claim-response")
    @task
    def get_claim_response(self):
        self.base_query(f'/v2/fhir/ClaimResponse?mbi={self.hashed_mbi()}', 'claimResponse')

    @tag('claim-response', 'service-date')
    @task
    def get_claim_response_with_service_date(self):
        self.base_query(f'/v2/fhir/ClaimResponse?mbi={self.hashed_mbi()}&service-date={self.SERVICE_DATE}', 'claimResponseServiceDate')

    @tag('claim-response', 'last-updated')
    @task
    def get_claim_response_with_last_updated(self):
        self.base_query(f'/v2/fhir/ClaimResponse?mbi={self.hashed_mbi()}&_lastUpdated={self.LAST_UPDATED}', 'claimResponseLastUpdated')

    @tag('claim-response', 'service-date', 'last-updated')
    @task
    def get_claim_response_with_service_date_and_last_updated(self):
        self.base_query(f'/v2/fhir/ClaimResponse?mbi={self.hashed_mbi()}&_lastUpdated={self.LAST_UPDATED}&service-date={self.SERVICE_DATE}', 'claimResponseServiceDateLastUpdated')
