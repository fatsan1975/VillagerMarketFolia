#!/usr/bin/env sh

# Text-only Gradle launcher.
# If wrapper JAR is missing (not committed to keep PR text-only),
# fall back to system Gradle.

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

echo "ERROR: 'gradle' command not found in PATH. Install Gradle or restore gradle/wrapper/gradle-wrapper.jar." >&2
exit 1
