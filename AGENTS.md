# AGENTS.md — Guía para agentes de IA

Contexto operativo para cualquier agente (Claude, Cursor, Copilot…) que modifique este repositorio. El README cubre el uso; aquí están las **reglas** que el código espera que se cumplan.

## 1. Resumen

- **Producto**: suite de escritorio para la base de datos `jardineria` (8 tablas: oficina, empleado, cliente, gama_producto, producto, pedido, detalle_pedido, pago).
- **Stack**: Java 21 · JavaFX 21 + AtlantaFX 2.1 + Ikonli · JDBC + HikariCP · MySQL 8 en Docker · JUnit 5, Mockito, ArchUnit, TestFX + Monocle.
- **Versión**: 2.0.0 (`pom.xml`). Punto de entrada: `com.jardineria.infrastructure.JardineriaApplication` (sin argumentos abre la GUI; `--cli` abre la consola).
- **Lo que se evalúa**: persistencia JDBC con MySQL en Docker, CRUD funcional, arquitectura hexagonal, SOLID y patrones GoF.

## 2. Reglas de arquitectura (las verifica `HexagonalArchitectureTest`)

| Capa | Paquete | Puede depender de | Nunca de |
|---|---|---|---|
| Dominio | `domain..` | JDK | `application`, `infrastructure`, `java.sql`, `javafx`, librerías externas |
| Aplicación | `application..` | `domain` | `infrastructure`, `java.sql`, `javafx` |
| Infraestructura | `infrastructure..` | todo | — |

- La GUI y la CLI consumen **puertos de entrada** (`application.port.in.*UseCase`), nunca servicios concretos ni repositorios. La GUI los recibe agrupados en el record `DesktopServices`.
- `ApplicationContext` es el único *composition root*: crea repositorios, servicios, fachada y proxy. No arranca hilos ni servidores.
- Toda conexión sale de `DatabaseConnectionManager.getInstance().getConnection()` (Singleton con HikariCP). Siempre `PreparedStatement` y *try-with-resources*.
- Las entidades con clave compuesta usan `DetallePedidoId` y `PagoId`.
- La gama `Herbaceas` es clave primaria y foránea en MySQL: **no se renombra**. Se muestra como "Herbáceas" con `Formatters.display`.

## 3. Interfaz JavaFX

- **Vistas**: heredan de `BaseView` (cabecera, scroll y estados cargando/error) y consultan datos con `load(callable, render)`, que ejecuta en un hilo virtual. **Nunca** se hace JDBC en el hilo de JavaFX.
- **Módulos CRUD**: heredan de `CrudModuleView<T>` e implementan `fetch`, `configureColumns`, `searchableText`, `openEditor`, `deleteItem`, `describe`, `csvHeader` y `csvRow`. Los formularios se construyen con `FormDialog` (validación inline y guardado en segundo plano).
- **Registro**: cada ruta de `navigation.Route` se registra en `views.DesktopViews.registerAll`. Una ruta nueva necesita entrada en el enum, vista y registro.
- **Estilos**: solo CSS (`resources/desktop/css/app.css` y `tokens-dark.css` / `tokens-light.css`). Los colores salen de variables (`-color-*`, `-jd-*`, `.tone-*`), nunca de valores fijos en Java, salvo los datos que traen su propio color (los hex del flyweight).
- **Iconos**: solo Ikonli (`Icons.of(Feather.X, size)`). **Prohibido usar emojis** en la UI, en el dominio y en la CLI. En un `FontIcon` no se usa `setStyle(...)` a secas, porque borra la fuente del icono: el color se añade concatenándolo a `getStyle()`.
- **Mensajes al usuario**: con `Notifier.success/info/warning/error` (toasts) y diálogos mediante `ModalHost` (`show`, `confirmDanger`). Nada de ventanas nativas `Alert`.
- **Accesibilidad**: todo botón de icono lleva `setAccessibleText` y `Tooltip`. Hay que respetar `Motion.isReduced()`.
- **Layout**: los contenedores de tarjetas usan `ResponsiveGrid`, que baja elementos de fila en lugar de comprimirlos. Hay que comprobar el resultado a 1100×720 (ventana mínima).

## 4. Texto y codificación

- Todo el código fuente, el SQL y los recursos están en **UTF-8**. El texto visible lleva ortografía española completa (tildes, ñ, ¿?, ¡!).
- Antes de terminar, comprueba que no hay mojibake: `grep -rn "Ã\|Â\|�" src docker` no debe devolver nada.
- Fechas y moneda siempre con `Formatters` (locale `es-ES`). El CSV de exportación usa `;`, BOM y RFC 4180 (`CsvExport`).

## 5. Base de datos

```bash
docker compose -f docker/docker-compose.yml up -d     # MySQL :3307 + phpMyAdmin :8090
```

- Credenciales: `root` / `password`, base `jardineria`. Se sobrescriben con `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD`.
- Esquema y datos: `docker/init/01-jardineria.sql`, que solo se ejecuta al crear el volumen.
- Volúmenes antiguos con texto roto: `docker/maintenance/repair-encoding.sql`.
- El contenedor `golands-db` que pueda existir en la máquina **no pertenece a este proyecto**: no se toca.

## 6. Comandos

```bash
mvn javafx:run                         # ejecutar la GUI
mvn test                               # suite completa (GUI headless; los tests JDBC se omiten sin MySQL)
mvn test -Dtest=DesktopScreenshotTest -Dui.screenshots=true   # capturas en target/screenshots
mvn package -DskipTests                # target/gestion-jardineria-2.0.0.jar (JAR sombreado)
java -jar target/gestion-jardineria-2.0.0.jar [--cli]
```

Tras un cambio visual, genera las capturas y revísalas en tema oscuro, en tema claro y en la ventana mínima (`narrow-*`).

## 7. Git

1. Rama por defecto: `develop`. Nunca se hace commit directo en `main` ni en `develop`.
2. Cada cambio va en una rama `feature/<kebab-case>`.
3. Commits convencionales con gitmoji: `✨ feat(scope)`, `🐛 fix`, `♻️ refactor`, `🎨 style`, `📝 docs`, `🧪 test`, `🔥 remove`, `🍱 assets`, `⚡ perf`. El cuerpo explica el porqué.
4. Integración con `git merge --no-ff` hacia `develop`, **solo cuando el responsable lo valide**. Ningún agente hace push ni merge por iniciativa propia.
