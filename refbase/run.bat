@echo off

adb push libs\armeabi\testsp system/bin > nul 2>&1
adb shell chmod 755 system/bin/testsp
adb shell testsp