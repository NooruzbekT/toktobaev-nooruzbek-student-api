#!/bin/bash
# Evidence helper — Nooruzbek Toktobaev
# Runs a command, prints it with a terminal prompt line and its real output,
# and appends both to evidence/<part>/<NN-name>.txt (later rendered to PNG by render.py).
#
# Usage: evidence/run.sh <part> <NN-name> '<command>'
#   part: part1-linux | part2-git | part3-docker | part4-jenkins
# Secrets are masked: values of *PASSWORD*/*TOKEN*/*SECRET* variables from .env,
# values listed in $EVIDENCE_MASK (colon-separated) and GitHub tokens.

EVIDENCE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$EVIDENCE_DIR")"

if [ $# -lt 3 ]; then
  echo "Usage: $0 <part> <NN-name> '<command>'" >&2
  exit 2
fi

part="$1"; name="$2"; shift 2
cmd="$*"
out="$EVIDENCE_DIR/$part/$name.txt"
mkdir -p "$EVIDENCE_DIR/$part"

# Collect secret values that must never appear in evidence
secrets=""
if [ -f "$PROJECT_DIR/.env" ]; then
  while IFS='=' read -r key value; do
    case "$key" in
      *PASSWORD*|*TOKEN*|*SECRET*) [ -n "$value" ] && secrets+="$value"$'\n' ;;
    esac
  done < "$PROJECT_DIR/.env"
fi
secrets+="$(printf '%s' "${EVIDENCE_MASK:-}" | tr ':' '\n')"

mask() {
  SECRETS="$secrets" perl -pe '
    BEGIN { @s = grep { length } split /\n/, $ENV{SECRETS}; }
    for my $x (@s) { s/\Q$x\E/********/g }
    s/\bgh[pousr]_[A-Za-z0-9]{20,}/gh*_********/g;
  '
}

cwd="${PWD/#$HOME/\~}"
rc_file="$(mktemp)"
{
  printf 'nooruzbek@%s:%s$ %s\n' "$(hostname)" "$cwd" "$cmd"
  bash -c "$cmd" 2>&1
  echo $? > "$rc_file"
} | mask | tee -a "$out"
echo >> "$out"

rc="$(cat "$rc_file")"
rm -f "$rc_file"
exit "$rc"
