# Welcome!

We want to ensure a welcoming environment for all of our projects. Our staff follow the [18F Code of Conduct](https://github.com/18F/code-of-conduct/blob/master/code-of-conduct.md) and all contributors should do the same.

We encourage you to read this project's CONTRIBUTING policy (you are here), its [LICENSE](LICENSE.md), and its [README](README.md).

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

To run the BFD locally in a way that will allow you and other systems to interact with it some modifications need to be made so that it serves on a consistent port. These changes are contained in the file `contributing/patches/allow_local_port_config.patch` and can be applied with 

```
make servable
```
To undo the changes run `make unservable`.

Since this changes the code in the repository please keep in mind not to commit these changes and to be aware of them while making your own changes.

Once the changes are applied the server needs to be started in order for them to take effect.
Run `make up` if no docker containers are running or

```
make restart
```

if they're already running.

The FHIR server should now be reachable from the browser at https://localhost:1337. In order for the FHIR server to trust your browser and return data, the client certificate at `apps/bfd-server/dev/ssl-stores/client-trusted-keystore.pfx` needs to be imported into the browser. In Chrome this can be done at `chrome://settings/certificates`. In Firefox it can be done at `about:preferences#privacy`, there is a button at the bottom called "View Certificates" that should give the option to import one.

### Loading data to work with

To load some data for the BFD to return first apply the patches that allow the system to load local data: 
```make loadable```
Then load the data 
```make load```
This can take as long as an hour depending on your system.

Once loaded going to a URL like [https://localhost:1337/v1/fhir/Patient/-19990000000001?_format=json](https://localhost:1337/v1/fhir/Patient/-19990000000001?_format=json) in your browser should show you some data.


Integration with a downstream system
---

An example of connecting http://github.com/cmsgov/bluebutton-web-server to the local BFD.

`.env`
```
BB20_CONTEXT=../../bluebutton-web-server
CERTSTORE=../../bb.dev/certstore
BFD_DIR=../
SYNTHETIC_DATA=./synthetic-data
```

`docker-compose.bb2.yml`

```
version: '3.3'

services:
  bbdb:
    image: postgres
    environment:
      - POSTGRES_DB=bluebutton
      - POSTGRES_PASSWORD=toor
  bb20:
    build:
      context: ${BB20_CONTEXT}
      dockerfile: Dockerfile
    command: python3 manage.py runserver 0.0.0.0:8000
    environment:
      - DJANGO_SETTINGS_MODULE=hhs_oauth_server.settings.dev
      - DJANGO_FHIR_CERTSTORE=/certstore
      - DATABASES_CUSTOM=postgres://postgres:toor@bbdb:5432/bluebutton
      - OAUTHLIB_INSECURE_TRANSPORT=true
      - DJANGO_DEFAULT_SAMPLE_FHIR_ID="20140000008325"
      - DJANGO_SECURE_SESSION=False
      - DJANGO_MEDICARE_LOGIN_URI=http://127.0.0.1:8080?scope=openid%20profile&client_id=bluebutton
      - DJANGO_SLS_USERINFO_ENDPOINT=http://msls:8080/userinfo
      - DJANGO_SLS_TOKEN_ENDPOINT=http://msls:8080/token
      - FHIR_URL=https://bfd.local:9954/v1/fhir/
    ports:
      - "8000:8000"
    links:
      - "bfd:bfd.local"
    volumes:
      - ${BB20_CONTEXT}:/code
      - ${CERTSTORE}:/certstore
    depends_on:
      - bbdb
```

The BlueButton 2 system also requires an Identity Provider
`docker-compose.msls.yml`
```
version: '3.3'

services:

  msls:
    build:
      context: ../../bb.dev/msls
      dockerfile: Dockerfile
    command: msls
    ports: 
      - "8080:8080"
```

Bringing these systems up together:

```
docker-compose -f docker-compose.yml -f docker-compose.bb2.yml -f docker-compose.msls.yml up -d
```

```
docker-compose -f docker-compose.bb2.yml exec bb2 ./docker-compose/migrate.sh
```
