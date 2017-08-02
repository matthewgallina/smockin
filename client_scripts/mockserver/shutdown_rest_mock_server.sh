#!/bin/bash

PORT=8000
BASE_URL="http://localhost:$PORT"

curl -i -X POST -H "Content-Type: application/json" -d '{}' $BASE_URL/mockedserver/rest/stop

exit 0
