#!/bin/bash
# Carga el esquema y los datos forzando UTF-8 (utf8mb4) para que las tildes y la "ñ" se guarden bien.
set -e
for archivo in /semilla/schema.sql /semilla/data.sql; do
  echo "Cargando $archivo"
  mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 eventos_gamer < "$archivo"
done
