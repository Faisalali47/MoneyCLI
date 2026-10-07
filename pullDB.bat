@echo off
echo.
echo Pulling Money CLI database from phone...
echo.

C:\Android\Sdk\platform-tools\adb.exe exec-out run-as com.moneycli cat databases/money_cli.db > "%~dp0money_cli.db"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Database updated successfully.
    echo.
) else (
    echo.
    echo Failed to pull database.
    echo Make sure the phone is connected and USB debugging is enabled.
    echo.
)

pause
