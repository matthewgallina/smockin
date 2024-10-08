@echo off

IF DEFINED %1 (
    set APP_DIR_PATH=%1\.smockin
    
) ELSE (
    set APP_DIR_PATH=%userprofile%\.smockin
)

setx SMOCKIN_DIR_PATH = %APP_DIR_PATH%

set DB_DIR_PATH=%APP_DIR_PATH%\db
set DB_DRIVER_DIR_PATH=%DB_DIR_PATH%\driver
set DB_DATA_DIR_PATH=%DB_DIR_PATH%\data

set H2_JAR_NAME=h2-2.3.232.jar
set DB_PROPS_FILE=db.properties
set APP_PROPS_FILE=app.properties
set DB_PLACEHOLDER_FILE=smockin_db.mv.db

IF EXIST %DB_DIR_PATH% (

  echo sMockin is already installed : '%APP_DIR_PATH%'

) ELSE (

  echo Installing .smockin config directory to user home...

  mkdir %DB_DRIVER_DIR_PATH%
  echo - Created directory %DB_DRIVER_DIR_PATH%
  mkdir %DB_DATA_DIR_PATH%
  echo - Created directory %DB_DATA_DIR_PATH%

  copy    install\%H2_JAR_NAME%     %DB_DRIVER_DIR_PATH%\%H2_JAR_NAME%
  echo - Added file %DB_DRIVER_DIR_PATH\%H2_JAR_NAME%
  copy    install\%DB_PROPS_FILE%     %DB_DIR_PATH%\%DB_PROPS_FILE%
  echo - Added file %DB_DIR_PATH%\%DB_PROPS_FILE%
  copy    install\%APP_PROPS_FILE%     %APP_DIR_PATH%
  echo - Added file %APP_DIR_PATH%\%APP_PROPS_FILE%
  copy    install\%DB_PLACEHOLDER_FILE%     %DB_DATA_DIR_PATH%\%DB_PLACEHOLDER_FILE%
  echo - Added file %DB_DATA_DIR_PATH%\%DB_PLACEHOLDER_FILE%

  echo The default H2 DB has been installed

  echo   Please run start.bat to launch the Smockin Application

)
