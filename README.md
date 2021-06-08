# Beneficiary FHIR Data Server (BFD)
====================================

## About

Beneficiary FHIR Data (BFD) Server: The BFD Server is an internal backend system used at CMS to represent Medicare beneficiaries' demographic, enrollment, and claims data in [FHIR](https://www.hl7.org/fhir/overview.html) format.

### DASG Mission
Drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they need to make informed decisions about their healthcare.

### BFD Mission
Enable the CMS Enterprise to drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they need to make informed decisions about their healthcare.

### BFD Vision
Provide a comprehensive, performant, and trustworthy platform to transform the way that the CMS enterprise shares and uses data.

### License

This project is in the worldwide [public domain](LICENSE.md). As stated in [LICENSE](LICENSE.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.

## BFD User Documentation

The following provide information on how to use BFD:

* [Request Audit Headers](./docs/request-audit-headers.md):
  This document details the HTTP headers that should be included when calling BFD,
    to ensure that proper audit information is available to the BFD team.
* [Request Options](./docs/request-options.md):
  This document details the request options that can be used when calling BFD.

## BFD Developer Documentation

The following provide information on how to develop BFD:

* [Sample Data Sets](./apps/bfd-model/bfd-model-rif-samples/dev/design-sample-data-sets.md):
  This document details the various sample/test data sets available for use with the Blue Button Data/backend systems.

## Development Environment Setup

### AWS Credentials

Many of the automated tests associated with the Blue Button framework use AWS resources.  Before running a build using Maven or importing projects into your Eclipse IDE, which will run a build automatically, please ensure the appropriate accounts and credentials are configured within your environment.  **This is necessary to prevent incurring unwanted charges on the wrong AWS account**.

Below are links to detailed instructions on configuring your AWS credentials for your environment:

  * [Configuration and Credential Files](http://docs.aws.amazon.com/cli/latest/userguide/cli-config-files.html)

### Github Configuration

You will need to configure an SSH credential in order to clone the Blue Button repositories.  Instructions are thoroughly documented on Github but for convenience here are the relevant links:

  * [Connecting to Github with SSH](https://help.github.com/articles/connecting-to-github-with-ssh/)
  * [Generating a new SSH key and adding it to the ssh-agent](https://help.github.com/articles/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent/)
  * [Adding a new SSH key to your GitHub account](https://help.github.com/articles/adding-a-new-ssh-key-to-your-github-account/)
  * [Testing your SSH connection](https://help.github.com/articles/testing-your-ssh-connection/)

### Cloning the Repository

Clone the repository:
```
mkdir -p ~/workspaces/bfd/
git clone git@github.com:CMSgov/beneficiary-fhir-data.git ~/workspaces/bfd/beneficiary-fhir-data.git
```

### Native Setup
1. Install JDK 8. You'll need Java 8 to run BFD. You can install OpenJDK 8 however you prefer. Problems currently arise after the 8.0.252 release. 
2. Install Maven 3. Project tasks are handled by Apache Maven. Install it however you prefer.
3. Configure your toolchain. You'll want to configure your `~/.m2/toolchains.xml` file to look like the following:
```xml
<?xml version="1.0" encoding="UTF8"?>
<toolchains>
  <!-- JDK toolchains -->
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>1.8</version>
      <vendor>sun</vendor>
    </provides>
    <configuration>
      <jdkHome>/path/to/your/jdk</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```
4. Change to the `apps/` directory and `mvn clean install -DskipITs`. The flag to skip the integration tests is important here. You will need to have AWS access for the integration tests to work correctly.
5. Set up a Postgres 12 database. Change to the `contributing` directory, and make sure the following variables are set: `SYNTHETIC_DATA_LOCATION` (A directory in an AWS S3 public bucket with the Synthetic Data RIF files -- Double check for the most recent location) i.e. run `export SYNTHETIC_DATA_LOCATION=https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields`, and `LOCAL_SYNTHETIC_DATA` - run `export LOCAL_SYNTHETIC_DATA=$BFD_PATH/contributing/synthetic-data`. 

Then run `./create-db.sh` to set up a database with the latest synthetic data update. If unable to, the easiest way to set up a local database is with the following command. Data will be persisted between starts and stops in the `bfd_pgdata` volume.
```sh
docker run \
  -d \
  --name 'bfd-db' \
  -e 'POSTGRES_USER=bfd' \
  -e 'POSTGRES_PASSWORD=InsecureLocalDev' \
  -p '5432:5432' \
  -v 'bfd_pgdata:/var/lib/postgresql/data' \
  postgres:12
```
6. To load one test beneficiary, with your database running, change directories into `apps/bfd-pipeline/bfd-pipeline-ccw-rif` and run `mvn -Dits.db.url="jdbc:postgresql://localhost:5432/bfd" -Dits.db.username=bfd -Dits.db.password=InsecureLocalDev -Dit.test=RifLoaderIT#loadSampleA clean verify`. This will kick off the integration test `loadSampleA`. After the job completes, you can verify that it ran properly with `docker exec bfd-db psql 'postgresql://bfd:InsecureLocalDev@localhost:5432/bfd' -c 'SELECT "beneficiaryId" FROM "Beneficiaries" LIMIT 1;'`
7. Run `export BFD_PORT=6500` and add it to your profile, too. The actual port is not important, but without it the `start-server` script will pick a different one each time, which gets annoying later.
8. Now it's time to start the server up. Change to `apps/bfd-server` and run `mvn -X -Dits.db.url="jdbc:postgresql://localhost:5432/bfd?user=bfd&password=InsecureLocalDev" --projects bfd-server-war package dependency:copy antrun:run org.codehaus.mojo:exec-maven-plugin:exec@server-start`. After it starts up, you can tail the logs with `tail -f bfd-server-war/target/server-work/server-console.log`.
9. We're finally going to make a request. BFD requires that clients authenticate themselves with a certificate. Those certs live in the `apps/bfd-server/dev/ssl-stores` directory. We can curl the server using a cert with this command `curl --cert $BFD_PATH/apps/bfd-server/dev/ssl-stores/client-unsecured.pem -s https://localhost:$BFD_PORT/v2/fhir/ExplanationOfBenefit/?patient=-20140000001827&_format=json`, where `$BFD_PATH` is that path to the `beneficiary-fhir-data` repo on your system. It may be helpful to have that set in your profile, too. To configure Postman, go to `Settings -> Certificates -> Add certificate` and load in `apps/bfd-server/dev/ssl-stores/client-trusted-keystore.pfx` under the PFX File option. The passphrase is `changeit`. Under `Settings -> General` you'll also want to turn off "SSL Certificate Verification."
10. Total success (probably)!. You have a working call. But you'll probably want more data. Move on to the next section to see how to load 30,000 synthetic beneficiaries. 

### Load Full Synthetic Dataset
1. Change to the top-level `contributing` directory and run `make synthetic-data/*.rif`. This will fetch RIF files (the raw incoming data) from a public S3 bucket.
2. Run `export LOCAL_SYNTHETIC_DATA=$BFD_PATH/contributing/synthetic-data`. You may want this one in your profile, too.
3. Apply the patched files with `git apply contributing/patches/load_local_synthetic_data.patch` from the root of the project. This will change three files: `StaticRifResource.java`, `StaticRifResourceGroup.java` and `RifLoaderIT.java`. The changes effectively create an integration test that point to the local RIF files that you just pulled down.
4. Change to the `apps` directory and run `mvn clean install -DskipITs` again to recompile with the newly changed files.
5. Make sure you have an active MFA session with AWS. The integration tests will need to be allowed to create an S3 bucket.
6. Change to `apps/bfd-pipeline/bfd-pipeline-ccw-rif` and run `mvn -Dits.db.url="jdbc:postgresql://localhost:5432/bfd" -Dits.db.username=bfd -Dits.db.password=InsecureLocalDev -Dit.test=RifLoaderIT#loadLocalSyntheticData clean verify`. This could take around an hour to complete.
7. Verify everything loaded with `docker exec bfd-db psql 'postgresql://bfd:InsecureLocalDev@localhost:5432/bfd' -c 'SELECT COUNT("beneficiaryId") FROM "Beneficiaries";'`. You should see a count of 30,000.

### Docker Setup

Requirements: Docker

Let's begin!

The instructions from here on should be run from the `contributing` directory located at /

To simply run tests or execute other tasks in the BFD bring up the docker containers.
Note: As a prerequisite, the bfd Docker environments need a few variables to be set in a file named .env placed within the /contributing directory. A sample file in the `contributing` directory has been added to serve as a starting point.

```
cp .env.sample .env
```

- (defaults to `..`) `BFD_DIR` specifies the directory on your host machine where you have cloned https://github.com/CMSgov/beneficiary-fhir-data
- (defaults to `9954`) `BFD_PORT` specifies the host port to use when running the API locally
- (defaults to `/app`) `BFD_MOUNT_POINT` the path within the service container where the beneficiary-fhir-data directory will be mounted.
- (defaults to `./synthetic-data`) `SYNTHETIC_DATA` specifies a folder where you have the full set of synthetic rif files.
- (defaults to `/synthetic-data`) `SYNTHETIC_DATA_MOUNT_POINT` specifies the folder in the bfd container where the data will be mounted

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

#### Serving the BFD

Run `make up` if no docker containers are running or `make restart` if they're already running.

The FHIR server should now be reachable from the browser at https://localhost:1337. In order for the FHIR server to trust your browser and return data, the client certificate at `apps/bfd-server/dev/ssl-stores/client-trusted-keystore.pfx` needs to be imported into the browser. The cert password is 'changeit'.

In Chrome this can be done at `chrome://settings/certificates`. In Firefox it can be done at `about:preferences#privacy`, there is a button at the bottom called "View Certificates" that should give the option to import one.
Note MacOS Users: To make this cert available to Chrome or Firefox you'll need to add this cert to the Keychain application.

#### Loading data to work with

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


#### Integration with a downstream system

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

### Security

We work with sensitive information: do not put any PHI or PII in the public repo for BFD.

If you believe you’ve found or been made aware of a security vulnerability, please refer to the CMS Vulnerability Disclosure Policy (here is a [link](https://www.cms.gov/Research-Statistics-Data-and-Systems/CMS-Information-Technology/CIO-Directives-and-Policies/Downloads/CMS-Vulnerability-Disclosure-Policy.pdf) to the most recent version as of the time of this commit.

### Eclipse Configuration

The following instructions are to be executed from within the Eclipse IDE application to ensure proper configuration.

#### Eclipse JDK

Verify Eclipse is using the correct Java 8 JDK.

1. Open **Window > Preferences**.
1. Select **Java > Installed JREs**.
1. If your JDK does not appear in the **Installed JREs** table add it by clicking the **Add** button, select **Standard VM** and locate your installation using the **Directory...** button.
1. Ensure your JDK is selected in the **Installed JREs** table by checking the checkbox next to the JDK you wish to use.

#### Eclipse Preferences

If you're using Eclipse for development, you'll want to configure its preferences, as follows:

1. Open **Window > Preferences**.
1. Select **Maven**.
    1. Enable **Download Artifact Sources**.
    1. Enable **Download Artifact JavaDoc**.
1. Select **Maven > Annotation Processing**.
    1. Enable the **Automatically configure JDT APT** option.
1. Select **Java > Code Style > Code Templates**.
    1. Click **Import...** and select this project's [eclipse-codetemplates.xml](docs/assets/eclipse-codetemplates.xml) file.
        * This configures the file, class, method, etc. comments on new items such that they match the existing style used in these projects.
    1. Enable the **Automatically add comments for new methods and types** option.
1. Select **Java > Code Style > Formatter**.
    1. Click **Import...** and select this project's [eclipse-java-google-style.xml](docs/assets/eclipse-java-google-style.xml) file.
        * This configures the Eclipse autoformatter (`ctrl+shift+f`) to (mostly) match the one used by the autoformatter that is applied during Maven builds.
        * The [eclipse-java-google-style.xml](docs/assets/eclipse-java-google-style.xml) file was originally acquired from here: <https://github.com/google/styleguide/blob/gh-pages/eclipse-java-google-style.xml>.
1. Select **Java > Editor > Save Actions**.
    1. Enable the **Perform the selected actions on save** option.
    1. Enable the **Format source code** option.
1. Click **OK**.

#### Importing Maven Projects into Eclipse

The repository can easily be added to your Eclipse workspace using the **Import** feature.

1. Open **File > Import...**.
1. Select **Existing Maven Projects**.
1. Specify the **Root Directory** using the **Browse...** button or by typing in a path: `~/workspaces/bfd/beneficiary-fhir-data.git`.
1. Verify that it found the projects in the **Projects** table.
1. Click **Finish**.
1. The projects and packages you selected will now appear in the **Project Explorer** window.

