@echo off
setlocal
set SRV=http://localhost:8080
curl -d "@src/test/resources/testfiles/peppol-bis3/base-example.xml" -H "Content-Type: application/xml" -H "X-Token: 4cKyX6OKBs80nWPyOamn" -X POST "%SRV%/api/validate/eu.peppol.bis3:invoice:latest"
