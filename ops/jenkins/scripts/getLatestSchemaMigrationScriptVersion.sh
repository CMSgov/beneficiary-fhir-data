#!/usr/bin/env bash
shopt -s expand_aliases

if [ "$(uname)" = 'Darwin' ]; then
    # compatibility for local development on macOS
    # requirements enforced through the following naive checks
    command -v gfind >/dev/null && alias "find=gfind" || echo "'gfind' not found; try 'brew install findutils'"
    command -v gsort >/dev/null && alias "sort=gsort" || echo "'gsort' not found; try 'brew install coreutils'"
    command -v gawk  >/dev/null && alias "awk=gawk"   || echo "'gawk' not found; try 'brew install gawk'"
    command -v gtail >/dev/null && alias "tail=gtail" || echo "'gtail' not found; try 'brew install gtail'"
    command -v gsed  >/dev/null && alias "sed=gsed"   || echo "'gsed' not found; try 'brew install gnu-sed'"
fi

REPO_ROOT="$(git rev-parse --show-toplevel)"
RELATIVE_MIGRATIONS_PATH="apps/bfd-model/bfd-model-rif/src/main/resources/db/migration"
MIGRATIONS_PATH="${REPO_ROOT}/${RELATIVE_MIGRATIONS_PATH}"

find "$MIGRATIONS_PATH" -type f -iname 'V*sql' -printf '%f\n' | sort --version-sort | tail -1 | awk -F '__' '{ print $1 }' | sed 's/[^0-9]//g'
