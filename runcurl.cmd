@REM
@REM Copyright (C) 2022-2026 Philip Helger
@REM
@REM All rights reserved.
@REM

@echo off
setlocal
set SRV=http://localhost:8080
curl -d "@src/test/resources/testfiles/peppol-bis3/base-example.xml" -H "Content-Type: application/xml" -H "X-Token: 4cKyX6OKBs80nWPyOamn" -X POST "%SRV%/api/validate/eu.peppol.bis3:invoice:latest"
