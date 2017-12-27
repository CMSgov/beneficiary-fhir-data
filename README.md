# FHIR Backend Stress Test

This is a test suite that utilizes jmeter, ansible and AWS to make a scalable
set of load and performance tests for the FHIR backend test server.

## Requirements

This test suite has dependencies on the bluebutton-server application.  Clone
that repo from: 

https://github.com/HHSIDEAlab/bluebutton-server

Then be sure to do a _mvn clean install_ in the bluebutton-server repository
before attempting to build this test suite.

## Running the tests

These tests are currently using fixed servers and will require your username and
password to execute.  Change to the _ansible_ subdirectory and use the following
command line to execute the tests:

    ansible-playbook site.yml -u <username> -k

This will execute the ansible test script and prompt for an ssh password to the
servers whose configurations are protected in the ansible vault file found in
group_vars.

