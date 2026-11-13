#!/usr/bin/env bash
# Arranque en macOS y Linux (también Git Bash / WSL en Windows).
#
#   ./scripts/iniciar.sh            base de datos + interfaz gráfica
#   ./scripts/iniciar.sh --cli      base de datos + consola interactiva
#   ./scripts/iniciar.sh --db       solo la base de datos (MySQL :3307 y phpMyAdmin :8090)
#   ./scripts/iniciar.sh --parar    detiene los contenedores (los datos se conservan)
#   ./scripts/iniciar.sh --check    comprueba requisitos sin arrancar nada
set -euo pipefail

cd "$(dirname "$0")/.."
MODO="${1:-gui}"
COMPOSE_FILE="docker/docker-compose.yml"

info()  { printf '\033[1;34m›\033[0m %s\n' "$*"; }
ok()    { printf '\033[1;32m✔\033[0m %s\n' "$*"; }
fallo() { printf '\033[1;31m✘\033[0m %s\n' "$*" >&2; exit 1; }

sistema() {
  case "$(uname -s)" in
    Darwin) echo "macOS ($(uname -m))" ;;
    Linux)  echo "Linux ($(uname -m))" ;;
    MINGW*|MSYS*|CYGWIN*) echo "Windows ($(uname -m))" ;;
    *) uname -s ;;
  esac
}

requisitos_java() {
  command -v java >/dev/null 2>&1 || fallo "No se encuentra Java. Instala el JDK 21 (ver docs/MULTIPLATAFORMA.md)."
  local version
  version=$(java -version 2>&1 | awk -F\" '/version/ {print $2}' | cut -d. -f1)
  [ "${version:-0}" -ge 21 ] || fallo "Se necesita Java 21 o superior (encontrado: ${version:-desconocido})."
  ok "Java $version"
}

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose -f "$COMPOSE_FILE" "$@"
  elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose -f "$COMPOSE_FILE" "$@"
  else
    fallo "No se encuentra Docker Compose (ni «docker compose» ni «docker-compose»)."
  fi
}

requisitos_docker() {
  command -v docker >/dev/null 2>&1 || fallo "No se encuentra Docker. Instala Docker Desktop o Docker Engine."
  docker info >/dev/null 2>&1 || fallo "Docker no está en marcha. Abre Docker Desktop (o «sudo systemctl start docker») y reintenta."
  ok "Docker en marcha"
}

base_de_datos() {
  info "Levantando MySQL y phpMyAdmin…"
  compose up -d
  info "Esperando a que MySQL esté listo…"
  for _ in $(seq 1 60); do
    estado=$(docker inspect -f '{{.State.Health.Status}}' jardineria-mysql 2>/dev/null || echo "?")
    if [ "$estado" = "healthy" ]; then
      ok "MySQL listo en localhost:3307 · phpMyAdmin en http://localhost:8090 (root / password)"
      return
    fi
    sleep 2
  done
  fallo "MySQL no respondió en 2 minutos. Revisa «docker logs jardineria-mysql»."
}

info "Sistema: $(sistema)"
case "$MODO" in
  --check)
    requisitos_java; requisitos_docker; ok "Todo listo." ;;
  --parar)
    requisitos_docker; compose stop; ok "Contenedores detenidos." ;;
  --db)
    requisitos_docker; base_de_datos ;;
  --cli)
    requisitos_java; requisitos_docker; base_de_datos
    ./mvnw -q compile exec:java -Dexec.args=--cli ;;
  gui|--gui)
    requisitos_java; requisitos_docker; base_de_datos
    info "Abriendo la interfaz gráfica (la primera vez Maven descarga dependencias)…"
    ./mvnw -q javafx:run ;;
  *)
    fallo "Opción desconocida: $MODO (usa --cli, --db, --parar o --check)" ;;
esac
