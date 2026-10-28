@echo off
set DB=restaurante_db
set USER=root
set PORT=3307
if not exist backups mkdir backups
for /f "tokens=1-4 delims=/ " %%a in ('date /t') do set FECHA=%%d-%%b-%%c
set HORA=%time:~0,2%-%time:~3,2%
set HORA=%HORA: =0%
mysqldump -P %PORT% -u %USER% -p %DB% > backups\backup_%DB%_%FECHA%_%HORA%.sql
echo Backup terminado.
pause
