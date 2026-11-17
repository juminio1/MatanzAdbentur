#!/usr/bin/env bash
# Ejecutar desde Git Bash en la carpeta que contiene docker-compose.yml.
# Crea tallerwebi_e2e sin eliminar ni modificar la base tallerwebi.
set -euo pipefail

if [[ ! -f docker-compose.yml ]]; then
  echo "ERROR: Ejecuta este script desde la carpeta donde esta docker-compose.yml." >&2
  exit 1
fi

echo "Iniciando PostgreSQL..."
docker compose up -d postgres

echo "Esperando a que PostgreSQL acepte conexiones..."
listo=false
for intento in {1..30}; do
  if docker exec tallerwebi-postgres sh -c 'pg_isready -U "$POSTGRES_USER" -d postgres' >/dev/null 2>&1; then
    listo=true
    break
  fi
  sleep 2
done
if [[ "$listo" != true ]]; then
  echo "ERROR: PostgreSQL no responde. Revisa: docker compose logs postgres" >&2
  exit 1
fi

existe=$(docker exec tallerwebi-postgres sh -c \
  'psql -U "$POSTGRES_USER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '\''tallerwebi_e2e'\'';"')

if [[ "$existe" == "1" ]]; then
  echo "La base tallerwebi_e2e ya existe. No se modifica."
else
  docker exec tallerwebi-postgres sh -c \
    'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -c "CREATE DATABASE tallerwebi_e2e;"'
  echo "Base tallerwebi_e2e creada correctamente."
fi

echo "Comprobacion:"
docker exec tallerwebi-postgres sh -c \
  'psql -U "$POSTGRES_USER" -d postgres -c "SELECT datname FROM pg_database WHERE datname = '\''tallerwebi_e2e'\'';"'
