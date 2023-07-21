Sample Data
===========

This module contains and references test beneficiary enrollment and Medicare claims data that is used to verify the code correctly operates against the real data that it will encounter in production. All access to that test data is routed through the [StaticRifResource](./src/main/java/gov/cms/bfd/pipeline/sampledata/StaticRifResource.java) class, which can be inspected to review how that data is used.
