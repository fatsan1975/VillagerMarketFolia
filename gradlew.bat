@ECHO OFF
SETLOCAL

REM Text-only Gradle launcher.
REM Wrapper JAR is intentionally not committed to keep PR text-only.

WHERE gradle >NUL 2>NUL
IF %ERRORLEVEL%==0 (
  gradle %*
  EXIT /B %ERRORLEVEL%
)

echo ERROR: 'gradle' command not found in PATH. Install Gradle or restore gradle/wrapper/gradle-wrapper.jar.
EXIT /B 1
