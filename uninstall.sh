#!/bin/sh

APP_DIR_PATH="${HOME}/.smockin"


#
# DELETE THE '.smockin' DIRECTORY
#
if [ ! -d "${APP_DIR_PATH}" ]
then

  echo ""
  echo "Smockin is not installed..."
  echo ""

  exit

else

  # Shutdown default H2 DB if running...
  ./shutdown.sh

  echo ""
	echo "Removing $APP_DIR_PATH in user home..."
  echo ""

  rm -rf $APP_DIR_PATH

  echo ""
  echo ""
  echo "*** Smockin has been uninstalled ***"
  echo ""
  echo ""

fi
