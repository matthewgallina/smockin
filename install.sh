#!/bin/sh

echo ""
echo "PLEASE NOTE: Smockin requires Java 8 or later to run"
echo ""

APP_DIR_PATH="${HOME}/.smockin"
DB_DIR_PATH="${APP_DIR_PATH}/db"
DB_DRIVER_DIR_PATH="${DB_DIR_PATH}/driver"
DB_DATA_DIR_PATH="${DB_DIR_PATH}/data"
PIDS_DIR_PATH="${APP_DIR_PATH}/pids"

H2_JAR_NAME="h2-2.1.212.jar"
DB_PROPS_FILE=db.properties
APP_PROPS_FILE=app.properties
DB_PLACEHOLDER_FILE=smockin_db.mv.db

#
# CREATE THE '.smockin' APP DIRECTORY AND INSTALL THE H2 DB LIB
#
if [ ! -d "${DB_DIR_PATH}" ]
then

  echo ""
	echo "Installing .smockin config directory to user home..."
  echo ""

  mkdir   -p   $DB_DRIVER_DIR_PATH
  echo "- Created directory $DB_DRIVER_DIR_PATH"
  mkdir   -p   $DB_DATA_DIR_PATH
  echo "- Created directory $DB_DATA_DIR_PATH"
  mkdir   -p   $PIDS_DIR_PATH
  echo "- Created directory $PIDS_DIR_PATH"

  cp    install/${H2_JAR_NAME}       ${DB_DRIVER_DIR_PATH}/${H2_JAR_NAME}
  echo "- Added file ${DB_DRIVER_DIR_PATH}/${H2_JAR_NAME}"
  cp    install/${DB_PROPS_FILE}     ${DB_DIR_PATH}/${DB_PROPS_FILE}
  echo "- Added file ${DB_DIR_PATH}/${DB_PROPS_FILE}"
  cp    install/${APP_PROPS_FILE}     ${APP_DIR_PATH}
  echo "- Added file ${APP_DIR_PATH}/${APP_PROPS_FILE}"

  if [ $(uname) = "Darwin" ]; then
    sed -i '' "s/{USER.HOME}/~/" ${DB_DIR_PATH}/${DB_PROPS_FILE}
  else
    # Linux specific
    sed -i -e "s/{USER.HOME}/~/" ${DB_DIR_PATH}/${DB_PROPS_FILE}
  fi

  cp    install/${DB_PLACEHOLDER_FILE}     ${DB_DATA_DIR_PATH}/${DB_PLACEHOLDER_FILE}
  echo "- Added file ${DB_DATA_DIR_PATH}/${DB_PLACEHOLDER_FILE}"

  echo ""
  echo ""
  echo "The default H2 DB has been installed"
  echo ""
#  echo "Please run start.sh to launch the sMockin Application"
#  echo ""
  echo ""


else

  echo ""
  echo "sMockin is already installed : '$APP_DIR_PATH'"
  echo ""

fi
