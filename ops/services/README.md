# `services` Subdirectory

This subdirectory contains the Terraform Terraservices representing BFD's various services. This could be a logical mapping between our Java applications, e.g. BFD Server to `##-server`, or other services like BFD EFT (`##-eft`).

Differing from `platform` Terraservices, these Terraservices are intended to be `apply`'d in each environment utilizing the Terraform Workspace as the environment.

## Terraservices

Terraservices are opinionated, top-level, _flat_ modules that aim to describe the resources necessary to create a single, independent "service". Think the BFD Server, BFD Pipeline, BFD EFT, etc.

### Conventions

<!-- TODO: Expand this further -->

- Terraservices are opionated, meaning that "hardcoding" invariant attribute values and resource definitions is _encouraged_. If a value changes depending on environment or cannot be computed, only then should it be elevated into our SSM configuration
- Terraservices, excluding those that are `apply`'d prior to the Terraservice describing our SSM configuration (i.e. `base` or `config`), should expose as few variables as possible. Variables that _are_ exposed should be constrained to overrides only, and should be introduced sparingly
- It should be possible for any operator, in the correct Terraform Workspace, to simply `terraform apply` the Terraservice and generate appropriate resources

## Service Folder Naming Conventions

In order to make Terraservice depdendency ordering more explicit, we have adopted a ordered prefix strategy starting at `01` and incrementing by 1 for each dependency layer, e.g. `01-base`, `02-config`, `03-cluster`, etc. Terraservices can exist at the same "layer" as each other, e.g.: `03-cluster`, `03-eft`, `03-database`, etc. This explicit ordering can be thought of as creating a tree, with each level of the tree indicating what services could be `apply`'d in parallel.

This ordering is strict with respect to our established environments (`test`, `prod-sbx`, `prod`), but some services may expose overrides or have defaults for ephemeral environments that make it possible to apply them without their dependent Terraservices having been `apply`'d (for example, specifying `db_environment_override` in an ephemeral `server` would allow an operator to skip applying `database`). Consult the `README` of the various services for more detail.
