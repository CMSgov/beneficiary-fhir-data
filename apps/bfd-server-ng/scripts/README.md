# bfd-server-ng Scripts

This folder contains scripts used in the maintenance of bfd-server-ng. At the moment, there is only one, but as codegen
becomes more sophisticated, it is possible for them to be moved over.

## jpa_graph.py

Because we don't have IntelliJ pro, we have to write our own JPA mapping scripts. This script is a self contained uv script
that runs via:

```aiignore
uv run jpa_grapy.py
```

At the moment, this script is still being updated, but the goal of this script is to read in the mapping YAML files for
source columns and profiles, then create maps of the Java JPA objects (@Entity / @MappedSuperClass / @Embeddable) and
display the hierarchy in those files, and if they are compliant with the profiles defined in the YAML files.

This is done by matching the name of a profile in the @Entity class (CMS / Basis / Regular) and checking each @Column within
itself, its extension hierarchy, and all embedded objects recursively, and spitting out a rough analysis of their status.