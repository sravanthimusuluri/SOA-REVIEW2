CREATE DATABASE IF NOT EXISTS bibliotech_auth;
CREATE DATABASE IF NOT EXISTS bibliotech_books;
CREATE DATABASE IF NOT EXISTS bibliotech_rentals;
CREATE DATABASE IF NOT EXISTS bibliotech_fines;

CREATE USER IF NOT EXISTS 'bibliotech'@'localhost' IDENTIFIED BY 'bibliotech';
CREATE USER IF NOT EXISTS 'bibliotech'@'%' IDENTIFIED BY 'bibliotech';
GRANT ALL PRIVILEGES ON bibliotech_auth.* TO 'bibliotech'@'localhost';
GRANT ALL PRIVILEGES ON bibliotech_books.* TO 'bibliotech'@'localhost';
GRANT ALL PRIVILEGES ON bibliotech_rentals.* TO 'bibliotech'@'localhost';
GRANT ALL PRIVILEGES ON bibliotech_fines.* TO 'bibliotech'@'localhost';
GRANT ALL PRIVILEGES ON bibliotech_auth.* TO 'bibliotech'@'%';
GRANT ALL PRIVILEGES ON bibliotech_books.* TO 'bibliotech'@'%';
GRANT ALL PRIVILEGES ON bibliotech_rentals.* TO 'bibliotech'@'%';
GRANT ALL PRIVILEGES ON bibliotech_fines.* TO 'bibliotech'@'%';
FLUSH PRIVILEGES;
