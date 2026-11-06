#!/usr/bin/env bash
set -Eeuo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
readonly REPO_ROOT
MATRIX="${REPO_ROOT}/.github/workflows/build_container_images_matrix.json"
readonly MATRIX

# @arg image Name of an image in .github/workflows/build_container_images_matrix.json
# @flag --list Show every buildable image and stop
# @option --tag Tag to apply to the image and to any base images built along the way
# @option --platform Target platform, overriding the one in the build matrix
# @flag --no-base Do not build the base images this image depends on
eval "$(argc --argc-eval "$0" "$@")"

readonly image="${argc_image:-}"
readonly list="${argc_list:-}"
readonly tag="${argc_tag:-}"
readonly platform="${argc_platform:-}"
readonly no_base="${argc_no_base:-}"

cd "$REPO_ROOT"

if ! command -v podman >/dev/null 2>&1; then
	echo "error: podman is required to build images" >&2
	exit 1
fi

list_images() {
	jq -r '.[] | [.name, .dockerfile, .platform] | @tsv' "$MATRIX" | column -t
}

if [[ -n "$list" ]]; then
	list_images
	exit 0
fi

if [[ -z "$image" ]]; then
	echo "error: an image name is required. Run with --list to see the images available." >&2
	exit 1
fi

entry_for() {
	jq -c --arg name "$1" '.[] | select(.name == $name)' "$MATRIX"
}

bases_of() {
	grep -oE '^FROM[[:space:]]+[A-Za-z0-9._-]+:\$\{base_version\}' "$1" |
		sed 's/^FROM[[:space:]]*//; s/:.*$//'
}

declares_base_version() {
	grep -qE '^ARG[[:space:]]+base_version\b' "$1"
}

build_image() {
	local name="$1"
	local entry dockerfile context image_platform bases version base
	declare -a tags build_args

	entry="$(entry_for "$name")"
	if [[ -z "$entry" ]]; then
		echo "error: '${name}' is not an image in the build matrix. Available images:" >&2
		list_images >&2
		exit 1
	fi

	dockerfile="$(jq -r '.dockerfile' <<<"$entry")"
	context="$(jq -r '.contextDir' <<<"$entry")"
	image_platform="$(jq -r '.platform' <<<"$entry")"
	if [[ -n "$platform" ]]; then
		image_platform="$platform"
	fi

	bases="$(bases_of "$dockerfile" || true)"

	if [[ -z "$no_base" ]]; then
		for base in $bases; do
			if [[ "$base" != "$name" && -n "$(entry_for "$base")" ]]; then
				build_image "$base"
			fi
		done
	elif [[ -n "$bases" ]]; then
		version="${tag:-latest}"
		for base in $bases; do
			if ! podman image exists "${base}:${version}"; then
				echo "error: ${base}:${version} is not in the local image store." >&2
				echo "Build it with: just docker-build ${base} --tag ${version}" >&2
				echo "or drop --no-base to build it as part of this run." >&2
				exit 1
			fi
		done
	fi

	tags=("$name")
	if [[ -n "$tag" ]]; then
		tags+=("$name:$tag")
	fi

	# A tagged build tags its base images with the same version, as CI does with its release
	# version. Without this, the Dockerfile's default of `latest` resolves to whatever base
	# happens to be in the local image store.
	build_args=()
	if [[ -n "$tag" ]] && declares_base_version "$dockerfile"; then
		build_args+=(--build-arg "base_version=$tag")
	fi

	echo "building ${name} from ${dockerfile} (context: ${context}, platform: ${image_platform})"
	podman buildx build "$context" --file "$dockerfile" --platform "$image_platform" \
		"${tags[@]/#/--tag=}" "${build_args[@]}"
}

build_image "$image"
