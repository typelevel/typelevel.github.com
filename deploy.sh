#!/bin/bash

set -o pipefail
set -e

GIT_EMAIL="bot@typelevel.org"
GIT_NAME="Typelevel Bot"
REMOTE_REPO="https://$GH_TOKEN@github.com/typelevel/typelevel.github.com.git"
BRANCH="master"

if [ -z "$GH_TOKEN" ]; then
  echo "No GitHub access token set, not deploying"
  exit 1
fi

git config --global user.name "$GIT_NAME"
git config --global user.email "$GIT_EMAIL"

git clone --depth 1 -b "$BRANCH" --no-checkout "$REMOTE_REPO" .deploy
git archive --format=tar "$TRAVIS_COMMIT" | (
  cd .deploy
  tar xf -
)

cp -r _posts .deploy

cd .deploy
git add -A -f
git commit -m "Update site at $(date --rfc-2822 -u) based on $TRAVIS_COMMIT" --allow-empty
git push
