GOALS:
- use multiple docker-files
- easy configuration of .env
- setup for synthetic data.

NOTES:
- bfd-server tests require server patch to not be present. run make unservable before trying bfd-server integration tests.

Setting up the FHIR server locally
---
Clone the repository onto you're computer.
```
git clone https://github.com/CMSgov/beneficiary-fhir-data
```

The instructions from here on should be run from the `contributing` directory in repository.

To simply run tests or execute other tasks in the BFD bring up the docker containers:
```
make up
```

This brings services up in the background and displays the logs from the `bfd` container. Once the logs show that that the system is started (this can take a minute or so depending on your machine) the logs can be exited with Ctrl+C (Cmd+C on a mac).

Now the system can be interacted with. Here's an example of running tests for the `bfd-server` module.
```
docker-compose exec bfd bash
cd /app/apps/bfd-server
mvn verify
```

### Serving the BFD

To run the BFD locally in a way that will allow you and other systems to interact with it some modifications need to be made so that it serves on a consistent port. These changes are contained in the file `contributing/patches/allow_local_port_config.patch` and can be applied with `make servable`. To undo the changes run `make unservable`.

Since this changes the code in the repository please keep in mind not to commit these changes and to be aware of them while making your own changes.

Once the changes are applied the server needs to be started in order for them to take effect.
Run `make up` if no docker containers are running or `make restart` if they're already running.

The FHIR server should now be reachable from the browser at https://localhost:1337. In order for the FHIR server to trust your browser and return data, the client certificate at `apps/bfd-server/dev/ssl-stores/client-trusted-keystore.pfx` needs to be imported into the browser. In Chrome this can be done at `chrome://settings/certificates`. In Firefox it can be done at `about:preferences#privacy`, there is a button at the bottom called "View Certificates" that should give the option to import one.

(4) Created a directory in bb.dev/../synthetic-data for synthetic data and pulled the synthetic data using the script attached to this ticket.

(5) Applied git patches.  From the beneficiary-fhir-data directory, executed: `git apply ../bb.dev/patches/*`

(6) In bb.dev, copied .env.example to .env and commented out `BB20_CONTEXT`

(7) In bb.dev, ran `docker-compose up -d bfd` and followed the logs using `docker-compose logs -f | grep bfd_1`

(8) Ran `docker-compose exec bfd make load`

(9) Import the client certificate (located here: https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-server/dev/ssl-stores/client-trusted-keystore.pfx) into Chrome.  This is required for requesting patient data.

(10) Request patient data: [https://localhost:1337/v1/fhir/Patient/-19990000000001?_format=json

Note: server cert is not accurate so connection will not be secure (on purpose).



Integration with a downstream system
---

Activities performed to integrate BCDA with BFD environment that is running locally:

(1) Updated BB_SERVER_LOCATION to reference the URL of the BFD application that is running locally.  The env var is referenced in docker-compose.yml (https://github.com/CMSgov/bcda-app/blob/master/docker-compose.yml) but specified in encrypted local.env (https://github.com/CMSgov/bcda-app/blob/master/shared_files/encrypted/local.env).  To referenced the local BFD server running in docker-compose stack, the BB_SERVER_LOCATION was set to `https://host.docker.internal:1337`.

(2) The BFD stack exposes port 5433 for the BFD's DB.  BCDA also exposes that DB port.  To eliminate port clashing, the BCDA docker-compose.yml was updated (locally) to swap out any reference of 5433 and replace with 5434.

(3) In order to ensure BCDA is using the correct client cert and client key to interact with BFD.  To do this, created a directory in BCDA's shared_files directory called bb.dev.  Then, copied the files from bb.dev/certstore to shared_files/bb.dev.  

(4) Updated the BCDA docker-compose.yml file to update the env vars for BB_CLIENT_CERT_FILE and BB_CLIENT_KEY_FILE to point to the files in shared_files/bb.dev

(5) Updated BB_HASH_ITER to 2.  The env var is referenced in docker-compose.yml (https://github.com/CMSgov/bcda-app/blob/master/docker-compose.yml) but specified in encrypted local.env (https://github.com/CMSgov/bcda-app/blob/master/shared_files/encrypted/local.env).  

(6) Updated BB_HASH_PEPPER to `6E6F747468657265616C706570706572`.  The env var is referenced in docker-compose.yml (https://github.com/CMSgov/bcda-app/blob/master/docker-compose.yml) but specified in encrypted local.env (https://github.com/CMSgov/bcda-app/blob/master/shared_files/encrypted/local.env).  

