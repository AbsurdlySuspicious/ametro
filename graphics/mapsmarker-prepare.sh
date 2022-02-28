#!/usr/bin/env bash

out=$1; shift 1
[ -d "$out" ] || { echo "out arg is not a directory"; exit 1; }

for f in "$@"; do
  o="$out/$(basename "$f")"; echo "$f -> $o"
  convert "$f" -crop 24x24+4+14 "$o"
done
