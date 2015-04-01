@echo off

adb push libs\armeabi\calc   system/bin
adb shell chmod 755 system/bin/calc