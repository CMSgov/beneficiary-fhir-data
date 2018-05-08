# FHIR Backend Stress Test

This is a test suite that utilizes jmeter, ansible and AWS to make a scalable
set of load and performance tests for the FHIR backend test server.

## Requirements

### Bluebutton-server Application

This test suite has dependencies on the bluebutton-server application.  Clone
that repo from: 

https://github.com/HHSIDEAlab/bluebutton-server

Then be sure to run a _mvn clean install_ in the bluebutton-server repository
before attempting to build this test suite.

### AWS Access Key

An AWS Access Key and Secret credential will need to be established within the 
ansible environment before attempting execution of the tests. Follow these
steps to setup the environment:
  1. Login to AWS bb-fhir console interface
  2. Navigate to the IAM service
  3. Select Users | __username__
  4. Select the __Security Credentials Tab__
  5. Select __Create Access Key__
  6. Download the CSV file
  7. Add the contents CSV file to the local __.aws/credentials__ file
  8. Add a __region__ selection to the __.aws/credentials__(i.e. region = us-east-1)

    Example credentials entry: 

        [bbfhir]
        region = us-east-1
        aws_access_key_id = <access key from csv file>
        aws_secret_access_key = <secret key from csv file> 

  9. Add a profile entry to the local __.aws./config file__.  This will be used by ansible to gain access to AWS.

    Example config entry:

        [profile bbfhir]
        region = us-east-1
        output = json
        aws_access_key_id = <access key from csv file>
        aws_secret_access_key = <secret key from csv file> 

### extravars.yml

To simplify the command line when running the ansible test scripts it is highly
recommended to create an extravars.yml file containing the following information
and pass it to the ansible script as demonstrated in [Running the
tests](#running-the-tests).

        ec2_key_name: <name of pem file to use when creating ec2 instances> 
        profile: <environment profile name(i.e. bbfhir)>
        mfa_sn: <serial number of mfa device>
        num_servers: <number of jmeter slave servers to launch> 

### Multifactor Authentication

The ansible scripts will prompt for an MFA token.  This is the will be the token
for the MFA device attached to the AWS console profile configured above.  To 
retrieve the serial number use the following steps:

  1. Login to AWS bb-fhir console interface
  2. Navigate to the IAM service
  3. Select Users | __username__
  4. Select the __Security Credentials Tab__
  5. Locate __Assigned MFA device__ entry
  6. Use this entry as the __mfa_sn__ entry in [extravars.yml](#extravars.yml) 

## Running the tests

These tests use scalable EC2 instances to execute a jmeter test within the AWS 
environment.  You are currently using fixed servers and will require your username 
and password to execute.  Change to the _ansible_ subdirectory and use the following
command line to execute the tests:

    ansible-playbook fhir-stress-test.yml --private_key=<key pem file> -e "@extravars.yml"

This will execute the jmeter tests within the bb-fhir AWS enrionment.  To
customize the test configuration edit __ansible/groups_vars/all/main.yml__.  The
configuration variables are well documented within that file so please reference
it directly for more information.  The file also contains the configuration 
information for AWS specific items like security groups, subnets, etc.

