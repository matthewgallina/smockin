#!/bin/bash

PORT=8000
BASE_URL="http://localhost:$PORT"

curl -i -H "Content-Type: application/json" -X POST -d '{}' "$BASE_URL/mockedserver/rest/start"

exit 0
