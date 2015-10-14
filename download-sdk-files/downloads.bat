::
:: download sdk files listed in sdk_urls.txt
:: --continue 端点续传
:: --no-clobber 如果文件已经存在就不再下载
:: --input-file=xxx 下载xxx文件每一行URL指定的文件
:: --base=xxx 为每一行URL添加一个base URL
:: --force-directories 根据URL创建文件路径
::

wget --continue --no-clobber --base=http://dl.google.com/android/repository/ --input-file=sdk_urls.txt --force-directories