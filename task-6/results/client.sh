#!/bin/sh
set -eu
curl --silent --show-error --fail-with-body -X POST "${WAREHOUSE_URL:-http://127.0.0.1:18481}/imports?sample=${1:-normal}"
echo
