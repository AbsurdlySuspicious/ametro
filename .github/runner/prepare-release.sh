#!/usr/bin/env bash

out_dir=$2
tag_raw=$1
tag=${tag_raw#refs/tags/}
version=${tag#release-}

if [ "$version" == "" ] || [ "$tag" == "" ]; then
  echo "tag is empty: version '$version', tag '$tag', raw '$tag_raw'"
  exit 1
fi

if ! [ -d "$out_dir" ]; then
  echo "out_dir is not a directory"
  exit 2
fi

apks=""
for f in "$out_dir"/*-signed.apk; do
  apks="${apks}${f}\n"
done

echo "::set-output name=apk-list::$apks"
echo "::set-output name=tag::$tag"
echo "::set-output name=version::$version"
