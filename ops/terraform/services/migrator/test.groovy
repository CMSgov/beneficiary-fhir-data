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
      image: "public.ecr.aws/c2o1d8s9/bfd-cbc-build:jdk17-mvn3-tfenv3-kt1.9-latest" # TODO: consider a smarter solution for resolving this image
      command:
        - cat
      tty: true
      imagePullPolicy: IfNotPresent
"""
    }
  }

 stages {
    stage('Create Parameter') {
        steps {
            script {
                // authenticate
                awsAuth.assumeRole()

                awsSsm.putParameter(
                            parameterName: "/bfd/test/common/nonsensitive/brandon_test",
                            parameterValue: "test",
                            parameterType: "String",
                            parameterTags: [
                                "Key=Environment,Value=test}",
                                "Key=stack,Value=test}",
                                "Key=Terraform,Value=False",
                                "Key=application,Value=bfd",
                                "Key=business,Value=oeda"
                            ],
                            shouldOverwrite: true
                        )
            }
        }
    }
 }
}
