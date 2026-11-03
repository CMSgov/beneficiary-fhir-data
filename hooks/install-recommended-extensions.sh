#!/usr/bin/env bash
set -Eeou pipefail

# VS Code has no builtin way to install extensions from a file
jq -r '.recommendations[]' .vscode/extensions.json | gxargs -d $'\n' -I{} code --install-extension "{}"
