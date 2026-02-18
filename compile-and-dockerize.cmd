@REM
@REM Copyright (C) 2022-2026 Philip Helger
@REM
@REM All rights reserved.
@REM

@echo off

call mvn clean install
if errorlevel 1 goto error

docker build --pull -t phelger/valsvc .
if errorlevel 1 goto error

goto end
:error
echo ERROR
:end
