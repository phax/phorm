@echo off

call mvn clean install
if errorlevel 1 goto error

docker build --pull -t phelger/valsvc .
if errorlevel 1 goto error

goto end
:error
echo ERROR
:end
