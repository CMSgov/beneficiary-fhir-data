###
# Makefile for BFD apps.
# Requires the `make` utility that comes with most major build systems:
#   MacOS: `xcode-select --install`
#   RedHat/Amznlinux: `sudo yum -y groupinstall "Development Tools"`
#   Debian/Ubuntu: `sudo apt install -y build-essential`
###
.DEFAULT_GOAL := help 		# display help if `make` is run without args
SHELL = /usr/bin/env bash # run make in a bash shell
.ONE_SHELL: 							# all in a single instance
SHELLFLAGS: -e 						# and exit on any error

###
# Env vars we want exported to each build target
###
.EXPORT_ALL_VARIABLES:
	BFD_PORT = 6500

###
# Dynamically display help for any target line ending with a double ## comment
#   (the '_:##' lines is used to add some seperation help output)
###
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[0m%-30s\033[0m %s\n", $$1, $$2}'
	@printf "bfd-?"
	@printf "\033[36m%-25s\033[0m Build & unit test specific apps:\n"
	@printf "\t\t\t\t bfd-model\n"
	@printf "\t\t\t\t bfd-shared-utils\n"
	@printf "\t\t\t\t bfd-server\n"
	@printf "\t\t\t\t bfd-pipeline\n"
	@printf "\t\t\t\t bfd-db-migrator\n"
.PHONY: help

###
# Initialization
###
init: ## Initialize the project (only needs to be done once)
	@cd .. && time mvn -f apps initialize
.PHONY: init

_:## 
###
# Run services
###
up: killall docker-db docker-s3 ./target ## Launch a new bfd-server and run migrations using local docker database
	@cd bfd-server && \
	export BFD_PORT=$(BFD_PORT) && \
	export PG_PASS=./.pgpass && \
	mvn --threads 1.5C -Dmaven.javadoc.skip=true -DskipTests \
	-Dits.db.url="jdbc:postgresql://localhost:5432/fhirdb?user=bfd&password=InsecureLocalDev" \
	--projects bfd-server-war package dependency:copy antrun:run org.codehaus.mojo:exec-maven-plugin:exec@server-start && \
	psql -h localhost -p 5432 -U bfd -c 'select * from schema_version order by installed_rank desc limit 1' fhirdb
.PHONY: up

start: # TODO: Find a way to simply start a stopped server without rebuilding everything. again.
.PHONY: start

stop: # Stops a running bfd server # TODO: this sucks
	@cd bfd-server && \
	time mvn -Dmaven.javadoc.skip=true -Dits.db.url="jdbc:postgresql://localhost:5432/fhirdb?user=bfd&password=InsecureLocalDev" \
	--projects bfd-server-war package dependency:copy antrun:run org.codehaus.mojo:exec-maven-plugin:exec@server-stop
.PHONY: stop

down: stop # Alias for stop
.PHONY: down

logs: ## Tails server logs (ctl+c to stop)
	@exec tail -f bfd-server/bfd-server-war/target/server-work/server-console.log
.PHONY: logs

killall: ## Kills all running bfd-server processes
	@pkill -f ".*bfd-server.*" >/dev/null 2>&1 || true
.PHONY: killall

_:## 
###
# Load sample data and TODO: run migrations
###
migrate: ./target docker-db
	@cd bfd-pipeline/
load-sample-rif: ./target docker-db ## Loads a single sample rif record into the local dev database
	@cd bfd-pipeline/bfd-pipeline-ccw-rif && \
	time mvn -Dits.db.url="jdbc:postgresql://localhost:5432/fhirdb" -Dits.db.username=bfd -Dits.db.password=InsecureLocalDev \
	-Dmaven.javadoc.skip=true \
	-Dit.test=RifLoaderIT#loadSampleA 
#\
clean verify
.PHONY: load-sample-rif

test-sample-rif: ## Tests that the loaded sample rif is present in the local dev db
	@docker exec bfd-db psql 'postgresql://bfd:InsecureLocalDev@localhost:5432/fhirdb' -c "SELECT bene_id FROM beneficiaries LIMIT 1;" \
	| grep '567834' >/dev/null && echo "Pass: found sample bene_id 567834" || echo "Fail: sample bene_id 5678934 not found"
.PHONY: test-sample-rif

_:## 
###
# Build and test apps
###

build: clean ../.git/hooks/pre-commit ## Build and unit test all apps (skips IT's)
	@time mvn -DskipITs install
.PHONY: build

package:
	@time mvn -q --threads 1.5C -DskipITs -DskipTests -Dmaven.javadoc.skip=true \
	package

install: clean
	@time mvn -q --threads 1.5C -DskipITs -DskipTests \
	install

verify: killall clean clean-docker-s3 docker-s3 ../.git/hooks/pre-commit ## Clean, build, test, and verify apps using docker s3 for ITs
	@time mvn --threads 1C \
	-Ds3.local=true -Ds3.localUser=bfdLocalS3Dev -Ds3.localPass=bfdLocalS3Dev \
	verify

test: ./target docker-s3 ## Runs all tests using a local docker container for S3 IT's
	@time mvn -Ds3.local=true -Ds3.localUser=bfdLocalS3Dev -Ds3.localPass=bfdLocalS3Dev install
.PHONY: test

test-s3: ## Runs IT's using real S3 resources (requires an active AWS mfa session)
	@time mvn clean install -DskipTests -Dmaven.javadoc.skip=true 
.PHONY: test-s3

clean: ## mvn clean
	@time mvn clean
.PHONY: clean

clean-mvn-cache: ## Clears your local maven repository forcing all deps to download on the next build
	@rm -rf ~/.m2/repository
.PHONY: clean-mvn-cache

purge: killall clean-docker clean clean-mvn-cache ## Purge all local dev data, artifacts, docker images/volumes, etc
	@docker rmi minio/minio >/dev/null 2>&1;
	@docker rmi postgres:12 >/dev/null 2>&1;
.PHONY: purge

yolo: # Builds apps in parallel with no testing or docs. Fast, but may introduce bugs due to our build chain not being thread safe.
	@time mvn -T 4C -DskipTests -DskipITs -Dmaven.javadoc.skip=true clean install
.PHONY: yolo

###
# build and unit test specific apps, ex `make bfd-model`
###
.PHONY: bfd-model bfd-shared-utils bfd-server bfd-pipeline bfd-db-migrator
bfd-model:
	@cd bfd-model && \
	time mvn clean install -DskipITs
bfd-shared-utils:
	@cd bfd-shared-utils && \
	time mvn clean install -DskipITs
bfd-server:
	@cd bfd-server && \
	time mvn clean install -DskipITs
bfd-pipeline:
	@cd bfd-pipeline && \
	time mvn clean install -DskipITs
bfd-db-migrator:
	@cd bfd-db-migrator && \
	time mvn clean install -DskipITs

_:## 
###
# Docker db/s3 dev containers
###
./.pgpass:
	@echo 'localhost:5432:fhirdb:bfd:InsecureLocalDev' > .pgpass

docker-db: ./.pgpass clean-docker-db ## Launches a local database container (requires docker)
	@docker run -d \
      --name 'bfd-db' \
      -e 'POSTGRES_DB=fhirdb' \
      -e 'POSTGRES_USER=bfd' \
      -e 'POSTGRES_PASSWORD=InsecureLocalDev' \
      -p '5432:5432' \
      -v 'bfd-db:/var/lib/postgresql/data' \
      postgres:12 -c max_connections=200 >/dev/null && \
			echo "bfd-db database container up"
.PHONY: docker-db

docker-s3: clean-docker-s3 ## Launches a local s3 (minio) container (requires docker)
	@docker run -d \
      -p 9000:9000 \
      -p 9001:9001 --name 'bfd-s3' \
      -e "MINIO_ROOT_USER=bfdLocalS3Dev" \
      -e "MINIO_ROOT_PASSWORD=bfdLocalS3Dev" \
			-v 'bfd-s3:/data' \
      minio/minio server /data --console-address ":9001" >/dev/null && \
			echo "bfd-s3 minio container up"
.PHONY: docker-s3

clean-docker: clean-docker-db clean-docker-s3 ## Removes all local dev db/s3 containers and purges persistant data
	@docker volume rm bfd-db >/dev/null 2>&1 || true
	@docker volume rm bfd-s3 >/dev/null 2>&1 || true
.PHONY: clean-docker

clean-docker-db: ## stop/remove running bfd-db containers (does not remove persistant data)
	@docker stop bfd-db >/dev/null 2>&1 || true
	@docker rm bfd-db >/dev/null 2>&1 || true
.PHONY: clean-docker-db

clean-docker-s3: ## stop/remove running bfd-s3 containers (does not remove persistant data)
	@docker stop bfd-s3 >/dev/null 2>&1 || true
	@docker rm bfd-s3 >/dev/null 2>&1 || true
.PHONY: clean-docker-s3

###
# Non phony targets (checks to see if the targeted file or directory exists)
###
# ensure git hooks are installed
../.git/hooks/pre-commit: init

# builds . unless ./target dir exists
./target:
	@time mvn clean install -DskipITs 

# specific apps (no testing)
./bfd-model/target:
	@cd bfd-model && \
	time mvn package -DskipTests -DskipITs -Dmaven.javadoc.skip=true 
./bfd-shared-utils/target:
	@cd bfd-shared-utils && \
	time mvn package -DskipTests -DskipITs -Dmaven.javadoc.skip=true 
./bfd-server/target:
	@cd bfd-server && \
	time mvn package install -DskipTests -DskipITs -Dmaven.javadoc.skip=true 
./bfd-pipeline/target:
	@cd bfd-pipeline && \
	time mvn package install -DskipTests -DskipITs -Dmaven.javadoc.skip=true 
./bfd-db-migrator/target:
	@cd bfd-db-migrator && \
	time mvn package install -DskipTests -DskipITs -Dmaven.javadoc.skip=true 
