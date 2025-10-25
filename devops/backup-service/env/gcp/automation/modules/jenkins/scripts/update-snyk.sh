#!/bin/bash
set -euo pipefail

trap 'on_error $LINENO $?' ERR

function on_error() {
    local lineno="$1"
    local errcode="$2"
    echo "Error occurred at line $lineno. Exit status: $errcode"
    exit "$errcode"
}

function version_greater() {
    local v1="$1"
    local v2="$2"
    [ "$(printf '%s\n' "$v1" "$v2" | sort -V | head -n1)" != "$v2" ]
}

function install_snyk() {
    local download_url="$1"
    local install_path="/usr/local/bin/snyk"

    echo "Downloading snyk-linux binary to $install_path..."
    curl --silent --fail --show-error --location "$download_url" --output "$install_path"
    chmod --verbose 0755 "$install_path"
    echo "Snyk CLI installed successfully at $install_path"
}

function main() {
    if ! command -v jq >/dev/null; then
        echo "Error: jq is required but not installed." >&2
        exit 1
    fi

    local response download_url current_version latest_version
    response="$(curl --retry 3 --retry-delay 2 --silent --fail --show-error https://api.github.com/repos/snyk/cli/releases/latest)"

    download_url="$(echo "$response" | jq -r '.assets[] | select(.name == "snyk-linux") | .browser_download_url')"
    if [[ -z "$download_url" ]]; then
        echo "Error: Download URL not found." >&2
        exit 1
    fi

    current_version="$(snyk --version 2>/dev/null || true)"
    latest_version="$(echo "$response" | jq -r '.tag_name' | cut -c2-)"

    if [[ -z "$current_version" ]]; then
        install_snyk "$download_url"
        exit 0
    fi

    if [[ -n "$latest_version" ]] && version_greater "$latest_version" "$current_version"; then
        local snyk_path
        snyk_path="$(command -v snyk || true)"
        if [[ -n "$snyk_path" && "$snyk_path" == /usr/local/bin/* ]]; then
            echo "Removing current Snyk binary from $snyk_path"
            rm --verbose --force "$snyk_path"
        fi
        install_snyk "$download_url"
    fi
}

main
