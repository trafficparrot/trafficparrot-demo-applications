#!/usr/bin/env bash
# Prints the tag for the next release: v<UTC date>.<N>, where N counts that day's releases from 1
# (the first release, v2026.10.05, has no counter and counts as that day's first).
#
#   tag=$(.github/scripts/next-tag.sh) && git tag "$tag" && git push origin "$tag"
set -euo pipefail
cd "$(dirname "$0")/../.."
git fetch -q --tags origin
day="v$(date -u +%Y.%m.%d)"
echo "$day.$(( $(git tag -l "$day" "$day.*" | wc -l) + 1 ))"
