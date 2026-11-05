@echo off
rem Met PingManager a jour depuis GitHub puis lance lapplication.
rem Double-cliquer sur ce fichier, ou le lancer depuis le dossier du projet.
cd /d "%~dp0.."
echo === Recuperation des dernieres fusions ===
git checkout main
if errorlevel 1 goto erreur
git pull
if errorlevel 1 goto erreur
echo.
echo === Ce qui vient detre fusionne (journaux des taches) ===
dir /b /o-d docs\journal\20*.md 2>nul
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
