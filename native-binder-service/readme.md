# A android native binder service demo

这个demo是受[Binder Demo][demo1]以及[android system service example][demo2]两个demo的启发而完成了，
其中`deps/include/`部分直接取自[android system service example][demo2]，在此向两位作者致谢。

# 编译方式

使用Android NDK r10编译通过，在debug版本的手机上验证通过，其他NDK版本没有测试。

# 代码说明

1. `deps/include/`: 从Android源码中复制而来，主要是`system/core/include`以及`native/include`，
该部分代码取自[android system service example][demo2]项目。
2. `deps/libs/`: 从Android编译出来的.so库，当然直接从手机中复制出来也可以使用。
3. `include/`、`src/`：代码主体，demo的逻辑在此实现
4. `test/`: 测试代码，主要是启动demo的service，以及启动客户端连接service，作为演示使用，是一个可执行文件。

# 使用方法

1. 使用NDK编译之后生成名为`calc`的可执行文件，
2. `adb push calc  /system/bin`
3. `adb shell chomd 755 /system/bin/calc`



[demo1]: https://github.com/gburca/BinderDemo
[demo2]: https://github.com/qianjigui/android_system_service_example