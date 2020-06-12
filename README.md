MyMedicare.gov BlueButton Parent POM
====================================

## About

Beneficiary FHIR Data (BFD) Server: The BFD Server is an internal backend system used at CMS to represent Medicare beneficiaries' demographic, enrollment, and claims data in [FHIR](https://www.hl7.org/fhir/overview.html) format.

### DASG Mission
Drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they need to make informed decisions about their healthcare. 

### BFD Mission
Enable the CMS Enterprise to drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they need to make informed decisions about their healthcare. 

### BFD Vision
Provide a comprehensive, performant, and trustworthy platform to transform the way that the CMS enterprise shares and uses data. 

## Releases

This project uses Maven's [maven-release-plugin](http://maven.apache.org/maven-release/maven-release-plugin/) for releases, and must be manually released by a developer with permissions to [its GitHub repo](https://github.com/HHSIDEAlab/bluebutton-parent-pom) and to [OSSRH](http://central.sonatype.org/pages/ossrh-guide.html) (which is used to ensure its releases land in Maven Central).

Run the following commands to perform a release:

    $ mvn release:prepare release:perform
    $ git push --all && git push --tags

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.

Setting up the FHIR server locally
---
Requirements: Docker 

Caution: Setting up your local environments requires git patches to run. Please make sure you `make unservable` and `make unloadadable` before you commit your changes to revert these patches. 

Let's begin!

Clone the repository onto you're computer.
```
git clone https://github.com/CMSgov/beneficiary-fhir-data
```

The instructions from here on should be run from the `contributing` directory in repository.

To simply run tests or execute other tasks in the BFD bring up the docker containers.
Note: As a prerequisite, the bfd Docker environments need a few variables to be set in a file named .env placed within the /contributing directory.

- `BFD_DIR` specifies the directory on your host machine where you have cloned https://github.com/CMSgov/beneficiary-fhir-data
- (optional) `SYNTHETIC_DATA` specifies a folder where you have the full set of synthetic rif files.
- (defaults to `/app`) `BFD_MOUNT_POINT` the path within the service container where the beneficiary-fhir-data directory will be mounted.

Here's an example `.env` file that docker-compose could use:
```
BFD_DIR=../../beneficiary-fhir-data
SYNTHETIC_DATA=../../synthetic-data
```

```
make up
```

This brings services up in the background and displays the logs from the `bfd` container. Once the logs show that that the system is started (this can take a minute or so depending on your machine) the logs can be exited with Ctrl+C.

Now the system can be interacted with. Here's an example of running tests for the `bfd-server` module.

```
docker-compose exec bfd bash
cd /app/apps/bfd-server
mvn verify
```

### Serving the BFD

To run the BFD locally in a way that will allow you and other systems to interact with it some modifications need to be made so that it serves on a consistent port. Caution: Since this changes the code in the repository (server-start.sh) please keep in mind not to commit these changes.

These changes are contained in the file `contributing/patches/allow_local_port_config.patch` and can be applied with 

```
make servable
```
To undo the changes run `make unservable`.

Once the changes are applied the server needs to be started in order for them to take effect.
Run `make up` if no docker containers are running or

```
make restart
```

if they're already running.

The FHIR server should now be reachable from the browser at https://localhost:1337. In order for the FHIR server to trust your browser and return data, the client certificate at `apps/bfd-server/dev/ssl-stores/client-trusted-keystore.pfx` needs to be imported into the browser. The cert password is 'changeit'. 

In Chrome this can be done at `chrome://settings/certificates`. In Firefox it can be done at `about:preferences#privacy`, there is a button at the bottom called "View Certificates" that should give the option to import one. 
Note MacOS Users: To make this cert available to Chrome or Firefox you'll need to add this cert to the Keychain application. 

### Loading data to work with

First you'll want some synthetic data to load and work with. To fetch the synthetic data from a public S3 bucket:
```
make synthetic-data/*.rif
```

Tip: This will download to a folder within the contributing folder within the repo. Consider moving this synthetic data outside the repo and updating your .env file to point to this new location. It will save you some steps in the future. 

To load some data for the BFD to return first apply the patches that allow the system to load local data: 

Caution: Since this changes the code in the repository please keep in mind not to commit these changes and to be aware of them while making your own changes. Reverse them before you submit your changes. 

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
Security
--
We work with sensitive information: do not put any PHI or PII in the public repo for BFD.

If you believe you’ve found or been made aware of a security vulnerability, please refer to the CMS Vulnerability Disclosure Policy (here is a [link](https://www.cms.gov/Research-Statistics-Data-and-Systems/CMS-Information-Technology/CIO-Directives-and-Policies/Downloads/CMS-Vulnerability-Disclosure-Policy.pdf) to the most recent version as of the time of this commit.
