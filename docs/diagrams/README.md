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
# diagram.py
from diagrams import Diagram
from diagrams.aws.compute import EC2
from diagrams.aws.database import RDS
from diagrams.aws.network import ELB

with Diagram("Web Service", show=False):
    ELB("lb") >> EC2("web") >> RDS("userdb")
```

This code generates below diagram.

```shell
python diagram.py
```

![web service diagram](./example/web_service_diagram.png)

It will be saved as `web_service.png` on your working directory.
