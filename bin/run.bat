@echo off
setlocal enabledelayedexpansion
title DTAQ Retail - Build and Run

rem Chay tu thu muc goc du an de doc duoc db.properties
cd /d "%~dp0"

echo ==================================================
echo  DTAQ Retail - tu dong build, ket noi CSDL, chay UI
echo ==================================================

rem ---------- 1. Tim JDK 21 ----------
set "JDK21="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" call :checkjdk "%JAVA_HOME%"
if not defined JDK21 (
  for /d %%D in ("%USERPROFILE%\.jdks\*21*") do if exist "%%~D\bin\java.exe" set "JDK21=%%~D"
)
if not defined JDK21 (
  for /d %%D in ("%ProgramFiles%\Java\*21*" "%ProgramFiles%\Eclipse Adoptium\*21*" "%ProgramFiles%\Microsoft\*21*") do if exist "%%~D\bin\java.exe" set "JDK21=%%~D"
)
if not defined JDK21 (
  echo [LOI] Khong tim thay JDK 21. Hay cai JDK 21 hoac dat JAVA_HOME tro toi JDK 21.
  goto :fail
)
set "JAVA_HOME=%JDK21%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [OK] JDK 21: %JAVA_HOME%

rem ---------- 2. Kiem tra Maven ----------
where mvn >nul 2>&1
if errorlevel 1 (
  echo [LOI] Khong tim thay Maven ^(mvn^) tren PATH. Cai Maven 3.9+ va thu lai.
  goto :fail
)
echo [OK] Maven san sang

rem ---------- 3. Cau hinh CSDL ----------
if not exist "db.properties" (
  if exist "db.properties.example" (
    copy /y "db.properties.example" "db.properties" >nul
    echo [!] Chua co db.properties - da tao tu db.properties.example.
    echo     Hay dien DTAQ_DB_PASSWORD ^(mat khau PostgreSQL^) roi chay lai file nay.
    start notepad "db.properties"
    goto :fail
  ) else (
    echo [LOI] Thieu db.properties va db.properties.example.
    goto :fail
  )
)

set "DB_URL=jdbc:postgresql://localhost:5432/dtaq_retail"
set "DB_USER=postgres"
set "DB_PASS="
for /f "usebackq tokens=1,* delims==" %%A in ("db.properties") do (
  if "%%A"=="DTAQ_DB_URL" set "DB_URL=%%B"
  if "%%A"=="DTAQ_DB_USER" set "DB_USER=%%B"
  if "%%A"=="DTAQ_DB_PASSWORD" set "DB_PASS=%%B"
)
rem Bien moi truong (neu co) uu tien hon db.properties, giong cach ung dung doc cau hinh
if defined DTAQ_DB_URL set "DB_URL=%DTAQ_DB_URL%"
if defined DTAQ_DB_USER set "DB_USER=%DTAQ_DB_USER%"
if defined DTAQ_DB_PASSWORD set "DB_PASS=%DTAQ_DB_PASSWORD%"

rem Tach host, port, ten database tu jdbc:postgresql://host:port/db
set "DB_REST=%DB_URL:jdbc:postgresql://=%"
for /f "tokens=1,2,3 delims=:/?" %%H in ("%DB_REST%") do (
  set "DB_HOST=%%H"
  set "DB_PORT=%%I"
  set "DB_NAME=%%J"
)
if not defined DB_PORT set "DB_PORT=5432"
if not defined DB_NAME set "DB_NAME=dtaq_retail"
rem Truong hop URL khong co port: host/db
echo %DB_PORT%| findstr /R "^[0-9][0-9]*$" >nul || (set "DB_NAME=%DB_PORT%" & set "DB_PORT=5432")

rem ---------- 4. Kiem tra ket noi PostgreSQL (va tao CSDL neu thieu) ----------
rem Co danh dau "da thiet lap xong" luu ngoai du an (mvn clean khong xoa duoc)
rem Xoa file nay neu doi mat khau / doi CSDL va muon kiem tra lai tu dau.
set "STATE_DIR=%LOCALAPPDATA%\DTAQ-Retail"
set "DB_FLAG=%STATE_DIR%\db_ready_%DB_HOST%_%DB_PORT%_%DB_NAME%.flag"
if not exist "%STATE_DIR%" mkdir "%STATE_DIR%" >nul 2>&1

if exist "%DB_FLAG%" (
  echo [OK] CSDL "%DB_NAME%" da thiet lap tu truoc - bo qua buoc kiem tra/tao.
  call :startpg
  goto :build
)

set "PSQL="
where psql >nul 2>&1 && set "PSQL=psql"
if not defined PSQL (
  for /d %%D in ("%ProgramFiles%\PostgreSQL\*") do if exist "%%~D\bin\psql.exe" set "PSQL=%%~D\bin\psql.exe"
)
if not defined PSQL (
  echo [!] Khong tim thay psql - bo qua buoc kiem tra truoc, ung dung se tu ket noi khi chay.
  goto :build
)

call :startpg
set "PGPASSWORD=%DB_PASS%"
set "PGCONNECT_TIMEOUT=5"
"%PSQL%" -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -Atc "select 1" >"%TEMP%\dtaq_pg_check.log" 2>&1
if errorlevel 1 (
  echo [LOI] Khong ket noi duoc PostgreSQL tai %DB_HOST%:%DB_PORT% voi user "%DB_USER%":
  type "%TEMP%\dtaq_pg_check.log"
  echo.
  echo     - Kiem tra dich vu PostgreSQL da chay chua ^(services.msc^).
  echo     - Kiem tra DTAQ_DB_USER / DTAQ_DB_PASSWORD trong db.properties.
  start notepad "db.properties"
  goto :fail
)
echo [OK] Ket noi PostgreSQL thanh cong ^(%DB_HOST%:%DB_PORT%^)

rem Ghi ket qua truy van ra file roi doc lai (tranh loi dau nhay khi dung for /f voi lenh)
"%PSQL%" -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -Atc "select 1 from pg_database where datname='%DB_NAME%'" >"%TEMP%\dtaq_pg_exists.log" 2>&1
set "DB_EXISTS="
findstr /X /C:"1" "%TEMP%\dtaq_pg_exists.log" >nul && set "DB_EXISTS=1"
if not defined DB_EXISTS (
  echo [*] Chua co CSDL "%DB_NAME%" - dang tao...
  "%PSQL%" -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "CREATE DATABASE \"%DB_NAME%\"" >"%TEMP%\dtaq_pg_create.log" 2>&1
  if errorlevel 1 (
    findstr /C:"already exists" "%TEMP%\dtaq_pg_create.log" >nul
    if errorlevel 1 (
      echo [LOI] Khong tao duoc CSDL "%DB_NAME%":
      type "%TEMP%\dtaq_pg_create.log"
      goto :fail
    )
  )
)
echo [OK] CSDL "%DB_NAME%" san sang ^(bang se do ung dung/Flyway tao khi khoi dong^)
set "PGPASSWORD="
echo ready> "%DB_FLAG%"

:build
rem ---------- 5. Build ----------
echo.
echo [*] Dang build du an ^(bo qua test^)...
call mvn -B -q clean package -DskipTests
if errorlevel 1 (
  echo [LOI] Build that bai. Xem thong bao phia tren.
  goto :fail
)

set "JAR=target\dtaq-retail-1.0.0-SNAPSHOT.jar"
if not exist "%JAR%" (
  echo [LOI] Khong thay %JAR% sau khi build.
  goto :fail
)
echo [OK] Build xong: %JAR%

rem ---------- 6. Chay ung dung (tu ket noi CSDL + chay Flyway migration) ----------
echo.
echo [*] Dang khoi dong ung dung... ^(dong cua so nay se tat ung dung^)
"%JAVA_HOME%\bin\java.exe" -jar "%JAR%"
if errorlevel 1 (
  echo [LOI] Ung dung thoat voi ma loi %errorlevel%.
  goto :fail
)
echo Ung dung da dong.
endlocal
exit /b 0

:checkjdk
"%~1\bin\java.exe" -version 2>&1 | findstr /C:" version" | findstr /C:"21." >nul
if not errorlevel 1 set "JDK21=%~1"
goto :eof

rem Khoi dong dich vu PostgreSQL cuc bo neu chua chay (can quyen Administrator moi khoi dong duoc)
:startpg
powershell -NoProfile -Command "Get-Service -Name 'postgresql*' -ErrorAction SilentlyContinue | Where-Object { $_.Status -ne 'Running' } | ForEach-Object { Write-Host ('[*] Dang khoi dong dich vu ' + $_.Name); try { Start-Service -InputObject $_ -ErrorAction Stop } catch { Write-Host '[!] Khong khoi dong duoc - hay mo PostgreSQL thu cong hoac chay run.bat bang Run as administrator.' } }"
goto :eof

:fail
echo.
pause
endlocal
exit /b 1
