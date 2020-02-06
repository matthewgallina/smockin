# Dockerfile
FROM adoptopenjdk/openjdk8:jre8u242-b08-alpine

ARG APP_VERSION_ARG='2.4.0'
RUN mkdir -p /app/db/data
RUN mkdir /app/db/driver
RUN mkdir /app/log
WORKDIR /app
COPY install/h2-1.4.194.jar /app/db/driver/h2-1.4.194.jar
COPY target/smockin-${APP_VERSION_ARG}.jar /app/smockin-${APP_VERSION_ARG}.jar
COPY install/launch.sh /app/launch.sh
EXPOSE 8000
EXPOSE 8001
EXPOSE 8002
EXPOSE 8003
EXPOSE 8010
EXPOSE 9092
RUN ["chmod", "+x", "/app/launch.sh"]
ENTRYPOINT ["/app/launch.sh"]
