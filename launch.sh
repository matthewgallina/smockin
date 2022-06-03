#!/bin/sh

if ["$spring_database_driverClassName" == "org.h2.Driver"]; then

    H2_DB_JAR="h2-2.1.212"

    DB_HOME="/app/db"
    DB_DATA_PATH="$DB_HOME/data/smockin_db"


    java -cp $DB_HOME/driver/$H2_DB_JAR.jar org.h2.tools.Server -tcp -tcpAllowOthers -tcpPort 9092 &

fi

java -Dspring.profiles.active=production -Dapp.version=$APP_VERSION -Duser.timezone=UTC -jar smockin-$APP_VERSION.jar 
