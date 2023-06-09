#!/bin/sh

APP_VERSION="2.20.0"
H2_DB_JAR="h2-2.1.212"

DB_USERNAME="smockin"
DB_PASSWORD="smockin"
DB_HOME="/app/db"
DB_DATA_PATH="$DB_HOME/data/smockin_db"
SERVER_PORT=8000
MULTI_USER_MODE=FALSE

java -cp $DB_HOME/driver/$H2_DB_JAR.jar org.h2.tools.Server -tcp -tcpAllowOthers -tcpPort 9092 > /dev/null 2>&1 &

java -Dspring.profiles.active=production -Dserver.port=$SERVER_PORT -Dspring.datasource.url=jdbc:h2:tcp://localhost:9092/$DB_DATA_PATH -Dspring.datasource.username=$DB_USERNAME -Dspring.datasource.password=$DB_PASSWORD -Dspring.datasource.maximumPoolSize=10 -Dspring.datasource.minimumIdle=10 -Duser.timezone=UTC -Dapp.version=$APP_VERSION -Dlogging.file=/app/log/smockin.log -Dmulti.user.mode=$MULTI_USER_MODE -jar smockin-$APP_VERSION.jar > /dev/null 2>&1
