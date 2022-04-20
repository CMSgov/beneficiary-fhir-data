import time
from urllib.parse import urlencode

'''
Creates a query path from a base path (i.e. /v2/fhir/Coverage) and a dictionary of query parameters
'''
def create_url_path(path: str, query_params: dict[str, str]):
  params = urlencode(query_params)
  return "{}?{}".format(path, params)