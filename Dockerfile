FROM maven:3.3-jdk-8 as BUILD
RUN mkdir /app
WORKDIR /app
ADD pom.xml /app/pom.xml
RUN mvn install
ADD src /app/src
RUN mvn package

FROM openjdk:8-jre as RUN
ARG APP_VERSION_ARG=2.19.0
RUN mkdir /app
RUN mkdir /app/db
RUN mkdir /app/db/data
RUN mkdir /app/db/driver
RUN mkdir /app/log
WORKDIR /app
COPY install/h2-2.1.212.jar /app/db/driver/h2-2.1.212.jar
COPY install/smockin_db.mv.db /app/db/data/smockin_db.mv.db
COPY --from=BUILD /app/target/smockin-${APP_VERSION_ARG}.jar /app/smockin-${APP_VERSION_ARG}.jar
COPY launch.sh /app/launch.sh
EXPOSE 8000
EXPOSE 8001
EXPOSE 8002
EXPOSE 8003

ENV APP_VERSION=${APP_VERSION_ARG}
ENV spring_database_driverClassName=org.h2.Driver
ENV spring_datasource_url="jdbc:h2:tcp://localhost:9092//app/db/data/smockin_db"
ENV spring_datasource_username=smockin
ENV spring_datasource_password=smockin
ENV spring_datasource_maximumPoolSize=10
ENV spring_datasource_minimumIdle=10
ENV SERVER_PORT=8000
ENV MULTI_USER_MODE=FALSE
ENV logging_file=/app/log/smockin.log

RUN chmod +x /app/launch.sh
ENTRYPOINT ["/app/launch.sh"]