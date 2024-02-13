# Beneficiary FHIR Server Diagrams

## What is Diagrams?

[Diagrams](https://diagrams.mingrammer.com/) (Diagram as Code) lets you draw the cloud system architecture in Python code.

It was born for prototyping a new system architecture without any design tools. You can also describe or visualize the existing system architecture as well.

Diagram as Code allows you to track the architecture diagram changes in any version control system.

Diagrams currently supports main major providers including: AWS, Azure, GCP, Kubernetes, Alibaba Cloud, Oracle Cloud etc... It also supports On-Premise nodes, SaaS and major Programming frameworks and languages.

> Diagrams does not control any actual cloud resources nor does it generate cloud formation or terraform code. It is just for drawing the cloud system architecture diagrams.

### Installation & Pre Reqs

Requires

- **Python 3.6** or higher
- [Graphviz](https://www.graphviz.org/) to render the diagram - Open Source graph visualization software

  > macOS users can download the Graphviz via `brew install graphviz` if you're using [Homebrew](https://brew.sh). Similarly, Windows users with [Chocolatey](https://chocolatey.org) installed can run `choco install graphviz`.

- Diagrams

```shell
# using pip (pip3)
$ pip install diagrams

# using pipenv
$ pipenv install diagrams
```

## Quick Start

```python
# example_diagram.py
from diagrams import Diagram
from diagrams.aws.compute import EC2
from diagrams.aws.database import RDS
from diagrams.aws.network import ELB

with Diagram("Web Service", show=False):
    ELB("lb") >> EC2("web") >> RDS("userdb")
```

This code generates below diagram.

```shell
python3 example_diagram.py
```

![web service diagram](./example/web_service.png)

It will be saved as `web_service.png` on your working directory.

### Issues with embedded images for svg output

 As of 2/13/24 - Due to issues with [embedding images into svg outputs](https://github.com/mingrammer/diagrams/issues/26), a small script has been added here to post-process against output from diagrams python scripts.

 Once you have ran a diagram script to generate an SVG file, you will need to run the `python3 embed_svg.py <name_of_original_svg_output>` and will generate a new svg with images embedded. This file will have a suffix of `_out` added which can later be removed. Delete original svg and rename suffixed file.
