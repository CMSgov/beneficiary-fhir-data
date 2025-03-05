# Upgrading Dependencies

All dependency versions should be declared in the `pom.xml` file contained in this folder.

Run `mvn versions:update-properties` to update dependencies to their latest versions.
Incompatible dependency versions should be tracked in `rules.xml`.

Note: running `mvn versions:use-latest-versions` should yield no changes as long as all versions are correctly tracked as properties.
