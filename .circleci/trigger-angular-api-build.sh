#!/bin/sh

set -eu

VERSION=$(mvn -B -q help:evaluate -Dexpression='project.version' -DforceStdout=true)

curl \
  -f \
  --header "Content-Type: application/json" \
  --header "Circle-Token: $CIRCLE_CI_TOKEN" \
  --request POST \
  --data "{\"branch\":\"master\", \"parameters\":{\"version\": \"${VERSION}\"}}" \
  https://circleci.com/api/v2/project/github/RafaelOstertag/astro-server-angular/pipeline
