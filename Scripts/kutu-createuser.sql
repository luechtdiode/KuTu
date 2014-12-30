CREATE USER 'kutu'@'localhost';

UPDATE mysql.user SET Password=PASSWORD('kutu') WHERE User='kutu' AND Host='localhost';
GRANT Usage ON *.* TO 'kutu'@'localhost';
GRANT Delete ON kutu.* TO 'kutu'@'localhost';
GRANT Insert ON kutu.* TO 'kutu'@'localhost';
GRANT References ON kutu.* TO 'kutu'@'localhost';
GRANT Select ON kutu.* TO 'kutu'@'localhost';
GRANT Show view ON kutu.* TO 'kutu'@'localhost';
GRANT Trigger ON kutu.* TO 'kutu'@'localhost';
GRANT Update ON kutu.* TO 'kutu'@'localhost';
GRANT Execute ON kutu.* TO 'kutu'@'localhost';
GRANT Create temporary tables ON kutu.* TO 'kutu'@'localhost';
FLUSH PRIVILEGES;
