#!/bin/bash

#
# Constants
#

ADMIN_PORT=8000
ADMIN_BASE_URL="http://localhost:$ADMIN_PORT"




#
# ADD SAMPLE DATA
#

printf "\n"
printf "Installing sample data..."
printf "\n\n"

# Get All Homes example
DEF_RESPONSE_BODY_1='"[{\"id\" : 1,\"line1\":\"1 Privet Drive\",\"line2\":\"\",\"city\":\"Martins Heron\",\"county\":\"Berkshire\",\"postcode\":\"RG40 1ZZ\",\"country\":\"GB\"},{\"id\" : 2,\"line1\":\"2 Privet Drive\",\"line2\":\"\",\"city\":\"Martins Heron\",\"county\":\"Berkshire\",\"postcode\":\"RG40 1ZZ\",\"country\":\"GB\"}]"'
DEFINITIONS_1="[{\"extId\":null,\"orderNo\":1,\"responseContentType\":\"application/json\",\"httpStatusCode\":200,\"responseBody\":$DEF_RESPONSE_BODY_1,\"responseHeaders\":{}}]"
REQ_DATA_1="{\"path\":\"/homes\",\"method\":\"GET\",\"status\":\"ACTIVE\",\"mockType\":\"SEQ\",\"definitions\":$DEFINITIONS_1,\"rules\":[]}"
curl -i -H "Content-Type: application/json" -X POST "$ADMIN_BASE_URL/restmock" -d "$REQ_DATA_1"


# Sequenced example
DEF_RESPONSE_BODY_2='"{\"id\" : 1,\"line1\":\"1 Privet Drive\",\"line2\":\"\",\"city\":\"Martins Heron\",\"county\":\"Berkshire\",\"postcode\":\"RG40 1ZZ\",\"country\":\"GB\"}"'
DEF_RESPONSE_BODY_3='"{\"id\" : 2,\"line1\":\"2 Privet Drive\",\"line2\":\"\",\"city\":\"Martins Heron\",\"county\":\"Berkshire\",\"postcode\":\"RG40 1ZZ\",\"country\":\"GB\"}"'
DEF_RESPONSE_BODY_4='"{\"id\" : 3,\"line1\":\"3 Privet Drive\",\"line2\":\"\",\"city\":\"Martins Heron\",\"county\":\"Berkshire\",\"postcode\":\"RG40 1ZZ\",\"country\":\"GB\"}"'
DEFINITIONS_2="[{\"extId\":\"de8ed629-2ce3-46ca-a85c-e6a0fbb1ff0e\",\"orderNo\":1,\"httpStatusCode\":200,\"responseContentType\":\"application/json\",\"responseBody\":$DEF_RESPONSE_BODY_2,\"responseHeaders\":{}},{\"extId\":\"e81e44d5-d2aa-4e7a-8e8a-d530ec40362e\",\"orderNo\":2,\"httpStatusCode\":200,\"responseContentType\":\"application/json\",\"responseBody\":$DEF_RESPONSE_BODY_3,\"responseHeaders\":{}},{\"extId\":\"0bc0d0a2-9cf3-4a25-b141-5bdf3ed99052\",\"orderNo\":3,\"httpStatusCode\":200,\"responseContentType\":\"application/json\",\"responseBody\":$DEF_RESPONSE_BODY_4,\"responseHeaders\":{}}]"
REQ_DATA_2="{\"path\":\"/homes/seq\",\"method\":\"GET\",\"status\":\"ACTIVE\",\"mockType\":\"SEQ\",\"definitions\":$DEFINITIONS_2,\"rules\":[]}"
curl -i -H "Content-Type: application/json" -X POST "$ADMIN_BASE_URL/restmock" -d "$REQ_DATA_2"


# Rules example
RULE_RESPONSE_BODY_1='"{\"id\" : 1,\"line1\":\"1 Privet Drive\",\"line2\":\"\",\"city\":\"Martins Heron\",\"county\":\"Berkshire\",\"postcode\":\"RG40 1ZZ\",\"country\":\"GB\"}"'
RULE_RESPONSE_BODY_2='"{\"id\" : 2,\"line1\":\"2 Privet Drive\",\"line2\":\"\",\"city\":\"Martins Heron\",\"county\":\"Berkshire\",\"postcode\":\"RG40 1ZZ\",\"country\":\"GB\"}"'
RULE_RESPONSE_BODY_3='"{\"id\" : 3,\"line1\":\"3 Privet Drive\",\"line2\":\"\",\"city\":\"Martins Heron\",\"county\":\"Berkshire\",\"postcode\":\"RG40 1ZZ\",\"country\":\"GB\"}"'
RULES_1="[{\"extId\":null,\"orderNo\":1,\"httpStatusCode\":200,\"responseContentType\":\"application/json\",\"responseBody\":$RULE_RESPONSE_BODY_1,\"responseHeaders\":{},\"groups\":[{\"extId\":null,\"orderNo\":1,\"conditions\":[{\"extId\":null,\"ruleMatchingType\":\"PATH_VARIABLE\",\"field\":\"id\",\"comparator\":\"EQUALS\",\"dataType\":\"NUMERIC\",\"caseSensitive\":false,\"value\":\"1\"}]}]},{\"extId\":null,\"orderNo\":2,\"httpStatusCode\":200,\"responseContentType\":\"application/json\",\"responseBody\":$RULE_RESPONSE_BODY_2,\"responseHeaders\":{},\"groups\":[{\"extId\":null,\"orderNo\":1,\"conditions\":[{\"extId\":null,\"ruleMatchingType\":\"PATH_VARIABLE\",\"field\":\"id\",\"comparator\":\"EQUALS\",\"dataType\":\"NUMERIC\",\"caseSensitive\":false,\"value\":\"2\"}]}]},{\"extId\":null,\"orderNo\":3,\"responseContentType\":\"application/json\",\"httpStatusCode\":200,\"responseBody\":$RULE_RESPONSE_BODY_3,\"responseHeaders\":{},\"groups\":[{\"extId\":null,\"orderNo\":1,\"conditions\":[{\"extId\":null,\"ruleMatchingType\":\"PATH_VARIABLE\",\"field\":\"id\",\"comparator\":\"EQUALS\",\"dataType\":\"NUMERIC\",\"caseSensitive\":false,\"value\":\"3\"}]}]}]"
DEFINITIONS_3="[{\"extId\":null,\"orderNo\":1,\"responseContentType\":\"application/json\",\"httpStatusCode\":404,\"responseBody\":null,\"responseHeaders\":{}}]"
REQ_DATA_3="{\"path\":\"/home/:id\",\"method\":\"GET\",\"status\":\"ACTIVE\",\"mockType\":\"RULE\",\"definitions\":$DEFINITIONS_3,\"rules\":$RULES_1}"
curl -i -H "Content-Type: application/json" -X POST "$ADMIN_BASE_URL/restmock" -d "$REQ_DATA_3"


# Get Auth token example
DEF_RESPONSE_BODY_5='"{\"token\":\"7cf05e96-70cc-440e-9b4d-33633b88c5cc\",\"timestamp\":1500385109565}"'
DEFINITIONS_4="[{\"extId\":null,\"orderNo\":1,\"responseContentType\":\"application/json\",\"httpStatusCode\":200,\"responseBody\":$DEF_RESPONSE_BODY_5,\"responseHeaders\":{}}]"
REQ_DATA_4="{\"path\":\"/auth\",\"method\":\"POST\",\"status\":\"ACTIVE\",\"mockType\":\"SEQ\",\"definitions\":$DEFINITIONS_4,\"rules\":[]}"
curl -i -H "Content-Type: application/json" -X POST "$ADMIN_BASE_URL/restmock" -d "$REQ_DATA_4"


# Using Auth token example
RULE_RESPONSE_BODY_4='"{\"id\" : 101,\"line1\":\"101 Privet Drive\",\"line2\":\"\",\"city\":\"Martins Heron\",\"county\":\"Berkshire\",\"postcode\":\"RG40 1ZZ\",\"country\":\"GB\"}"'
RULES_2="[{\"extId\":\"3881a247-6b92-49cb-8e32-4cbd7407d89a\",\"orderNo\":1,\"httpStatusCode\":401,\"responseContentType\":\"application/json\",\"responseBody\":\"\",\"responseHeaders\":{},\"groups\":[{\"extId\":\"8cec8e54-0652-4606-a6bc-08a93a9e343d\",\"orderNo\":1,\"conditions\":[{\"extId\":null,\"ruleMatchingType\":\"REQUEST_HEADER\",\"field\":\"Authorization\",\"comparator\":\"IS_MISSING\",\"dataType\":\"TEXT\",\"caseSensitive\":false,\"value\":null}]}]},{\"extId\":\"d68d715b-6e79-4e77-80e7-bc2c415d64bd\",\"orderNo\":2,\"httpStatusCode\":200,\"responseContentType\":\"application/json\",\"responseBody\":$RULE_RESPONSE_BODY_4,\"responseHeaders\":{\"Wildcard-Home\":\"true\"},\"groups\":[{\"extId\":\"d70b6e29-5a58-4b6a-8eb5-3a0c764f2617\",\"orderNo\":1,\"conditions\":[{\"extId\":null,\"ruleMatchingType\":\"REQUEST_HEADER\",\"field\":\"Authorization\",\"comparator\":\"CONTAINS\",\"dataType\":\"TEXT\",\"caseSensitive\":false,\"value\":\"7cf05e96-70cc-440e-9b4d-33633b88c5cc\"}]}]}]"
DEFINITIONS_5="[{\"extId\":null,\"orderNo\":1,\"responseContentType\":\"application/json\",\"httpStatusCode\":401,\"responseBody\":null,\"responseHeaders\":{}}]"
REQ_DATA_5="{\"path\":\"/home/*/secure\",\"method\":\"GET\",\"status\":\"ACTIVE\",\"mockType\":\"RULE\",\"definitions\":$DEFINITIONS_5,\"rules\":$RULES_2}"
curl -i -H "Content-Type: application/json" -X POST "$ADMIN_BASE_URL/restmock" -d "$REQ_DATA_5"

printf "\n\n\n"



#
# (Re)Start Mocking Server
#
printf "Stopping Mock Server...\n"
./mockserver/shutdown_rest_mock_server.sh
printf "\n"
printf "Starting Mock Server..."
printf "\n"
./mockserver/start_rest_mock_server.sh


printf "\n\n"
printf "\n\n"

printf "Examples of cURL client calls to mocked endpoints:\n\n"

printf " curl -i -X GET http://localhost:8001/homes\n\n"
printf " curl -i -X GET http://localhost:8001/homes/seq\n\n"
printf " curl -i -X GET http://localhost:8001/home/1\n\n"
printf " curl -i -X POST '{}' http://localhost:8001/auth\n\n"
printf ' curl -i -H "Authorization: Bearer 7cf05e96-70cc-440e-9b4d-33633b88c5cc" -X GET http://localhost:8001/home/1/secure'

printf "\n\n\n"

printf "Completed sample data installation"

printf "\n\n"

exit 0
