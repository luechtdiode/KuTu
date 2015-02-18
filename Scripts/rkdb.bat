
mysql.exe < %1\kutu-createuser.sql -uroot -p%2 --port=36551
mysql.exe < %1\kutu-ddl.sql -uroot -p%2 --port=36551
mysql.exe < %1\kutu-initialdata.sql -uroot -p%2 --port=36551