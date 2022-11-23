@echo off

docker run -d --name valsvc -p 8080:8080 phelger/valsvc
if errorlevel 1 goto error

goto end
:error
echo ERROR
:end
