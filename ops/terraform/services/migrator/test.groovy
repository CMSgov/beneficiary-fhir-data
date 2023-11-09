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
    stage('Run Pipeline Job from Queue') {
      steps {
        script {
            awsAuth.assumeRole()
            awsSsm.putParameter(
                parameterName: "/bfd/test/common/nonsensitive/brandon_test",
                parameterValue: "test",
                parameterType: "String",
                parameterTags: "Key=Source,Value=${JOB_NAME}",
                shouldOverwrite: true
            )
        }
      }
    }
  }
}
