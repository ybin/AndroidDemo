# Strong pointer & weak pointer in Android native code

这是一个NDK项目，所有代码在NDK r10下编译验证通过。

### 代码说明

代码分为两部分：

1. 移植RefBase.cpp代码用于NDK编译
2. 移植RefBase.cpp及相关文件到Linux上

第一部分编译为librefbase.so，push到手机的/system/lib/目录下，executable程序直接引用该库，
makefile文件为Android.mkk。

第二部分把RefBase.cpp及相关代码单独拎出来，主要是atomic操作移植比较困难，当然如果使用C++11
的话直接改一下函数名称即可，我们这里的做法是做一个fake atomic操作集合，因为我们的代码
只作为演示使用，不涉及线程安全性，所以完全可以做一个假的原子操作集合，在Linux上直接编译即可，
```bash
g++ -I. *
```
