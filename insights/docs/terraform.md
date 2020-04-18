# Terraform Scripts
BFD-Insights scripts to create and modify all AWS resources. Although some provisioning is done within QuickSight, the Terraform scripts implement the vast majority of the provisioning in the BFD-Insights project.    

## Top Level Folders

- **modules**:  Common modules between deployment 
- **prod-lake**: Create the buckets, workgroups, 
- **group**: Create users and their membership into groups
- **test-lake**: The pipelines and database for testing ETL scripts. 
- **projects**: Individual projects which create workflows

## Modules
Various code resource modules. These modules should be generic to projects. Modules should form an abstraction. 

## Projects
Project folders may contain:
- **main.tf** required main entry point
- **variables.tf** optional inputs
- **outputs.tf** optional outputs
- **modules** optional sub-modules for the project 
