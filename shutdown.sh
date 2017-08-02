#!/bin/sh

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
