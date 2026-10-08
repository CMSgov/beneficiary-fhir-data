import time

import requests
import urllib3
from urllib3.exceptions import NewConnectionError


def wait_for_matchbox():
    print("Waiting for local matchbox server to start...")
    while True:
        try:
            response = requests.get("http://localhost:18080/matchboxv3/actuator/health", timeout=5)
            response.raise_for_status()
            return
        except requests.ConnectionError:
            print("Connection refused. Retrying...")
        except requests.RequestException as ex:
            print("Matchbox not ready. Retrying...", ex)
        time.sleep(5)


if __name__ == "__main__":
    wait_for_matchbox()
