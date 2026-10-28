@echo off
if "%~1"=="" (
  echo Uso: restaurar_mysql.bat ruta\backup.sql
  pause
  exit /b 1
)
mysql -P 3307 -u root -p restaurante_db < "%~1"
echo Restauracion finalizada.
pause
