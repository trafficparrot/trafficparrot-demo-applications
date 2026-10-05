#!/usr/bin/env bash
# Prints `apps=<json>`: the rows of .github/apps.json to build. A pull request or a push to master
# builds the apps whose folders it changes, or every app when it changes anything under .github/.
# A tag, a manual run, or a push whose previous commit cannot be fetched builds every app.
#
# Reads EVENT (github.event_name), REF (github.ref) and BEFORE (github.event.before).
set -euo pipefail

apps=.github/apps.json
all() {
  echo "apps=$(jq -c . "$apps")"
  exit 0
}

case "$EVENT" in
  pull_request)
    # The checkout is GitHub's merge commit: its first parent is the branch the request targets.
    base=$(git rev-parse HEAD^1)
    ;;
  push)
    [[ "$REF" == refs/tags/* || -z "${BEFORE:-}" || "$BEFORE" =~ ^0+$ ]] && all
    git fetch --quiet --no-tags --depth=1 origin "$BEFORE" || all
    base="$BEFORE"
    ;;
  *)
    all
    ;;
esac

changed=$(git diff --name-only "$base" HEAD)
if grep -q '^\.github/' <<< "$changed"; then
  all
fi
echo "apps=$(jq -c --arg changed "$changed" \
  '($changed | split("\n")) as $files | [.[] | select(.app as $app | $files | any(startswith($app + "/")))]' "$apps")"
