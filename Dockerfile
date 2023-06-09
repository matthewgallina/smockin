# Dockerfile

#
# docker build -t smockin-2200 .
# docker tag smockin-2200 mgallina/smockin:2200
# docker push mgallina/smockin:2200
# docker run --name smockin -d -p 8000:8000 -p 8001:8001 -p 8002:8002 -p 8003:8003 mgallina/smockin:2200
#

FROM adoptopenjdk/openjdk11:jre-11.0.19_7-ubuntu
ARG APP_VERSION_ARG='2.20.0'
RUN mkdir /app
RUN mkdir /app/db
RUN mkdir /app/db/data
RUN mkdir /app/db/driver
RUN mkdir /app/log
WORKDIR /app
COPY install/h2-2.1.212.jar /app/db/driver/h2-2.1.212.jar
COPY install/smockin_db.mv.db /app/db/data/smockin_db.mv.db
COPY target/smockin-${APP_VERSION_ARG}.jar /app/smockin-${APP_VERSION_ARG}.jar
COPY launch.sh /app/launch.sh
EXPOSE 8000
EXPOSE 8001
EXPOSE 8002
EXPOSE 8003
RUN ["chmod", "+x", "/app/launch.sh"]
ENTRYPOINT ["/app/launch.sh"]
