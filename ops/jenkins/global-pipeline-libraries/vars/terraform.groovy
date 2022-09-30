#!/usr/bin/env groovy
// terraform.groovy contains global method for deploying various terraservice

/* Deploys base terraservice
 * @param args a {@link Map} must include `env`, `directory`
 * <ul>
 * <li>env string represents the targeted BFD SDLC Environment
 * <li>directory string relative path to terraservice module directory
 * <li>tfVars optional map represents module's terraform input variables and their respective values
 * </ul>
*/
void deployTerraservice(Map args = [:]) {
    bfdEnv = args.env
    serviceDirectory = args.directory
    tfVars = args.tfVars ?: [:]

    // format terraform variables
    terraformVariables = tfVars.collect { k,v -> "\"-var=${k}=${v}\"" }.join(" ")

    dir("${workspace}/${serviceDirectory}") {
        // Debug output terraform version
        sh 'terraform --version'

        // Initilize terraform
        sh 'terraform init -no-color'

        // - Attempt to create the desired workspace
        // - Select the desired workspace
        // NOTE: this is the terraform concept of workspace **NOT** Jenkins
        sh """
terraform workspace new "$bfdEnv" 2> /dev/null || true &&\
terraform workspace select "$bfdEnv" -no-color
"""
        echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"

        // Gathering terraform plan
        sh "terraform plan ${terraformVariables} -no-color -out=tfplan"

        echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"

        // Apply Terraform plan
        sh 'terraform apply -no-color -input=false tfplan'

        echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"
    }
}

return this
