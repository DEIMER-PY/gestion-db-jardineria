# AGENTS.md — Guía para agentes de IA

Contexto operativo para cualquier agente (Claude, Cursor, Copilot…) que modifique este repositorio. El README cubre el uso; aquí están las **reglas** que el código espera que se cumplan.

## 1. Resumen

- **Producto**: suite de escritorio para la base de datos `jardineria` (8 tablas: oficina, empleado, cliente, gama_producto, producto, pedido, detalle_pedido, pago).
- **Stack**: Java 21 · JavaFX 21 + AtlantaFX 2.1 + Ikonli · JDBC + HikariCP · MySQL 8 en Docker · JUnit 5, Mockito, ArchUnit, TestFX + Monocle · Maven Wrapper (`mvnw`, no requiere Maven instalado) · CI en Windows, macOS y Linux (`.github/workflows/ci.yml`).
- **Versión**: 2.0.0 (`pom.xml`). Punto de entrada: `com.jardineria.infrastructure.JardineriaApplication` (sin argumentos abre la GUI; `--cli` abre la consola).
- **Lo que se evalúa**: persistencia JDBC con MySQL en Docker, CRUD funcional, arquitectura hexagonal, SOLID y patrones GoF.

## 2. Reglas de arquitectura (las verifica `HexagonalArchitectureTest`)

| Capa | Paquete | Puede depender de | Nunca de |
|---|---|---|---|
| Dominio | `domain..` | JDK | `application`, `infrastructure`, `java.sql`, `javafx`, librerías externas |
| Aplicación | `application..` | `domain` | `infrastructure`, `java.sql`, `javafx` |
| Infraestructura | `infrastructure..` | todo | — |

- La GUI y la CLI consumen **puertos de entrada** (`application.port.in.*UseCase`), nunca servicios concretos ni repositorios. La GUI los recibe agrupados en el record `DesktopServices`.
- `ApplicationContext` es el único *composition root*: crea repositorios y servicios. No arranca hilos ni servidores.
- Los patrones GoF viven en `domain.pattern` y `application.facade` y se verifican con pruebas (`GofStructuralPatternsTest`, `JardineriaBusinessFacadeTest`). La GUI no tiene pantalla de demostración: no la reintroduzcas.
- Toda conexión sale de `DatabaseConnectionManager.getInstance().getConnection()` (Singleton con HikariCP). Siempre `PreparedStatement` y *try-with-resources*.
- Las entidades con clave compuesta usan `DetallePedidoId` y `PagoId`.
- La gama `Herbaceas` es clave primaria y foránea en MySQL: **no se renombra**. Se muestra como "Herbáceas" con `Formatters.display`.

## 3. Interfaz JavaFX

- **Vistas**: heredan de `BaseView` (cabecera, scroll y estados cargando/error) y consultan datos con `load(callable, render)`, que ejecuta en un hilo virtual. **Nunca** se hace JDBC en el hilo de JavaFX.
- **Módulos CRUD**: heredan de `CrudModuleView<T>` e implementan `fetch`, `configureColumns`, `searchableText`, `openEditor`, `deleteItem`, `describe`, `csvHeader` y `csvRow`. Los formularios se construyen con `FormDialog` (validación inline y guardado en segundo plano).
- **Registro**: cada ruta de `navigation.Route` se registra en `views.DesktopViews.registerAll`. Una ruta nueva necesita entrada en el enum, vista y registro.
- **Navegación contextual**: para abrir un módulo con un elemento concreto usa `Navigator.navigate(ruta, consulta)`; la vista lo recibe en `View.reveal`. Los elementos clicables que no son botones se preparan con `Interactions.activatable` (foco, rol de botón, clic, `Enter` y `Espacio`).
- **Validación**: las reglas de formulario van con `FormField.rule`; para claves de altas usa `CrudModuleView.uniqueKey`. La capa de aplicación vuelve a validar al guardar.
- **Estilos**: solo CSS. `ThemeManager` aplica, por este orden, Primer (AtlantaFX), `css/themes/<tema>.css` (superficies y texto), `css/accents.css` (acento por modo, `.theme-dark.accent-*` / `.theme-light.accent-*`) y `css/app.css` (componentes), más clases en la raíz (`theme-*`, `accent-*`, `density-*`, `corners-*`, `ambient-off`). Los colores salen de variables (`-color-*`, `-jd-*`, `.tone-*`), nunca de valores fijos en Java, salvo los datos que traen su propio color (los hex del flyweight). Un tema nuevo necesita entrada en `ThemePreset` y su CSS; un acento nuevo, entrada en `Accent` y sus dos bloques en `accents.css`. Detalle en `docs/APARIENCIA.md`.
- **Apariencia**: la configura el usuario en Ajustes mediante `AppearanceProfile` (record inmutable, se modifica con `ThemeManager.update(p -> p.withX(...))`) y se persiste con `ProfileStore` (`preferences()` en la app, `inMemory()` en pruebas). No guardes preferencias visuales por otra vía.
- **Panel general**: los datos se obtienen en una sola carga (`DashboardSnapshot.load`); las conclusiones textuales viven en `DashboardInsights` (lógica pura, con pruebas). Las variaciones se calculan con `Series.recentChange` (ventana fija, vacío si la base es cero); no compares periodos de distinta duración. Detalle en `docs/DASHBOARD.md`.
- **Iconos**: solo Ikonli (`Icons.of(Feather.X, size)`). **Prohibido usar emojis** en la UI, en el dominio y en la CLI. En un `FontIcon` no se usa `setStyle(...)` a secas, porque borra la fuente del icono: el color se añade concatenándolo a `getStyle()`.
- **Mensajes al usuario**: con `Notifier.success/info/warning/error` (toasts) y diálogos mediante `ModalHost` (`show`, `confirmDanger`). Nada de ventanas nativas `Alert`.
- **Accesibilidad**: todo botón de icono lleva `setAccessibleText` y `Tooltip`. Hay que respetar `Motion.isReduced()`.
- **Layout**: los contenedores de tarjetas usan `ResponsiveGrid`, que baja elementos de fila en lugar de comprimirlos. Hay que comprobar el resultado a 1100×720 (ventana mínima).

- **Multiplataforma** (Windows, macOS, Linux; x64 y ARM64): los atajos se registran con `KeyCombination.SHORTCUT_DOWN` (nunca `CONTROL_DOWN`) y todo texto que nombre teclas pasa por `Keys.text(...)` (muestra ⌘ ⌥ ⌫ en macOS). Nada de rutas fijas ni separadores `\`: usa `Path`/`Files` y `user.home`. Los scripts existen en dos versiones (`scripts/iniciar.sh` y `scripts/iniciar.ps1` + `.cmd`): si cambias uno, cambia el otro. Detalle en `docs/MULTIPLATAFORMA.md`.

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
./mvnw javafx:run                       # ejecutar la GUI (Windows: .\mvnw.cmd); o ./scripts/iniciar.sh | scripts\iniciar.cmd
./mvnw test                               # suite completa (GUI headless; los tests JDBC se omiten sin MySQL)
./mvnw test -Dtest=DesktopScreenshotTest -Dui.screenshots=true   # capturas en target/screenshots
./mvnw test -Dtest=DesktopGalleryTest -Dui.screenshots=true      # galería de temas, Ajustes y panel en target/screenshots/gallery
./mvnw package -DskipTests              # target/gestion-jardineria-2.0.0-<plataforma>.jar (win, mac, mac-aarch64, linux, linux-aarch64)
java -jar target/gestion-jardineria-2.0.0-<plataforma>.jar [--cli]
```

Tras un cambio visual, genera las capturas y revísalas en tema oscuro, en tema claro y en la ventana mínima (`narrow-*`). Si cambian pantallas documentadas, actualiza las imágenes de `docs/img` (ver `docs/CAPTURAS.md`).

## 7. Git

1. Rama por defecto: `develop`. Nunca se hace commit directo en `main` ni en `develop`.
2. Cada cambio va en una rama `feature/<kebab-case>`.
3. Commits convencionales con gitmoji: `✨ feat(scope)`, `🐛 fix`, `♻️ refactor`, `🎨 style`, `📝 docs`, `🧪 test`, `🔥 remove`, `🍱 assets`, `⚡ perf`. El cuerpo explica el porqué.
4. Integración con `git merge --no-ff` hacia `develop`, **solo cuando el responsable lo valide**. Ningún agente hace push ni merge por iniciativa propia.
