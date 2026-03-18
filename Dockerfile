#
# Copyright (C) 2022-2026 Philip Helger (www.helger.com)
# philip[at]helger[dot]com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Stage 1

FROM ubuntu:latest AS build

# Install wget and unzip
RUN apt-get update \
  && apt-get install -y unzip \
  && rm -rf /var/lib/apt/lists/*

COPY target/*.war phorm.war
RUN unzip phorm.war -d /phorm \
  && rm /phorm/WEB-INF/classes/application.properties \
  && mv /phorm/WEB-INF/classes/application.docker.properties /phorm/WEB-INF/classes/application.properties


# Stage 2

FROM tomcat:10.1-jdk21

ENV CATALINS_OPTS="-Djava.security.egd=file:/dev/urandom"

WORKDIR $CATALINA_HOME/webapps

COPY --from=build /phorm $CATALINA_HOME/webapps/ROOT
