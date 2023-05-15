#!/bin/sh

echo ""
echo "PLEASE NOTE: Smockin requires Java 11 or later to run"
echo ""

APP_NAME="sMockin!"
APP_VERSION="2.20.0"

APP_DIR_PATH="${HOME}/.smockin"
DB_DIR_PATH="${APP_DIR_PATH}/db"
PIDS_DIR_PATH="${APP_DIR_PATH}/pids"
DB_DRIVER_DIR_PATH="${DB_DIR_PATH}/driver"
DB_DATA_DIR_PATH="${DB_DIR_PATH}/data"
DB_MARKER="db_initialized"
H2_JAR_NAME="h2-2.1.212.jar"
DB_PROPS_FILE=db.properties
APP_PROPS_FILE=app.properties
SMOCKIN_PID_FILE="$PIDS_DIR_PATH/smockin-app.pid"
H2_DB_PID_FILE="$PIDS_DIR_PATH/smockin-db.pid"

USE_INMEM_DB=false
RESET_SYS_ADMIN=false
MULTI_USER_MODE=false
USE_CONSOLE=false
ENABLE_REMOTE_DEBUG=false
H2_CONSOLE_PORT=9099
H2_OPTS=""
REMOTE_DEBUG_ARGS=""


if [ ! -d "${APP_DIR_PATH}" ]
then
  ./install.sh
  sleep 3
fi



#
# Check if smockin already has a pid.
if [ -f $SMOCKIN_PID_FILE ];
then
    echo "SMOCKIN appears to already running, restarting this service..."
    ./shutdown.sh
    sleep 5
fi

# DB properties
DB_PROPS_FILE=$(grep "^[^#;]" ${DB_DIR_PATH}/${DB_PROPS_FILE})

DB_USERNAME=$(echo "$DB_PROPS_FILE" | grep "DB_USERNAME" | awk '{ print $3 }')
DB_PASSWORD=$(echo "$DB_PROPS_FILE" | grep "DB_PASSWORD" | awk '{ print $3 }')
DRIVER_CLASS=$(echo "$DB_PROPS_FILE" | grep "DRIVER_CLASS" | awk '{ print $3 }')
JDBC_URL=$(echo "$DB_PROPS_FILE" | grep "JDBC_URL" | awk '{ print $3 }')
HIBERNATE_DIALECT=$(echo "$DB_PROPS_FILE" | grep "HIBERNATE_DIALECT" | awk '{ print $3 }')
MAX_POOL_SIZE=$(echo "$DB_PROPS_FILE" | grep "MAX_POOL_SIZE" | awk '{ print $3 }')
MIN_POOL_SIZE=$(echo "$DB_PROPS_FILE" | grep "MIN_POOL_SIZE" | awk '{ print $3 }')

# APP properties
APP_PROPS_FILE=$(grep "^[^#;]" ${APP_DIR_PATH}/${APP_PROPS_FILE})

H2_PORT=$(echo "$APP_PROPS_FILE" | grep "H2_PORT" | awk '{ print $3 }')
APP_PORT=$(echo "$APP_PROPS_FILE" | grep "APP_PORT" | awk '{ print $3 }')
MULTI_USER_MODE_CONF=$(echo "$APP_PROPS_FILE" | grep "MULTI_USER_MODE" | awk '{ print $3 }')


if ([ ! -z $MULTI_USER_MODE_CONF ] && [ $MULTI_USER_MODE_CONF = "TRUE" ])
then
    MULTI_USER_MODE=true
fi

if ([ ! -z "$1" ] && [ $1 = "-INMEM" ]) || ([ ! -z "$2" ] && [ $2 = "-INMEM" ]); then
    USE_INMEM_DB=true
    JDBC_URL='jdbc:h2:mem:smockindev'
fi

if ([ ! -z "$1" ] && [ $1 = "-RESET_SYS_ADMIN" ]) || ([ ! -z "$2" ] && [ $2 = "-RESET_SYS_ADMIN" ]) || ([ ! -z "$3" ] && [ $3 = "-RESET_SYS_ADMIN" ]); then
    RESET_SYS_ADMIN=true
fi

if ([ ! -z "$1" ] && [ $1 = "-CONSOLE" ]) || ([ ! -z "$2" ] && [ $2 = "-CONSOLE" ]) || ([ ! -z "$3" ] && [ $3 = "-CONSOLE" ]); then
    USE_CONSOLE=true
fi

if ([ ! -z "$1" ] && [ $1 = "-DEBUG" ]) || ([ ! -z "$2" ] && [ $2 = "-DEBUG" ]) || ([ ! -z "$3" ] && [ $3 = "-DEBUG" ]); then
    ENABLE_REMOTE_DEBUG=true
    USE_CONSOLE=true
fi


echo "#####################################################################################"
echo "# "
echo "#  $APP_NAME"
echo "# "
echo "#  v$APP_VERSION"
echo "#  "



if ( $USE_CONSOLE ); then
    H2_OPTS="-web -webAllowOthers -webPort $H2_CONSOLE_PORT"
fi

if ( $ENABLE_REMOTE_DEBUG ); then
    REMOTE_DEBUG_ARGS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8008"
fi


#
# Check for H2 DB Server driver and start it up (in TCP server mode) if not already running
#
if ([ $DRIVER_CLASS = "org.h2.Driver" ] && [ !$USE_INMEM_DB ]);
then

  H2_PID=$(ps aux | grep SmockinH2DB | grep -v grep | awk '{print $2}')
  JDBC_URL=$(echo $JDBC_URL | sed "s/{H2.PORT}/$H2_PORT/g")

  if [ ! -z "$H2_PID" ] 
  then
    echo "#  H2 TCP Database is already running..."
  else
    echo "#  Starting H2 TCP Database..."
    java -cp $DB_DRIVER_DIR_PATH/$H2_JAR_NAME -DSmockinH2DB org.h2.tools.Server $H2_OPTS -tcp -tcpAllowOthers -tcpPort $H2_PORT > /dev/null 2>&1 &
    echo "$!" > $H2_DB_PID_FILE
  fi

fi


echo "#  JDBC Connectivity Properties:"
echo "#  - JDBC DRIVER: $DRIVER_CLASS"
echo "#  - JDBC URL: $JDBC_URL"
echo "#"


#
# Prepare runtime args
#
VM_ARGS="-Dspring.datasource.url=$JDBC_URL -Dspring.datasource.username=$DB_USERNAME -Dspring.datasource.password=$DB_PASSWORD -Dspring.datasource.maximumPoolSize=$MAX_POOL_SIZE -Dspring.datasource.minimumIdle=$MIN_POOL_SIZE -Duser.timezone=UTC -Dapp.version=$APP_VERSION"
APP_PROFILE="production"
RESET_SYS_ADMIN_ARG=""

if ( $USE_INMEM_DB ); then
  APP_PROFILE=""
fi

if ( $RESET_SYS_ADMIN ); then
  RESET_SYS_ADMIN_ARG="-Dreset.sys.admin=true"
fi


#
# Override any proxy environment variables, as this can cause 403 issues with internal S3 client and S3 mock server
#
export HTTP_PROXY=
export HTTPS_PROXY=


#
# START UP SMOCKIN APPLICATION
# (JPA WILL CREATE THE ACTUAL SMOCKIN DB AUTOMATICALLY IF IT DOES NOT ALREADY EXIST)
#
echo "#"
if ( $MULTI_USER_MODE ); then
  echo "#  Starting Main Application in 'MULTI USER MODE'..."
else
  echo "#  Starting Main Application..."
fi
echo "#"

echo "#  Please Note:"
echo "#  - Application logs are available from: .smockin/log (under the user.home directory)"
echo "#  - Navigate to: 'http://localhost:$APP_PORT/index.html' to access the Smockin Admin UI."

if ( $USE_CONSOLE ); then
    echo "#  - Enabling H2 web console: http://localhost:$H2_CONSOLE_PORT/"
    echo "     Please consult your db.properties file under your user home, for JDBC connection values..."
    echo " "
fi


####### Modes #######
#
# Running 'start.sh' with no argument starts the application asynchronously in the background using the main (H2 TCP) DB.
#
# Note, these commands can be combined.
# (i.e 'run.sh -CONSOLE' will enable the console and use the main DB, whereas 'run.sh -CONSOLE -INMEM' will do the same but with run with an in-memory DB.)
#
# -INMEM               Uses an in-memory DB.
# -RESET_SYS_ADMIN     Resets the System Admin user's password back to factory default.
# -CONSOLE             Runs in console view.
#
#


if ( $USE_CONSOLE ); then
  mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=$APP_PROFILE -Dserver.port=$APP_PORT -Dmulti.user.mode=$MULTI_USER_MODE $VM_ARGS $RESET_SYS_ADMIN_ARG -Dlogging.level.com.smockin=DEBUG $REMOTE_DEBUG_ARGS"
else
  echo "#  - Run 'shutdown.sh' when you wish to terminate this application."
  mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=$APP_PROFILE -Dserver.port=$APP_PORT -Dmulti.user.mode=$MULTI_USER_MODE $VM_ARGS $RESET_SYS_ADMIN_ARG $REMOTE_DEBUG_ARGS" > /dev/null 2>&1 &
  echo "$!" > $SMOCKIN_PID_FILE
fi


echo "#"
echo "#####################################################################################"
