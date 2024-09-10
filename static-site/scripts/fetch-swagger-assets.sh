#!/usr/bin/env bash

# fetches the latest minified swagger assets from git

set -e


cd "$(dirname "$0")"
swagger_tmp="swagger-tmp"
rm -rf "./$swagger_tmp"
mkdir -p "./$swagger_tmp"
cd "./$swagger_tmp"

git clone https://github.com/swagger-api/swagger-ui --no-checkout . --depth 1
git sparse-checkout init
git sparse-checkout set dist
git checkout

assets="../../assets/"
cp ./dist/swagger-ui-bundle.js "$assets"
cp ./dist/swagger-ui-standalone-preset.js "$assets"
cp ./dist/swagger-ui.css "$assets"
rm -rf "../$swagger_tmp"
