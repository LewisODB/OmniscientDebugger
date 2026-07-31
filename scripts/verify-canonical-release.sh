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

snapshot_checkout=false
if git -C "$project_directory" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    if [ -n "$(git -C "$project_directory" status --porcelain --untracked-files=all)" ]; then
        echo "Canonical release verification requires a clean worktree." >&2
        exit 2
    fi
    source_commit=$(git -C "$project_directory" rev-parse HEAD)
    snapshot_checkout=true
elif [ -f "$project_directory/SOURCE-COMMIT" ]; then
    source_commit=$(tr -d '\r\n' < "$project_directory/SOURCE-COMMIT")
else
    echo "Canonical release verification requires a Git checkout or source archive." >&2
    exit 2
fi
case "$source_commit" in
    *[!0-9a-f]*|'')
        echo "Invalid source commit: $source_commit" >&2
        exit 2
        ;;
esac
if [ "${#source_commit}" -ne 40 ]; then
    echo "Invalid source commit: $source_commit" >&2
    exit 2
fi

container_uid=$(id -u)
container_gid=$(id -g)
temporary_directory=$(mktemp -d "${TMPDIR:-/tmp}/odb-canonical-release.XXXXXX")
source_directory="$temporary_directory/source"
canonical_output="$project_directory/build/canonical-release"

cleanup() {
    chmod -R u+w "$temporary_directory" 2>/dev/null || true
    rm -rf -- "$temporary_directory"
}
trap cleanup EXIT HUP INT TERM

if [ "$snapshot_checkout" = true ]; then
    mkdir "$source_directory"
    git -C "$project_directory" archive --format=tar HEAD | tar -xf - -C "$source_directory"
else
    source_directory="$project_directory"
fi

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
