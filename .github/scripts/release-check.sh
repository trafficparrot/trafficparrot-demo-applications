#!/usr/bin/env bash
# Keeps a released version's download unchanged for good. <dist> holds one folder per app, each
# with the app's <app>-<version>.zip just built. For each:
#  - a version no earlier release carries is new, and the fresh build is published;
#  - a version an earlier release carries is published again byte for byte from that release,
#    provided the app has not changed since; if it has, this fails until its version is raised.
# Run from the repository root with the release history fetched (tags) and GH_TOKEN set.
#
#   .github/scripts/release-check.sh <dist>
set -euo pipefail
dist="$1"
repo="${GITHUB_REPOSITORY:-trafficparrot/trafficparrot-demo-applications}"
failed=0
for folder in "$dist"/*/; do
  app="$(basename "$folder")"
  zips=("$folder"*.zip)
  if [[ ${#zips[@]} -ne 1 || ! -f "${zips[0]}" ]]; then
    echo "::error::expected one zip for $app, found: ${zips[*]}"
    failed=1
    continue
  fi
  name="$(basename "${zips[0]}")"
  # The oldest release carrying this file, if any (the API lists the newest first).
  first="$(gh api "repos/$repo/releases" --paginate \
    --jq ".[] | select(any(.assets[]; .name == \"$name\")) | .tag_name" | tail -n 1)"
  if [[ -z "$first" ]]; then
    echo "$name: new in this release"
  elif git diff --quiet "$first" HEAD -- "$app" ":(exclude)$app/README.md"; then
    gh release download "$first" --repo "$repo" --pattern "$name" --dir "$folder" --clobber
    echo "$name: unchanged, the file $first released"
  else
    echo "::error::$app has changed since $first released $name: raise the version in $app/pom.xml"
    failed=1
  fi
done

# A README links .../releases/latest/download/<app>-<version>.zip, which breaks when the version
# moves on without it.
for link in $(git grep -h -o "releases/latest/download/[A-Za-z0-9._-]*\.zip" -- ':!.github' | sort -u); do
  name="${link##*/}"
  if ! compgen -G "$dist/*/$name" > /dev/null; then
    echo "::error::$(git grep -l "$link" -- ':!.github' | tr '\n' ' ')links $name, which this release does not carry"
    failed=1
  fi
done
exit "$failed"
