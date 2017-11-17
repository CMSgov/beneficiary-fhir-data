# FHIR Backend Stress Test

This is a test suite that utilizes jmeter, ansible and AWS to make a scalable
set of load and performance tests for the FHIR backend test server.

## Requirements

This test suite has dependencies on the bluebutton-server application.  Clone
that repo from: 

https://github.com/HHSIDEAlab/bluebutton-server

Then be sure to do a _mvn clean install_ in the bluebutton-server repository
before attempting to build this test suite.


