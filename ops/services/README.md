# `services` Subdirectory

This subdirectory contains the Terraform Terraservices representing BFD's various services. This could be a logical mapping between our Java applications, e.g. BFD Server to `##-server`, or other services like BFD EFT (`##-eft`).

## Terraservices

Terraservices are opinionated, top-level, _flat_ modules that aim to describe the resources necessary to create a single, independent "service". Think the BFD Server, BFD Pipeline, BFD EFT, etc.

### Conventions

<!-- TODO: Expand this further -->

- Terraservices are opionated, meaning that "hardcoding" invariant attribute values and resource definitions is _encouraged_. If a value changes depending on environment or cannot be computed, only then should it be elevated into our SSM configuration
- Terraservices, excluding those that are `apply`'d prior to the Terraservice describing our SSM configuration (i.e. `base` or `config`), should expose as few variables as possible. Variables that _are_ exposed should be constrained to overrides only, and should be introduced sparingly
- It should be possible for any operator, in the correct Terraform Workspace, to simply `terraform apply` the Terraservice and generate appropriate resources
