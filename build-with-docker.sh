#!/bin/sh
#
# Copyright (C) 2022-2026 Philip Helger
#
# All rights reserved.
#

docker run --rm --name my-maven-project -v "$HOME/.m2":/root/.m2 -v "$(pwd)":/usr/src/mymaven -w /usr/src/mymaven maven:3-eclipse-temurin-21 mvn "$@"

