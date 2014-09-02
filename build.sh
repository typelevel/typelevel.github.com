#!/usr/bin/env bash
set -e

mk_pyenv()
{
  [ -e .pyenv ] && (echo ".pyenv already exists, exiting"; exit)

  echo "Creating new .pyenv ..."
  mkdir .pyenv
  virtualenv -p python2 .pyenv
}

mk_pyenv

PATH="$PATH:.pyenv/bin" jekyll serve --watch --baseurl ''
