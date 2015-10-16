::
:: download sdk files listed in sdk file list.
:: --continue 端点续传
:: --no-clobber 如果文件已经存在就不再下载
:: --input-file=xxx 下载xxx文件每一行URL指定的文件
:: --base=xxx 为每一行URL添加一个base URL
:: --force-directories 根据URL创建文件路径
::

set file_list=sdk_file_list.txt
::set base_url=https://dl.google.com/android/repository/
set base_url=http://mirrors.opencas.cn/android/repository/

wget --continue --no-clobber --base=%base_url% --input-file=%file_list% --force-directories