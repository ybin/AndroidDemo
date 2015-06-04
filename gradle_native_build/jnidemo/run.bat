@echo off
setlocal
set path=%path%;%~dp0\build\binaries\jnidemoSharedLibrary
%~dp0\build\binaries\jnidemotestExecutable\jnidemotest.exe