#!/usr/bin/env bash

branch=$(git rev-parse --abbrev-ref HEAD)
commit=$(git rev-parse --short HEAD)
rename="s/-unsigned//; s/\.apk/-$branch-$commit.apk/"
out="$HOME/out"

mkdir "$out" || exit 1
find app/build -type f -name '*.apk' | while read -r f; do
  name=$(basename "$f" | perl -pe "$rename")
  mv -v "$f" "$out/$name"
done
