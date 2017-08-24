#!/bin/sh

# Check Java 8 is installed
SMOCKIN_JAVA_VERSION=$(java -version 2>&1 | grep -i version | sed 's/.*version ".*\.\(.*\)\..*"/\1/; 1q')

if [ "${SMOCKIN_JAVA_VERSION}" != 8 ]
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
