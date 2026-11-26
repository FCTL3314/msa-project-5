#!/bin/bash
set -eu
api=http://kibana:5601/api
curl -fsS -X POST "$api/saved_objects/index-pattern/warehouse?overwrite=true" -H 'kbn-xsrf: setup' -H 'Content-Type: application/json' -d '{"attributes":{"title":"warehouse-batch-*","timeFieldName":"@timestamp"}}'
curl -fsS -X POST "$api/kibana/settings" -H 'kbn-xsrf: setup' -H 'Content-Type: application/json' -d '{"changes":{"defaultIndex":"warehouse"}}'
