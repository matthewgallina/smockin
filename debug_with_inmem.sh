#!/bin/sh

APP_VERSION="2.18.4"

mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dapp.version=$APP_VERSION -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8008"
