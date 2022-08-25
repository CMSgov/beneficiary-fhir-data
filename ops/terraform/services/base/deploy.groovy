#!/usr/bin/env groovy

// TODO: Consider further generalizing this and hoisting into a new ops/jenkins/global-pipeline-libs/terraform.groovy

/* Deploys base terraservice
 * @param args a {@link Map} must include `bfdEnv`, `serviceDirectory`
 * <ul>
 * <li>bfdEnv string represents the targeted BFD SDLC Environment
 * <li>serviceDirectory string relative path to terraservice module directory
 * </ul>
*/
void deployTerraservice(Map args = [:]) {
    bfdEnv = args.env
    serviceDirectory = args.directory
    dir("${workspace}/${serviceDirectory}") {
        // Debug output terraform version
        sh "terraform --version"

        // Initilize terraform
        sh "terraform init -no-color"

        // - Attempt to create the desired workspace
        // - Select the desired workspace
        // NOTE: this is the terraform concept of workspace **NOT** Jenkins
        sh """
terraform workspace new "$bfdEnv" 2> /dev/null || true &&\
terraform workspace select "$bfdEnv" -no-color
"""
        echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"

        // Gathering terraform plan
        sh 'terraform plan -no-color -out=tfplan'

        echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"

        // Apply Terraform plan
        sh 'terraform apply -no-color -input=false tfplan'

        echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"
    }
}

return this
