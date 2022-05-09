#
# Copyright (C) Philip Helger
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
RUN unzip valsvc.war -d /valsvc

# Stage 2

FROM tomcat:9-jdk11

ENV CATALINS_OPTS="$CATALINA_OPTS -Djava.security.egd=file:/dev/urandom"

WORKDIR $CATALINA_HOME/webapps

COPY --from=build /valsvc $CATALINA_HOME/webapps/ROOT
