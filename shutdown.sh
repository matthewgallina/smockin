#!/bin/sh

# Check Java 8 is installed
SMOCKIN_JAVA_VERSION=$(java -version 2>&1 | grep -i version | sed 's/.*version ".*\.\(.*\)\..*"/\1/; 1q')

if [ "${SMOCKIN_JAVA_VERSION}" \< 8 ]
then
  echo ""
  echo "Smockin requires Java 8 or later to run"
  echo ""
  echo "Please visit 'http://www.java.com/en/download' to install the latest Java Runtime Environment (JRE)"
  echo ""
  echo "If you have installed Java and are still seeing this message, then please ensure this is present in your PATH"
  echo ""

  exit
fi



APP_DIR_PATH="${HOME}/.smockin"
PIDS_DIR_PATH="${APP_DIR_PATH}/pids"
SMOCKIN_PID_FILE="$PIDS_DIR_PATH/smockin-app.pid"
DB_PID_FILE="$PIDS_DIR_PATH/smockin-db.pid"


#
# Kill smockin if running
if [ -f $SMOCKIN_PID_FILE ];
then
    kill -15 $(less $SMOCKIN_PID_FILE)
    echo "Smockin Admin Stopped"
    rm $SMOCKIN_PID_FILE
else
    echo "Nothing to stop. Smockin Admin is not running"
fi


#
# Kill H2 DB if running
if [ -f $DB_PID_FILE ];
then
    kill -15 $(less $DB_PID_FILE)
    echo "H2 DB TCP Server Stopped"
    rm $DB_PID_FILE
else
    echo "Nothing to stop. H2 DB TCP Server is not running"
fi

exit 0
