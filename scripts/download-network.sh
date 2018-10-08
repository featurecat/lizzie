#!/bin/sh

set -eux

HASH="d351f06e446ba10697bfd2977b4be52c3de148032865eaaf9efc9796aea95a0c"
CACHE="$HOME/.leelaz"
TARGET="$CACHE/$HASH.gz"

if ! test -f "$TARGET"; then
  mkdir -p "$CACHE"
  url="http://zero.sjeng.org/networks/$HASH.gz"
  curl --output "$TARGET" "$url"
fi

cp "$TARGET" ./network.gz
