#!/usr/bin/env bash
# Makes an app's download zip, laid out as trafficparrot.com serves it, in dist/.
# Run from the repository root, after `mvn verify` in the app's folder.
#
#   .github/scripts/package.sh <app-folder> <zip-name>
set -euo pipefail
# Info-ZIP's zip and unzip take default options from these, so a caller's ZIP=<name> would
# become an extra file argument and the zip would land somewhere else.
unset ZIP ZIPOPT UNZIP UNZIPOPT ZIPINFO ZIPINFOOPT

app="$1"
zip="$2"
dist="$PWD/dist"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$dist"
rm -f "$dist/$zip"

# The one file in target/ matching a pattern; anything else is an error.
one() {
  # shellcheck disable=SC2206 # $1 is a glob, expanded on purpose
  local found=("$app"/target/$1)
  if [[ ${#found[@]} -ne 1 || ! -f "${found[0]}" ]]; then
    echo "expected one $app/target/$1, found: ${found[*]}" >&2
    exit 1
  fi
  echo "${found[0]}"
}

case "$app" in
  fruit-order-system|vegetable-order-system|flower-order-system|food-order-system)
    # The release folder as it is, with the application jar in lib/.
    cp -R "$app/release/$app" "$work/"
    cp "$(one "*-jar-with-dependencies.jar")" "$work/$app/lib/"
    ;;
  finance-application)
    # The start scripts, the default settings and the application jar, in one folder.
    folder="${zip%.zip}"
    mkdir "$work/$folder"
    cp "$app"/release/finance-application/* "$app/src/main/resources/finance-application.properties" \
      "$(one "*-jar-with-dependencies.jar")" "$work/$folder/"
    ;;
  *)
    # The app's own build makes the zip.
    cp "$(one "*.zip")" "$dist/$zip"
    ;;
esac

if [[ ! -f "$dist/$zip" ]]; then
  (cd "$work" && zip -qr "$dist/$zip" .)
fi

# IBM's MQ client is compiled against, never shipped: readers download it themselves.
if unzip -Z1 "$dist/$zip" | grep -i "com\.ibm\.mq"; then
  echo "$zip contains IBM MQ files" >&2
  exit 1
fi
for jar in $(unzip -Z1 "$dist/$zip" | grep "\.jar$"); do
  unzip -p "$dist/$zip" "$jar" > "$work/inner.jar"
  # Not grep -q: it stops reading early, and under pipefail unzip's broken pipe would hide the match.
  if unzip -Z1 "$work/inner.jar" | grep "^com/ibm/mq/" > /dev/null; then
    echo "$zip: $jar contains IBM MQ classes" >&2
    exit 1
  fi
done

unzip -l "$dist/$zip"
