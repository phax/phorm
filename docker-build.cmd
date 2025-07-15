@REM
@REM Copyright (C) 2022-2025 Philip Helger
@REM
@REM All rights reserved.
@REM

@echo off

docker build --pull -t phelger/valsvc .
if errorlevel 1 goto error

goto end
:error
echo ERROR
:end
