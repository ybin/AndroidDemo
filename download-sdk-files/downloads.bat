::
:: download sdk files listed in sdk_urls.txt
:: --continue �˵�����
:: --no-clobber ����ļ��Ѿ����ھͲ�������
:: --input-file=xxx ����xxx�ļ�ÿһ��URLָ�����ļ�
:: --base=xxx Ϊÿһ��URL���һ��base URL
:: --force-directories ����URL�����ļ�·��
::

wget --continue --no-clobber --base=http://dl.google.com/android/repository/ --input-file=sdk_urls.txt --force-directories