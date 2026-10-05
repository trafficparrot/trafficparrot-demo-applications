#!/usr/bin/env bash
# Builds the Apache Thrift compiler from Apache's source release into <dir>/thrift, for the apps
# whose build generates Thrift code. Ubuntu's thrift-compiler (0.19 on 24.04) generates code that
# does not compile against libthrift 0.24, so the version here follows thrift.version in
# thrift-calculator/pom.xml. Needs bison, flex, g++ and make.
#
#   .github/scripts/thrift-compiler.sh <dir>
set -euo pipefail

version=0.24.0
sha256=e0fa5839a4c5c1d631b0931cf2c554ebbfa4e2fee3a9fb3ffd4f82ce4396c6e4
dest="$1"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

curl -fsSL -o "$work/thrift.tar.gz" "https://archive.apache.org/dist/thrift/$version/thrift-$version.tar.gz"
echo "$sha256  $work/thrift.tar.gz" | sha256sum --check --quiet
tar -xzf "$work/thrift.tar.gz" -C "$work"
(
  cd "$work/thrift-$version"
  ./configure --quiet --disable-libs --disable-tests --disable-tutorial
  make --quiet -j "$(nproc)" -C compiler/cpp
)
mkdir -p "$dest"
cp "$work/thrift-$version/compiler/cpp/thrift" "$dest/thrift"
"$dest/thrift" --version
