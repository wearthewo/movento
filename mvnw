#!/bin/sh
if command -v mvn >/dev/null 2>&1; then exec mvn "$@"; fi
if ! command -v docker >/dev/null 2>&1; then echo "Maven or Docker is required." >&2; exit 1; fi
exec docker run --rm -v "$PWD:/workspace" -w /workspace maven:3.9.9-eclipse-temurin-17 mvn "$@"
