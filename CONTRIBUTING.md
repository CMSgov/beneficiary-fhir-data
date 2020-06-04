# Welcome!

We want to ensure a welcoming environment for all of our projects. Our staff follow the [18F Code of Conduct](https://github.com/18F/code-of-conduct/blob/master/code-of-conduct.md) and all contributors should do the same.

Although this is a public repo, contributing to the BFD is for CMS approved contributors only, not outside contributors.

Contributing to the BFD
---

### Background

BFD exists to enable the CMS Enterprise to drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they need to make informed decisions about their healthcare.

We provide a comprehensive, performant, and trustworthy platform to transform the way that the CMS enterprise shares and uses data by providing Coverage, Patient, and EOB FHIR-formatted data.

Review the [README](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/README.md) for additional information on the BFD. 


### Contributing changes

Contributions to the BFD are welcome from any party inside CMS.
Small changes like "good first issues" can be submitted for consideration without any additional process.

Any [substantive change must go though an RFC process](#proposing-substantive-changes) before work on the change itself can start. 

Any code changes should be properly commented and accompanied by appropriate unit test as per DASG engineering guidelines.

INSERT IMAGE OF PROCESS

**FAQ**

Q: What kind of changes don't require an RFC?

A: In general bug fixes and small changes that do not affect behavior or meaning. If you're unsure please quickly ask in the [#bfd](https://cmsgov.slack.com/archives/C010WDXAZFZ) channel or in the Scrum of Scrums meeting. If your PR involves a substantial change, it will be rejected and you will be asked to go through the RFC process.

Q: How do I know what “first issues” are up for grabs? 

A: First issues will be tracked on the BFD’s jira board and labeled (via versions) “Good First Issue”

### Proposing substantive changes

Substantive changes need to go through a design process involving the core team.
Opening an RFC provides a path for this inclusion and process.
Start an RFC by copying the file [`rfcs/0000-template.md`](rfcs/0000-template.md) to `rfcs/0000-<my idea>.md` and fill in the details. 
Open a PR using the RFC template [submit a pull request](#opening-a-pr).
The RFC will remain open for a 2 week period, at the end of which a go/no-go meeting will be held.
If approved by at least two core team members and there are no outstanding reservations, the RFC will be accepted and given a number.
Once accepted the author of the RFC and their team can scope the work within their regular process. Link or reference the RFC in the related JIRA ticket.
The core team will respond with design feedback, and the author should be prepared to revise it in response.

**FAQ**
Q: What qualifies as a substantive change?

A: There is no strict definition, however examples of substantive changes are:

1. Any change to or addition of an API endpoint (either URL or response) that is not a bug fix.
2. Changes that affect the ingestion of data into the BFD (the ETL process). 
3. Changes that significantly alter the structure of the codebase.


Q: What if I'm not sure if I need an RFC?

A: Reach out to the [#bfd](https://cmsgov.slack.com/archives/C010WDXAZFZ) channel or ask in the Scrum of Scrums meeting and see what the BFD team thinks.


Q: How should I prepare for an RFC?

A: Bring the idea to the [#bfd](https://cmsgov.slack.com/archives/C010WDXAZFZ) channel or the Scrum of Scrums meeting and talk it over with the core team.


Q: What if my RFC is not accepted?

A: It will be closed, but can be reopened if it is updated to address the items that prevented it's acceptance.


Q: What if my team doesn’t have the resources to implement our accepted RFC? 

A: Anyone can coordinate with you and the core team to take over the work. 

### Getting started

Clone this repository to get started.

```
git clone https://github.com/CMSgov/beneficiary-fhir-data.git
```

Then checkout a new branch to work on (the `-b` tells it to make a new branch).

```
git checkout -b <your username>/<your feature name>
# If you ever have to rename this branch you can do so with
# git branch -m <your username>/<your feature name> <your username>/<your new name>
```

#### Opening A PR

To contribute work back to the BFD your branch must be pushed to the `CMSgov/beneficiary-fhir-data` repository.
```
git push origin <your username>/<your feature name>
# To make sure "origin" points to CMSgov/beneficiary-fhir-data run
# git remote -v
# if a different remote points to CMSgov/beneficiary-fhir-data
# replace "origin" with that remotes name in the origional command
```
In order to obtain permission to do this contact a github administrator or reach out on [#bfd](https://cmsgov.slack.com/archives/C010WDXAZFZ).
Once pushed, [open a pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request) and use this [PR template](https://github.com/CMSgov/cms-oeda-dasg) found in .github

Please fill out each section of the body of the PR or quickly justify why the section does not apply to this particular change.
Reviewers will automatically be suggested or requested based on the projects CODEOWNERS file, feel free to add other reviewers if desired.
Once all automated checks pass and two reviewers have approved your pull request, a code owner from the core team will do a final review and make the decision to merge it in or not.

If you have any questions feel free to reach out on the [#bfd](https://cmsgov.slack.com/archives/C010WDXAZFZ) channel in CMS slack!

**FAQ**

Q: What if the core team rejects my PR?

A: The BFD core team will commit to never rejecting a PR without providing a path forward. The developer who put up the PR should review any feedback, and discuss with their product manager the scope of the work that is now outstanding.

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
