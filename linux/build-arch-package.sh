#!/usr/bin/env bash
set -euo pipefail

project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
cd "$project_dir"

rm -rf build dist sa2ration_linux.egg-info src/sa2ration_linux.egg-info
python -m build --sdist --no-isolation
version=$(python -c 'import tomllib; print(tomllib.load(open("pyproject.toml", "rb"))["project"]["version"])')
cp "dist/sa2ration_linux-${version}.tar.gz" "packaging/sa2ration-linux-${version}.tar.gz"

cd packaging
makepkg --noconfirm -f "$@"
