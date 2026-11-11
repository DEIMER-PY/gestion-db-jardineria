# AGENTS.md — Guía de Contexto y Referencia para Agentes de Inteligencia Artificial

Este documento está diseñado específicamente como manual de contexto, arquitectura y estándares operativos para **Claude**, **Antigravity**, **Cursor** y cualquier otro agente autónomo de IA que continúe el desarrollo y mantenimiento del repositorio **`gestion-db-jardineria`**.

---

## 1. Visión General del Proyecto

- **Nombre del Sistema**: Jardinería Enterprise Suite
- **Versión**: 1.3.0 Enterprise (Java 21 LTS)
- **Propósito**: Gestión integral y analítica operacional de la base de datos empresarial `jardineria`.
- **Ecosistema Tecnológico**:
  - **Lenguaje**: Java 21 LTS (Virtual Threads / Project Loom).
  - **Patrón de Arquitectura**: Arquitectura Hexagonal Pura (Ports & Adapters) + DDD táctico.
  - **Persistencia**: MySQL 8.0 en contenedor Docker (`port 3307`), HikariCP Connection Pool (Singleton).
  - **Presentación Dual**:
    1. **GUI Nativa de Escritorio**: Swing moderno potenciado por **FlatLaf**, motor gráfico vectorial propio `VectorIcons` (Java2D nativo), microtendencias `SparklineChart`, notificaciones no bloqueantes `ToastNotificationManager`, migas de pan `BreadcrumbBar` y filtros reactivos `SearchFilterBar`.
    2. **Modo Aplicación Web / Electron**: Servidor HTTP embebido en Java (`HttpServer` nativo) que sirve una SPA de alto impacto visual (Tailwind CSS, Chart.js, Lucide SVG icons) abierta en ventana independiente mediante Chromium/Chrome (`--app`).
    3. **Consola CLI**: Menú interactivo estructurado para entornos headless.
  - **Patrones de Diseño**: 7 patrones estructurales GoF implementados y testeados.

---

## 2. Reglas Arquitectónicas Innegociables

Cualquier agente que modifique este código **DEBE** respetar estrictamente las siguientes fronteras arquitectónicas:

### 2.1 Pureza de la Capa de Dominio (`com.jardineria.domain.*`)
- **Agnóstica de Frameworks**: El paquete `domain` **NO** debe importar librerías de persistencia (JDBC, JPA, Spring), componentes gráficos (`java.awt.*`, `javax.swing.*`), librerías web ni utilitarios de terceros.
- **Entidades Ricas**: Las 8 entidades (`Oficina`, `Empleado`, `Cliente`, `GamaProducto`, `Producto`, `Pedido`, `DetallePedido`, `Pago`) encapsulan reglas de validación y métodos de negocio.
- **Claves Compuestas**: Usar siempre `DetallePedidoId` y `PagoId` para las entidades dependientes.
- **Puertos de Salida**: Las interfaces en `domain.repository` definen los contratos abstractos (`CrudRepository<T, ID>` y repositorios específicos).

### 2.2 Capa de Aplicación (`com.jardineria.application.*`)
- Contiene los servicios orquestadores (`*Service.java`).
- No contiene lógica de presentación ni consultas SQL directas.
- Coordina transacciones y valida integridad referencial a través de los puertos de repositorio.

### 2.3 Patrón Singleton en Conexiones (`DatabaseConnectionManager.java`)
- Toda conexión JDBC a MySQL debe obtenerse a través de `DatabaseConnectionManager.getInstance().getConnection()`.
- Implementado con **Double-Checked Locking** y `volatile`.
- El pool HikariCP se inicializa una sola vez y se cierra ordenadamente mediante shutdown hook.

### 2.4 Patrones GoF Estructurales (`com.jardineria.domain.pattern.*`)
- **Facade**: `JardineriaBusinessFacade` orquesta el flujo atómico de checkout.
- **Adapter**: `PayPalAdapter` y `StripeAdapter` implementan `PaymentGatewayPort`.
- **Decorator**: `BulkDiscountDecorator`, `SeasonalPromoDecorator`, `VatTaxDecorator` envuelven `PriceCalculator`.
- **Composite**: `ProductBundleComposite` y `ProductLeaf` implementan `CatalogComponent`.
- **Proxy**: `CachedProductLookupProxy` cachea en RAM las consultas a `ProductoRepository`.
- **Flyweight**: `GamaFlyweightFactory` y `GamaFlyweight` gestionan el estado intrínseco botánico mediante strings hexadecimales (`#10B981`), **sin acoplamiento a AWT**.
- **Bridge**: `ExecutiveReport` desacopla los reportes de los generadores `CsvOutputImplementor`, `HtmlOutputImplementor` y `JsonOutputImplementor`.

---

## 3. Estándares Visuales y de Interfaz

### 3.1 Prohibición de Emojis Planos
- **REGLA CRÍTICA**: Nunca incrustar emojis planos (`🌿`, `📦`, `🗑️`, `💾`, etc.) en etiquetas, botones ni tablas de Swing.
- Utilizar exclusivamente:
  - `VectorIcons.get(IconType, size, color)` para iconos escalables en Java Swing.
  - Iconos Lucide SVG en el panel web (`data-lucide="..."`).

### 3.2 Notificaciones No Bloqueantes
- En lugar de `JOptionPane.showMessageDialog` para operaciones comunes, utilizar:
  ```java
  ToastNotificationManager.showSuccess("Título", "Mensaje detallado");
  ToastNotificationManager.showInfo("Título", "Mensaje informativo");
  ToastNotificationManager.showWarning("Título", "Mensaje de advertencia");
  ToastNotificationManager.showError("Título", "Mensaje de error");
  ```

### 3.3 Ortografía y Localización en Español
- Todo texto visible al usuario debe respetar la ortografía correcta con tildes y caracteres UTF-8: *Jardinería, Código, Teléfono, Dirección, Pedido, Operación, Crédito, Catálogo, Métricas*.
- Al serializar JSON numérico en `JardineriaWebServer`, **SIEMPRE usar `Locale.US`** para formatear decimales con punto (`.`), evitando que el locale del sistema produzca comas (`,`) que rompan el parser JSON del frontend.

---

## 4. Flujo de Trabajo Git (Git Flow)

Los agentes deben ceñirse estrictamente al estándar Git definido en los requerimientos:

1. **Rama Base**: `develop` es la rama por defecto. Nunca hacer commits directos a `main`.
2. **Creación de Ramas**:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/<nombre-en-kebab-case>
   ```
3. **Estructura de Commits (Conventional Commits + Gitmojis)**:
   - `✨ feat(<scope>): <descripción>` — Nueva funcionalidad o componente.
   - `🐛 fix(<scope>): <descripción>` — Corrección de errores.
   - `♻️ refactor(<scope>): <descripción>` — Refactorización sin alterar comportamiento.
   - `🎨 style(<scope>): <descripción>` — Mejoras estéticas o de formato.
   - `📝 docs(<scope>): <descripción>` — Actualización de documentación o guías.
   - `🧪 test(<scope>): <descripción>` — Adición o ajuste de pruebas automatizadas.
4. **Integración**:
   - Merge sin fast-forward (`--no-ff`) hacia `develop`.
   - Eliminar la rama local `feature/*` tras completar la integración.

---

## 5. Infraestructura y Base de Datos

### Contenedor Docker Oficial
- **Archivo Compose**: `docker/docker-compose.yml`
- **Script de Inicialización**: `docker/init/01-jardineria.sql`
- **Puerto de Exposición**: `3307` (mapeado a `3306` interno)
- **Credenciales**:
  - Base de datos: `jardineria`
  - Usuario: `root`
  - Contraseña: `jardineria_root_pwd`
- **Comandos de Gestión**:
  ```bash
  # Iniciar contenedor
  docker compose -f docker/docker-compose.yml up -d

  # Verificar estado y puerto
  docker ps --filter "name=jardineria-mysql"

  # Detener contenedor
  docker compose -f docker/docker-compose.yml down
  ```

---

## 6. Comandos Frecuentes para Agentes

### Compilación y Pruebas
```bash
# Ejecutar todas las 23 pruebas unitarias y de integración
mvn clean test

# Empaquetar el Uber JAR autónomo con dependencias sombreadas (Maven Shade)
mvn package -DskipTests=true
```

### Modos de Ejecución de la Aplicación
```bash
# Modo 1: Aplicación de Escritorio Web (Ventana standalone sin barra URL)
java -jar target/gestion-jardineria-1.0.0.jar --app

# Modo 2: Interfaz Gráfica Nativa de Escritorio (FlatLaf Swing + VectorIcons)
java -jar target/gestion-jardineria-1.0.0.jar

# Modo 3: Consola Interactiva (CLI para terminal o servidores sin display)
java -jar target/gestion-jardineria-1.0.0.jar --cli
```

---

## 7. Mapa de Directorios Clave

```
gestion-db-jardineria/
├── docker/
│   ├── docker-compose.yml              # Configuración MySQL 8.0 en puerto 3307
│   └── init/01-jardineria.sql          # Esquema y datos oficiales poblados
├── src/main/java/com/jardineria/
│   ├── domain/
│   │   ├── model/                      # 8 Entidades de dominio + Claves compuestas
│   │   ├── repository/                 # Contratos de repositorio (Puertos de salida)
│   │   ├── pattern/                    # 7 Patrones estructurales GoF
│   │   └── exception/                  # Excepciones de negocio personalizadas
│   ├── application/service/            # 8 Servicios de aplicación + DashboardService
│   └── infrastructure/
│       ├── config/                     # HikariCP Singleton Connection Manager
│       ├── adapter/out/persistence/    # Repositorios JDBC + RowMappers
│       ├── adapter/in/ui/              # GUI Swing: MainDashboardWindow, views/ y components/
│       ├── adapter/in/web/             # JardineriaWebServer (Java 21 HttpServer API)
│       └── adapter/in/cli/             # Menú interactivo de consola
├── src/main/resources/
│   ├── web/index.html                  # Frontend moderno (Tailwind + Chart.js + Lucide)
│   └── logback.xml                     # Configuración de logs SLF4J
├── AGENTS.md                           # Guía técnica para agentes de IA
└── pom.xml                             # Configuración Maven 3.9+ y Java 21
```

---

> **Nota para Agentes**: Al agregar nuevas funcionalidades, asegúrate de mantener la cobertura de pruebas unitarias (`src/test/java`), verificar que `mvn clean test` termine en `BUILD SUCCESS`, y nunca incluir archivos temporales o directorios redundantes en la raíz del repositorio.
