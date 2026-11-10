# 🌿 Sistema de Gestión de Base de Datos Jardinería

Sistema integral en **Java 21** para la administración de la base de datos empresarial **Jardinería**, implementado bajo **Arquitectura Hexagonal (Ports & Adapters)**, principios **SOLID**, **Patrón Singleton** para la gestión de conexiones y una **interfaz gráfica moderna con Dashboard** desarrollada con **FlatLaf**.

---

## 🏛️ Arquitectura Hexagonal y Principios de Diseño
- **Núcleo de Dominio (`domain`)**: Entidades puras y objetos de valor con sus relaciones UML explícitas y puertos de repositorios (`CrudRepository`).
- **Capa de Aplicación (`application`)**: Servicios de negocio desacoplados y agregador de métricas en tiempo real (`DashboardService`).
- **Capa de Infraestructura (`infrastructure`)**:
  - **Adaptadores Outbound**: Repositorios JDBC con `HikariCP`, RowMappers dedicados y `DatabaseConnectionManager` (Patrón Singleton thread-safe).
  - **Adaptadores Inbound**: Interfaz Gráfica de Usuario (GUI) Swing moderna con **FlatLaf** (Dark/Light mode, tarjetas KPI, tablas con filtros y modales CRUD) y menú interactivo por terminal (CLI).

---

## 🐳 Contenedor MySQL en Docker
El proyecto incluye un entorno Docker listo para levantar la base de datos oficial `jardineria` con todas sus tablas y datos de prueba precargados.

```bash
docker compose -f docker/docker-compose.yml up -d
```

---

## 🚀 Ejecución de la Aplicación

### Modo Gráfico (GUI Dashboard)
```bash
mvn exec:java -Dexec.mainClass="com.jardineria.infrastructure.JardineriaApplication"
```

### Modo Consola (CLI Interactivo)
```bash
mvn exec:java -Dexec.mainClass="com.jardineria.infrastructure.JardineriaApplication" -Dexec.args="--cli"
```
