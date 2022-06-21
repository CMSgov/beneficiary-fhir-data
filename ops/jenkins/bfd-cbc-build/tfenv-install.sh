#!/usr/bin/env bash
# This both "installs" tfenv and uses tfenv to install
# specified versions of terraform. "Installation" of tfenv is merely
# a cloning operation from the tfenv git repository.
# This DOES NOT set the default versions, i.e. `tfenv use x.y.z`.
# Instead, a `.terraform-version` file must be included along the
# directory hierarchy for regular use.
#
# Globals:
#   TFENV_REPO_HASH a string of the desired git commit hash from
#                   https://github.com/tfutils/tfenv to 'install'.
#                   Value set to `$1`.
#   TFENV_VERSIONS a space-delimited string of valid terraform versions
#                  to pre-install. Value set to `$2`.
set -euo pipefail

TFENV_REPO_HASH="$1"
TFENV_VERSIONS="$2"

git clone https://github.com/tfutils/tfenv.git ~/.tfenv
cd ~/.tfenv
git reset --hard "$TFENV_REPO_HASH" --

for v in $TFENV_VERSIONS; do ~/.tfenv/bin/tfenv install "$v"; done
