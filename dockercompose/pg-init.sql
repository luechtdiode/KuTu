CREATE USER kutuadmin;
CREATE ROLE kutu_admin WITH SUPERUSER USER kutuadmin;
CREATE SCHEMA IF NOT EXISTS kutu AUTHORIZATION kutu_admin;
SET search_path TO kutu;