#!/bin/bash

# GitHub Actions _aggressively_ tries to terminate runners when a Workflow is canceled/fails. See
# https://github.com/orgs/community/discussions/26311. In the context of Tofu/Terraform, this is
# bad. Tofu locks state, and doesn't commit changes to state until _after_ the apply is finished. If
# GHA tries to stop Tofu, it _will_ either fail to commit state or unlock state, or both. We use
# this wrapper (adapted from
# https://github.com/orgs/community/discussions/26311#discussioncomment-7571648) to try and give
# Tofu as much time as possible to clean itself up.

COUNTER=0
_term() {
  echo "Caught SIGTERM signal!"

  if [[ $COUNTER -lt 1 ]]; then
    echo "Passing signal to tofu"
    kill -TERM "$child" 2>/dev/null
  else
    echo "Already passed signal to tofu"
  fi

  ((COUNTER++))
}

_int() {
  echo "Caught SIGINT signal!"

  if [[ $COUNTER -lt 1 ]]; then
    echo "Passing signal to tofu"
    kill -INT "$child" 2>/dev/null
  else
    echo "Already passed signal to tofu"
  fi

  ((COUNTER++))
}

_other() {
  echo "Caught OTHER signal!"

  if [[ $COUNTER -lt 1 ]]; then
    echo "Passing signal to tofu"
    kill -INT "$child" 2>/dev/null
  else
    echo "Already passed signal to tofu"
  fi

  ((COUNTER++))
}

trap _term SIGTERM
trap _int SIGINT

trap _other SIGHUP
trap _other SIGUSR1
trap _other SIGUSR2
trap _other SIGABRT
trap _other SIGQUIT
trap _other SIGPIPE

tofu "$@" &
child=$!
wait "$child"
