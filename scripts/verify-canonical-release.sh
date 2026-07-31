#!/bin/sh
set -eu

script_directory=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_directory=$(dirname -- "$script_directory")
canonical_properties="$project_directory/gradle/canonical-build.properties"
container_image=$(sed -n 's/^container\.image=//p' "$canonical_properties")

if [ -z "$container_image" ]; then
    echo "Canonical container image is missing from $canonical_properties." >&2
    exit 2
fi
if [ -n "$(git -C "$project_directory" status --porcelain --untracked-files=all)" ]; then
    echo "Canonical release verification requires a clean worktree." >&2
    exit 2
fi

source_commit=$(git -C "$project_directory" rev-parse HEAD)
container_uid=$(id -u)
container_gid=$(id -g)
temporary_directory=$(mktemp -d "${TMPDIR:-/tmp}/odb-canonical-release.XXXXXX")
source_directory="$temporary_directory/source"
canonical_output="$project_directory/build/canonical-release"
mkdir "$source_directory"

cleanup() {
    chmod -R u+w "$temporary_directory" 2>/dev/null || true
    rm -rf -- "$temporary_directory"
}
trap cleanup EXIT HUP INT TERM

git -C "$project_directory" archive --format=tar HEAD | tar -xf - -C "$source_directory"

docker run --rm \
    --platform linux/amd64 \
    --user "$container_uid:$container_gid" \
    --env LC_ALL=C.UTF-8 \
    --env TZ=UTC \
    --env HOME=/tmp/odb-home \
    --env GRADLE_USER_HOME=/tmp/gradle-home \
    --tmpfs "/tmp/odb-home:rw,exec,uid=$container_uid,gid=$container_gid,size=16777216" \
    --tmpfs "/tmp/gradle-home:rw,exec,uid=$container_uid,gid=$container_gid,size=2147483648" \
    --volume "$source_directory:/workspace" \
    --workdir /workspace \
    "$container_image" \
    ./gradlew -PodbSourceCommit="$source_commit" --no-daemon --no-watch-fs clean verifyCanonicalRelease

rm -rf -- "$canonical_output"
mkdir -p "$canonical_output"
cp -R "$source_directory/build/release/." "$canonical_output/"
