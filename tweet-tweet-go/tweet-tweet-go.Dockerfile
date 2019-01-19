FROM maven:3.6.0-jdk-11-slim as build
VOLUME $HOME/.m2:/root/.m2

RUN mkdir --parents /usr/src/app
WORKDIR /usr/src/app

ADD pom.xml /usr/src/app
RUN mvn verify clean --fail-never

ADD . /usr/src/app
RUN mvn install -DskipTests

FROM adoptopenjdk/openjdk11:jdk11u-alpine-nightly-slim
COPY --from=build /usr/src/app/target/progetto-ir-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --from=build /usr/src/app/src/main/resources /app/users
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]