#
# Copyright (C) 2022-2024 Philip Helger
#
# All rights reserved.
#

# Stage 1

FROM ubuntu:latest as build

# Install wget and unzip
RUN apt-get update \
  && apt-get install -y unzip \
  && rm -rf /var/lib/apt/lists/*

COPY target/*.war valsvc.war
RUN unzip valsvc.war -d /valsvc \
  && rm /valsvc/WEB-INF/classes/application.properties \
  && mv /valsvc/WEB-INF/classes/application.docker.properties /valsvc/WEB-INF/classes/application.properties


# Stage 2

FROM tomcat:10.1-jdk11

ENV CATALINS_OPTS="$CATALINA_OPTS -Djava.security.egd=file:/dev/urandom"

WORKDIR $CATALINA_HOME/webapps

COPY --from=build /valsvc $CATALINA_HOME/webapps/ROOT
