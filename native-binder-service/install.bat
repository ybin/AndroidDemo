@echo off

adb push D:\sunyanbin10130990\AndroidstudioProjects\nativeservice\libs\armeabi\_calc system/bin
adb shell chmod 755 system/bin/_calc