@echo off

REM   You can use more major version by removing %k and %l and %m.This command prompt version.
REM   for /f tokens^=2-5^ delims^=.-_^" %j in ('java -fullversion 2^>^&1') do @set "jver=%j%k%l%m"
REM   echo %jver%
IF DEFINED %SMOCKIN_DIR_PATH% (set APP_DIR_PATH=%SMOCKIN_DIR_PATH%) ELSE (set APP_DIR_PATH=%userprofile%\.smockin)

set APP_NAME=sMockin
set APP_VERSION=2.20.2


set DB_DIR_PATH=%APP_DIR_PATH%\db
set DB_DRIVER_DIR_PATH=%DB_DIR_PATH%\driver
set DB_DATA_DIR_PATH=%DB_DIR_PATH%\data
set DB_MARKER=db_initialized
set H2_JAR_NAME=h2-2.3.232.jar
set DB_PROPS_FILE=db.properties
set APP_PROPS_FILE=app.properties

IF NOT EXIST %APP_DIR_PATH% (
  echo Please run the install.sh script first to install required .smockin config to your user home
  exit /B
)


SETLOCAL ENABLEDELAYEDEXPANSION
SET count=1
FOR /F "tokens=3 USEBACKQ" %%F IN (`findstr "^[^#;]" %DB_DIR_PATH%\%DB_PROPS_FILE%`) DO (
  SET var!count!=%%F
  SET /a count=!count!+1
)
set DB_USERNAME=%var1%
set DB_PASSWORD=%var2%
set DRIVER_CLASS=%var3%
set JDBC_URL=%var4%
set HIBERNATE_DIALECT=%var5%
set MAX_POOL_SIZE=%var6%
set MIN_POOL_SIZE=%var7%


SET count=1
FOR /F "tokens=3 USEBACKQ" %%F IN (`findstr "^[^#;]" %APP_DIR_PATH%\%APP_PROPS_FILE%`) DO (
  SET var!count!=%%F
  SET /a count=!count!+1
)


IF DEFINED %1 (set APP_PORT=%1) ELSE (set APP_PORT=%var2%)

set H2_PORT=%var1%
set MULTI_USER_MODE_CONF=%var3%

set MULTI_USER_MODE=false

if "%MULTI_USER_MODE_CONF%"=="TRUE" (
  set MULTI_USER_MODE=true
)


call set MOD_DB_PATH1=%%userprofile:C:\=C:/%%
call set MOD_DB_PATH2=%%MOD_DB_PATH1:\=/%%
call set JDBC_URL=%%JDBC_URL:{USER.HOME}=%MOD_DB_PATH2%%%


echo #####################################################################################
echo # 
echo #  %APP_NAME%
echo #
echo #  v%APP_VERSION%
echo #


REM   Check for H2 DB Server driver and start it up (in TCP server mode) if not already running
if "%DRIVER_CLASS%"=="org.h2.Driver" (

  call set JDBC_URL=%%JDBC_URL:{H2.PORT}=%H2_PORT%%%

  echo #  Starting H2 TCP Database...
  start java -cp %DB_DRIVER_DIR_PATH%\%H2_JAR_NAME% org.h2.tools.Server -tcp -tcpAllowOthers -tcpPort %H2_PORT%

)


#
# Override any proxy environment variables, as this can cause 403 issues with internal S3 client and S3 mock server
#
set HTTP_PROXY=
set HTTPS_PROXY=


echo #  JDBC Connectivity Properties:
echo #  - JDBC DRIVER: %DRIVER_CLASS%
echo #  - JDBC URL: %JDBC_URL%
echo #


REM   Prepare runtime args

set VM_ARGS=--spring.datasource.url=%JDBC_URL%,--spring.datasource.username=%DB_USERNAME%,--spring.datasource.password=%DB_PASSWORD%,--spring.datasource.maximumPoolSize=%MAX_POOL_SIZE%,--spring.datasource.minimumIdle=%MIN_POOL_SIZE%,--user.timezone=UTC,--app.version=%APP_VERSION%
set APP_PROFILE=production


REM   START UP SMOCKIN APPLICATION
REM   (JPA WILL CREATE THE ACTUAL SMOCKIN DB AUTOMATICALLY IF IT DOES NOT ALREADY EXIST)
echo #
echo #  Starting Main Application...
echo #
echo #  Please Note:
echo #  - Application logs are available from: .smockin/log (under the user.home directory)
echo #  - Navigate to: 'http://localhost:%APP_PORT%/index.html' to access the Smockin Admin UI.

mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=%APP_PROFILE%,--multi.user.mode=%MULTI_USER_MODE%,--server.port=%APP_PORT%,%VM_ARGS%"
echo #
echo #####################################################################################


ENDLOCAL
