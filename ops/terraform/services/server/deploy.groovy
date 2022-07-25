// initializes, selects appropriate workspace, plans, and applies terraform
def deployLocustRegression(Map args = [:]) {
    bfdEnv = args.bfdEnv
    dockerImageTagOverride = args.dockerImageTagOverride

    dir("${workspace}/ops/terraform/services/server/bfd-regression-suite") {
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
        // Gathering terraform plan
        echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"
        if (dockerImageTagOverride != null) {
            sh """
terraform plan \
-var='docker_image_tag_override=${dockerImageTagOverride}' \
-no-color -out=tfplan
"""
        } else {
            sh "terraform plan -no-color -out=tfplan"
        }
        
        // Apply Terraform plan
        echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"
        sh '''
terraform apply \
-no-color -input=false tfplan
'''
        echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"
    }
}

return this