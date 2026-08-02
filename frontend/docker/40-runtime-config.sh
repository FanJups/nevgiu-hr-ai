#!/bin/sh
set -eu

: "${API_URL:?API_URL must be set}"

escaped_api_url=$(printf '%s' "$API_URL" | sed 's/[\\&|]/\\&/g')
sed "s|__API_URL__|${escaped_api_url}|g" \
  /opt/nevgiu/runtime-config.template.js \
  > /usr/share/nginx/html/runtime-config.js
