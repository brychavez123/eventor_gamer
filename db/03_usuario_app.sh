#!/bin/bash
# Crea el usuario que usa la aplicación con PRIVILEGIO MÍNIMO (A01/A05):
#  - SELECT en todas las tablas (lectura de eventos, banners, usuarios para el login).
#  - INSERT solo en las tablas que la semana 5 necesita escribir (fotos/videos, comentarios, compartidos).
#  - DELETE solo en comentario (el autor puede borrar su comentario).
#  - Sin UPDATE, sin DDL (CREATE/DROP/ALTER) y sin acceso a otras bases.
set -e
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS 'eventos_app'@'%' IDENTIFIED BY '${DB_APP_PASSWORD}';
GRANT SELECT ON eventos_gamer.* TO 'eventos_app'@'%';
GRANT INSERT ON eventos_gamer.evento_medio TO 'eventos_app'@'%';
GRANT INSERT ON eventos_gamer.evento_medio_contenido TO 'eventos_app'@'%';
GRANT INSERT, DELETE ON eventos_gamer.comentario TO 'eventos_app'@'%';
GRANT INSERT ON eventos_gamer.evento_compartido TO 'eventos_app'@'%';
FLUSH PRIVILEGES;
SQL
