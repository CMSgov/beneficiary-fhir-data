#!/usr/bin/env bash
set -Eeou pipefail

installed="/tmp/installed_extensions.txt"
recommended="/tmp/recommended_extensions.txt"

cleanup() {
	rm -f "$installed" "$recommended"
}

trap 'cleanup' SIGTERM
trap 'cleanup' SIGINT

# Check the list of recommended extensions against the list of installed extensions
code --list-extensions | tr '[:upper:]' '[:lower:]' >"$installed"
jq -r '.recommendations[]' .vscode/extensions.json | tr '[:upper:]' '[:lower:]' >"$recommended"

not_installed="$(grep -vxf "$installed" "$recommended")"

# Install any that are missing
if [ ! -z "$not_installed" ]; then
	echo "$not_installed" | gxargs -d $'\n' -I{} code --install-extension "{}"
fi

cleanup
