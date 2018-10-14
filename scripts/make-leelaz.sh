#!/bin/sh

set -eux

COMMIT="73f1f934169a62d7b054aefccecb41029de0fc5f"
CACHE="$HOME/.leelaz"
TARGET="$CACHE/leelaz-$COMMIT"

mkdir -p "$CACHE"

if ! test -f "$TARGET"; then
  if test "$(uname)" == "Linux"; then
    sudo apt install -y            \
      cmake                        \
      libboost-dev                 \
      libboost-filesystem-dev      \
      libboost-program-options-dev \
      libopenblas-dev              \
      opencl-headers               \
      ocl-icd-libopencl1           \
      ocl-icd-opencl-dev           \
      zlib1g-dev
  elif test "$(uname)" == "FreeBSD"; then
    brew install boost || true
    brew install cmake || true
  fi

  # Get next, on a stable commit
  git clone https://github.com/gcp/leela-zero || true
  cd leela-zero
  git reset --hard "$COMMIT"
  git submodule update --init --recursive

  # Configure CPU only
  sed -i.bak '/#define USE_OPENCL/d' src/config.h

  # Build leelaz
  mkdir -p build && cd build
  cmake ..
  cmake --build .

  mv "leelaz" "$TARGET"
  cd ../..
fi

cp "$TARGET" ./leelaz
