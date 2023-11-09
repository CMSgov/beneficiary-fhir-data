#!/usr/bin/env groovy

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
