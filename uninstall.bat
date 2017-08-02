@echo off

set APP_DIR_PATH=%userprofile%\.smockin

REM    DELETE THE '.smockin' DIRECTORY

IF EXIST %DB_DIR_PATH% (

  echo Removing %APP_DIR_PATH% in user home...

  del "%APP_DIR_PATH%" /q
  FOR /D %%p IN ("%APP_DIR_PATH%*.*") DO rmdir "%%p" /s /q

  echo *** Smockin has been uninstalled ***

) ELSE (

  echo Smockin is not installed...

)
