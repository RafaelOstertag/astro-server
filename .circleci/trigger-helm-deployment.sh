#!/bin/sh

set -eu

VERSION=$(mvn -B -q help:evaluate -Dexpression='project.version' -DforceStdout=true)

if echo "${VERSION}" | grep -E -- '-SNAPSHOT$' >/dev/null
then
  echo "Snapshot version, will not deploy" >&2
  exit 1
fi

curl \
  -f \
  --header "Content-Type: application/json" \
  --header "Circle-Token: $CIRCLE_CI_TOKEN" \
  --request POST \
  --data "{\"branch\":\"master\", \"parameters\":{\"version\": \"${VERSION}\"}}" \
  https://circleci.com/api/v2/project/github/RafaelOstertag/astro-server-helm/pipeline
