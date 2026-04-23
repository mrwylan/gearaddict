#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: ./release.sh <version>   (e.g. 0.1.2)" >&2
  exit 1
fi

VERSION="$1"

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "error: version must match X.Y.Z (got: $VERSION)" >&2
  exit 1
fi

TAG="v$VERSION"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "error: not inside a git repository" >&2
  exit 1
fi

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "error: working tree has uncommitted changes" >&2
  exit 1
fi

BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$BRANCH" != "main" ]]; then
  echo "error: must be on 'main' branch (currently on '$BRANCH')" >&2
  exit 1
fi

if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
  echo "error: tag $TAG already exists" >&2
  exit 1
fi

echo "==> Bumping pom.xml to $VERSION"
./mvnw -q versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false

echo "==> Committing"
git add pom.xml
git commit -m "Release $VERSION"

echo "==> Tagging $TAG"
git tag "$TAG"

echo "==> Pushing main"
git push origin main

echo "==> Pushing $TAG (this triggers the Release workflow)"
git push origin "$TAG"

echo "Done. Watch the workflow with: gh run watch"
