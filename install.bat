@echo off

set APP_DIR_PATH=%userprofile%\.smockin
set DB_DIR_PATH=%APP_DIR_PATH%\db
set DB_DRIVER_DIR_PATH=%DB_DIR_PATH%\driver
set DB_DATA_DIR_PATH=%DB_DIR_PATH%\data

set H2_JAR_NAME=h2-1.4.194.jar
set DB_PROPS_FILE=db.properties
set APP_PROPS_FILE=app.properties

IF EXIST %DB_DIR_PATH% (

  echo Smockin is already installed : '%APP_DIR_PATH%'

) ELSE (

  echo Installing .smockin config directory to user home...

  mkdir %DB_DRIVER_DIR_PATH%
  echo - Created directory %DB_DRIVER_DIR_PATH%
  mkdir %DB_DATA_DIR_PATH%
  echo - Created directory %DB_DATA_DIR_PATH%

  copy    install\%H2_JAR_NAME%       %DB_DRIVER_DIR_PATH%\%H2_JAR_NAME%
  echo - Added file %DB_DRIVER_DIR_PATH\%H2_JAR_NAME%
  copy    install\%DB_PROPS_FILE%     %DB_DIR_PATH%\%DB_PROPS_FILE%
  echo - Added file %DB_DIR_PATH%\%DB_PROPS_FILE%
  copy    install\%APP_PROPS_FILE%     %APP_DIR_PATH%
  echo - Added file %APP_DIR_PATH%\%APP_PROPS_FILE%

  echo The default H2 DB has been installed

  echo   Please run start.bat to launch the Smockin Application

)
