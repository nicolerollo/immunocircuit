#!/usr/bin/env bash
set -euo pipefail
SCRIPT=${1:-examples/th17_demo.icirc}
java -cp out immunocircuit.Main "$SCRIPT"
