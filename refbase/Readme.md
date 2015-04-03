# Strong pointer & weak pointer in Android native code

这是一个NDK项目，所有代码在NDK r10下编译验证通过。

### 代码说明

代码分为两部分：

1. 移植RefBase.cpp代码用于NDK编译
2. sp, wp测试代码

第一部分编译为librefbase.so，push到手机的/system/lib/目录下，
第二部分引用该so库。
