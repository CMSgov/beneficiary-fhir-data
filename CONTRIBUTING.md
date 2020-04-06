# Welcome!

We want to ensure a welcoming environment for all of our projects. Our staff follow the [18F Code of Conduct](https://github.com/18F/code-of-conduct/blob/master/code-of-conduct.md) and all contributors should do the same.

We encourage you to read this project's CONTRIBUTING policy (you are here), its [LICENSE](LICENSE.md), and its [README](README.md).

Contributing to the BFD
---

### Getting started

To get started clone this repository.

```
git clone https://github.com/CMSgov/beneficiary-fhir-data.git
```

Then checkout a new branch to work on (the `-b` tells it to make a new branch).

```
git checkout -b <your username>/<your feature name>
# If you ever have to rename this branch you can do so with
# git branch -m <your username>/<your feature name> <your username>/<your new name>
```

If you have any questions feel free to reach out on the [#bfd](https://cmsgov.slack.com/archives/C010WDXAZFZ) channel in CMS slack!

### Contributing changes

Contributions to the BFD codebase are welcome from any party inside CMS.

Small changes like "good first issues" can be submitted for consideration without any additional process.

Any [substantive change must go thought an RFC process]() before work on the change itself can start.

Once your branch ready to submit for consideration to be brought into the master branch push that branch to this GitHub repository and [open a pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request).

Please fill out each section of the body of the PR or quickly justify why the section does not apply to this particular change.
Reviewers will automatically be suggested or requested based on the projects CODEOWNERS file, feel free to add other reviewers if desired.
Once all automated checks pass and two reviewers have approved your pull request a code owner from the core team will do a final review and make a decision to bring it in or not.

**FAQ**

Q: What if the core team rejects my PR?
A: The BFD core team will commit to never rejecting a PR without providing a path forward. The developer who put up the PR should review any feedback, and discuss with their product manager the scope of the work that is now outstanding.

Q: How do I know what “first issues” are up for grabs? 
A: First issues will be tracked on the BFD’s jira board and labeled (via versions) “Good First Issue”

### Proposing substantive changes

If you intend to make a substantive change to the BFD it needs to go through a design process involving the core team.
Opening an RFC provides a path for this inclusion and process.
To do that copy the file [`rfcs/0000-template.md`](rfcs/0000-template.md) to `rfcs/0000-<my idea>.md` and fill out the details. 
Once filled out submit a pull request.
The core team will respond with design feedback, and the author should be prepared to revise it in response.
The RFC will remain open for a 2 week period at the end of which a go/no-go meeting will be held.
If approved by at least two core team members approve the proposal and there are no outstanding reservations It will be accepted and merged into the master branch.
Once accepted the author of the RFC and their team can scope the work within their regular process.

**FAQ**
Q: What qualifies as a substantive change?
A: There is no strict definition however examples of substantive changes are:

1. Any change to or addition of an API endpoint

Q: How should I prepare for an RFC?
A: Bring the idea to the #bfd channel and talk it over with the core team.

Q: What if my RFC is not accepted?
A: It will be closed and can be reopened if it is updated to address the items that prevented it's acceptance.

Q: What if my team doesn’t have the resources to implement our accepted RFC? 
A: Anyone can coordinate with you and the core team to take over the work. 




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

This brings services up in the background and displays the logs from the `bfd` container. Once the logs show that that the system is started (this can take a minute or so depending on your machine) the logs can be exited with Ctrl+C.

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
