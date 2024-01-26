@REM
@REM Copyright (C) 2022-2024 Philip Helger
@REM
@REM All rights reserved.
@REM

@echo off
::docker run -it --rm --name my-maven-project -v "$(pwd)":/usr/src/mymaven -w /usr/src/mymaven maven:3-eclipse-temurin-11 %*
docker run -it --rm --name my-maven-project -v "%CD%":/usr/src/mymaven -w /usr/src/mymaven maven:3-eclipse-temurin-11 mvn %*
