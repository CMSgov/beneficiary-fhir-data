import urllib3

from locust import HttpUser, task, tag

from common import config, data, errors, test_setup as setup

server_public_key = setup.loadServerPublicKey()

'''
If there is no server cert, the warnings are disabled because thousands will
appear in the logs and make it difficult to see anything else.
'''
if not server_public_key:
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

mbis = data.load_pa_mbis()
client_cert = setup.getClientCert()
setup.set_locust_env(config.load())


class BFDUser(HttpUser):
    SERVICE_DATE = 'service-date=gt2020-01-05'
    LAST_UPDATED = '_lastUpdated=gt2020-05-05'

    def _mbi(self):
        if len(mbis) == 0:
            errors.no_data_stop_test(self)
        return mbis.pop()

    def _get(self, query, name):
        self.client.get(query, cert=client_cert, verify=server_public_key, name=name)

    @tag('claim')
    @task
    def _get_claim(self):
        self._get(f'/v2/fhir/Claim?mbi={self._mbi()}', 'claim')

    @tag('claim', 'service-date')
    @task
    def _get_claim_with_service_date(self):
        self._get(f'/v2/fhir/Claim?mbi={self._mbi()}&{self.SERVICE_DATE}',
                  'claimServiceDate')

    @tag('claim', 'last-updated')
    @task
    def _get_claim_with_last_updated(self):
        self._get(f'/v2/fhir/Claim?mbi={self._mbi()}&{self.LAST_UPDATED}',
                  'claimLastUpdated')

    @tag('claim', 'service-date', 'last-updated')
    @task
    def _get_claim_with_service_date_and_last_updated(self):
        self._get(f'/v2/fhir/Claim?mbi={self._mbi()}&{self.LAST_UPDATED}&{self.SERVICE_DATE}',
                  'claimServiceDateLastUpdated')

    @tag("claim-response")
    @task
    def _get_claim_response(self):
        self._get(f'/v2/fhir/ClaimResponse?mbi={self._mbi()}',
                  'claimResponse')

    @tag('claim-response', 'service-date')
    @task
    def _get_claim_response_with_service_date(self):
        self._get(f'/v2/fhir/ClaimResponse?mbi={self._mbi()}&{self.SERVICE_DATE}',
                  'claimResponseServiceDate')

    @tag('claim-response', 'last-updated')
    @task
    def _get_claim_response_with_last_updated(self):
        self._get(f'/v2/fhir/ClaimResponse?mbi={self._mbi()}&{self.LAST_UPDATED}',
                  'claimResponseLastUpdated')

    @tag('claim-response', 'service-date', 'last-updated')
    @task
    def _get_claim_response_with_service_date_and_last_updated(self):
        self._get(f'/v2/fhir/ClaimResponse?mbi={self._mbi()}&{self.LAST_UPDATED}&{self.SERVICE_DATE}',
                  'claimResponseServiceDateLastUpdated')
