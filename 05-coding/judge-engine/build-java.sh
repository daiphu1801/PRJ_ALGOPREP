#!/bin/sh
set -eu

# Accept only the two source names owned by F3; source text is never an argument.
case "${1-}" in
  Solution.java|Main.java) source_file="$1" ;;
  *) echo 'Invalid Java source filename' >&2; exit 2 ;;
esac
test "$#" -eq 1 || exit 2
mkdir -p /w/classes
javac -encoding UTF-8 -proc:none -Xmaxerrs 20 -J-Duser.language=en \
  -d /w/classes "/w/$source_file"
jar cf /w/classes.jar -C /w/classes .
