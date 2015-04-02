# Understanding android binder: Java part

## 代码说明

实现一个跨进程的计算器(虽然它只有一个加法功能)。

1. `com.ybin.calc.calculatro`: binder部分
2. `com.ybin.calc.calcservice`: 服务端(使用Service)
3. `com.ybin.calc.calcclient`: 客户端(一个Activity)

其实完全可以使用AIDL来实现，但是我们为了掩饰binder的Java部分，特意手动
完成了AIDL的工作。

## 代码编译

此为一个Android studio module，直接import进你的project，然后编译即可。

详细的解释说明，请移步我的个人博客文章：[理解binder: native部分][java_binder]

[java_binder]: 