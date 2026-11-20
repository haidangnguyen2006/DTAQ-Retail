@echo off
rem Xuat anh moi man hinh (2x) + screens.json cho plugin Figma. Can PostgreSQL dang chay.
cd /d "%~dp0\..\.."
call mvn -q -DskipTests package || exit /b 1
java -Dsun.java2d.uiScale=1 -cp "target\test-classes;target\dtaq-retail-1.0.0-SNAPSHOT.jar" com.dtaq.retail.ui.FigmaExport tools\figma-plugin\screens %*
