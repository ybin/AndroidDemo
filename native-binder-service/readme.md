# A android native binder service demo

���demo����[Binder Demo][demo1]�Լ�[android system service example][demo2]����demo������������ˣ�
����`deps/include/`����ֱ��ȡ��[android system service example][demo2]���ڴ�����λ������л��

# ���뷽ʽ

ʹ��Android NDK r10����ͨ������debug�汾���ֻ�����֤ͨ��������NDK�汾û�в��ԡ�

# ����˵��

1. `deps/include/`: ��AndroidԴ���и��ƶ�������Ҫ��`system/core/include`�Լ�`native/include`��
�ò��ִ���ȡ��[android system service example][demo2]��Ŀ��
2. `deps/libs/`: ��Android���������.so�⣬��Ȼֱ�Ӵ��ֻ��и��Ƴ���Ҳ����ʹ�á�
3. `include/`��`src/`���������壬demo���߼��ڴ�ʵ��
4. `test/`: ���Դ��룬��Ҫ������demo��service���Լ������ͻ�������service����Ϊ��ʾʹ�ã���һ����ִ���ļ���

# ʹ�÷���

1. ʹ��NDK����֮��������Ϊ`calc`�Ŀ�ִ���ļ���
2. `adb push calc  /system/bin`
3. `adb shell chomd 755 /system/bin/calc`



[demo1]: https://github.com/gburca/BinderDemo
[demo2]: https://github.com/qianjigui/android_system_service_example