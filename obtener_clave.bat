@echo off
chcp 65001 >nul
echo ========================================================
echo Extraccion de Auth Key para Amazfit GTR 3 (Zepp)
echo ========================================================
echo.
set /p ZEPP_EMAIL="Introduce tu correo de Zepp: "
echo.
echo Introduce tu contrasena (los caracteres estaran ocultos):

set HUAMI_BIN=""
where huami-token.exe >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set HUAMI_BIN=huami-token.exe
) else if exist "%APPDATA%\Python\Python314\Scripts\huami-token.exe" (
    set HUAMI_BIN="%APPDATA%\Python\Python314\Scripts\huami-token.exe"
) else (
    set HUAMI_BIN=python -m huami_token
)

%HUAMI_BIN% -m amazfit -e "%ZEPP_EMAIL%" -b
echo.
pause
