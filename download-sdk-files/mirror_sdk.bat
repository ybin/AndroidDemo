::
:: download sdk files listed in sdk file list.
:: --continue �˵�����
:: --no-clobber ����ļ��Ѿ����ھͲ�������
:: --input-file=xxx ����xxx�ļ�ÿһ��URLָ�����ļ�
:: --base=xxx Ϊÿһ��URL���һ��base URL
:: --force-directories ����URL�����ļ�·��
::

set file_list=sdk_file_list.txt
::set base_url=https://dl.google.com/android/repository/
set base_url=http://mirrors.opencas.cn/android/repository/

wget --continue --no-clobber --base=%base_url% --input-file=%file_list% --force-directories