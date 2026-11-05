@echo off
chcp 65001 >nul
rem Met PingManager a jour depuis GitHub, affiche ce qui a ete fait, puis lance l'application.
rem Double-cliquer sur ce fichier, ou le lancer depuis le dossier du projet.
cd /d "%~dp0.."
echo === Recuperation des dernieres fusions ===
git checkout main
if errorlevel 1 goto erreur
for /f %%i in ('git rev-parse HEAD') do set AVANT=%%i
git pull
if errorlevel 1 goto erreur
for /f %%i in ('git rev-parse HEAD') do set APRES=%%i
echo.
if "%AVANT%"=="%APRES%" (
  echo Rien de nouveau depuis la derniere mise a jour.
  goto lancer
)
echo === Ce qui a ete fait depuis ta derniere mise a jour ===
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$f = git diff --name-only --diff-filter=AM %AVANT% %APRES% -- 'docs/journal/20*.md';" ^
  "if (-not $f) { git log --format='- %%s' %AVANT%..%APRES% --first-parent; exit }" ^
  "foreach ($j in $f) {" ^
  "  $t = Get-Content -Encoding UTF8 $j;" ^
  "  Write-Host ''; Write-Host ('### ' + ($t[0] -replace '^# ','')) -ForegroundColor Cyan;" ^
  "  $dedans = $false;" ^
  "  foreach ($l in $t) {" ^
  "    if ($l -match '^## ') { $dedans = ($l -match 'Pour Quentin|tester|Questions'); if ($dedans) { Write-Host $l -ForegroundColor Yellow }; continue }" ^
  "    if ($dedans -and $l.Trim()) { Write-Host $l }" ^
  "  }" ^
  "}"
echo.
echo Detail complet : dossier docs\journal\  -  point d'ensemble : RECAPITULATIF.md
:lancer
echo.
echo === Lancement de PingManager ===
call mvn clean javafx:run
goto fin
:erreur
echo.
echo La mise a jour a echoue : des fichiers modifies sur ce PC bloquent peut-etre le pull.
echo Lancer "git status" pour voir lesquels.
:fin
pause
