# Strong pointer & weak pointer in Android native code

����һ��NDK��Ŀ�����д�����NDK r10�±�����֤ͨ����

### ����˵��

�����Ϊ�����֣�

1. ��ֲRefBase.cpp��������NDK����
2. ��ֲRefBase.cpp������ļ���Linux��

��һ���ֱ���Ϊlibrefbase.so��push���ֻ���/system/lib/Ŀ¼�£�executable����ֱ�����øÿ⣬
makefile�ļ�ΪAndroid.mkk��

�ڶ����ְ�RefBase.cpp����ش��뵥�����������Ҫ��atomic������ֲ�Ƚ����ѣ���Ȼ���ʹ��C++11
�Ļ�ֱ�Ӹ�һ�º������Ƽ��ɣ������������������һ��fake atomic�������ϣ���Ϊ���ǵĴ���
ֻ��Ϊ��ʾʹ�ã����漰�̰߳�ȫ�ԣ�������ȫ������һ���ٵ�ԭ�Ӳ������ϣ���Linux��ֱ�ӱ��뼴�ɣ�
```bash
g++ -I. *
```
