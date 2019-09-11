#!/bin/bash
#
# Simple HTTP server that listens on the passed in port. Useful for testing and build out. 
#
set -e
sudo yum update -y
sudo yum install -y python3
echo "<html><body><p>Hello from BFD</p></body></html>" > index.html
python3 -m http.server ${port}
