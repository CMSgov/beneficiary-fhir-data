#!/usr/bin/env groovy
@Library("bfd@brandon-BFD-3012-ssm-deficiencies") _

pipeline {
  agent {
    kubernetes {
      defaultContainer 'bfd-cbc-build'
      yaml """
apiVersion: v1
kind: Pod
spec:
  serviceAccount: bfd
  restartPolicy: Never
  containers:
    - name: bfd-cbc-build
      image: "public.ecr.aws/c2o1d8s9/bfd-cbc-build:jdk11-mvn3-an29-tfenv-aeaa61fa6"
      command:
        - cat
      tty: true
      imagePullPolicy: IfNotPresent
"""
    }
  }

  stages {
    stage('Test create & tag parameter') {
      steps {
        script {
            awsAuth.assumeRole()
            awsSsm.putParameter(
                parameterName: "/bfd/test/common/nonsensitive/brandon_test",
                parameterValue: "test",
                parameterType: "String",
                shouldOverwrite: true
            )
            awsSsm.tagResource(
                resourceType: "Parameter",
                resourceId: "/bfd/test/common/nonsensitive/brandon_test",
                resourceTags: "Key=Source,Value=${JOB_NAME} Key=Environment,Value=mgmt Key=stack,Value=mgmt Key=Terraform,Value=False Key=application,Value=bfd Key=business,Value=oeda",
            )
        }
      }
    }
  }
}
