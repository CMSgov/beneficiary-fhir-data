#!/usr/bin/env bash

if ! pgrep locust >/dev/null; then
  ./controller.sh &>>locust.log &
  python3 ./controller.py &
fi
