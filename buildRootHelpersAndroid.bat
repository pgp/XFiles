setlocal enableextensions enabledelayedexpansion
REM TODO set -e

SET "NDKDIR=%appdata%\..\Local\Android\Sdk\ndk-bundle"
SET "RHDIR=%cd%\XFilesRootHelper"
SET "XFILES_ASSET_DIR=%cd%\libs"
md %XFILES_ASSET_DIR%

if exist %RHDIR%\NUL (echo rh directory exists) else (git clone https://github.com/pgp/XFilesRootHelper.git)

cd %RHDIR%
SET "MAINDIR=%cd%"

set "FORMAT7ZDIR=%MAINDIR%\ANDROID\Format7zFree\jni"
set "RHDIR=%MAINDIR%\ANDROID\RootHelper\jni"

set "FORMAT7ZLIBDIR=%MAINDIR%\ANDROID\Format7zFree\libs"
set "RHLIBDIR=%MAINDIR%\ANDROID\RootHelper\libs"

set "TLSCERTDIR=%MAINDIR%\cert"

REM build lib7z.so
cd %FORMAT7ZDIR%
REM rd /s /q ..\obj\*
REM rd /s /q ..\libs\*
if defined LOWPERF (call %NDKDIR%\ndk-build -j2) else (call %NDKDIR%\ndk-build -j4)

REM build roothelper executable shared object (r)
cd %RHDIR%
REM rd /s /q ..\obj\*
REM rd /s /q ..\libs\*
if defined LOWPERF (call %NDKDIR%\ndk-build -j2) else (call %NDKDIR%\ndk-build -j4)

REM rename to libr.so (for gradle to accept it as embeddable in apk)
cd %RHLIBDIR%
for /D %%i in (*) do ( cd %%i & ren r libr.so & cd..)

REM copy libraries

robocopy %RHLIBDIR% %XFILES_ASSET_DIR% /s /e
robocopy %FORMAT7ZLIBDIR% %XFILES_ASSET_DIR% /s /e

cd %XFILES_ASSET_DIR%
for /D %%i in (*) do ( cd %%i & copy %TLSCERTDIR%\dummycrt.pem libdummycrt.so /Y & copy %TLSCERTDIR%\dummykey.pem libdummykey.so /Y & cd..)
