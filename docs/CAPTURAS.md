# Capturas

Las imágenes de la documentación no se hacen a mano: las generan las pruebas de interfaz con **TestFX** sobre **Monocle** (JavaFX sin pantalla), contra la base de datos real de Docker. Así siempre reflejan la versión actual del código.

> ¿Por qué no Playwright? Playwright automatiza navegadores; esta aplicación es JavaFX de escritorio, así que el equivalente nativo es TestFX.

## Cómo regenerarlas

```bash
docker-compose -f docker/docker-compose.yml up -d          # MySQL con datos
./mvnw test -Dtest=DesktopScreenshotTest -Dui.screenshots=true  # cada módulo, ambos modos, formularios, ventana mínima
./mvnw test -Dtest=DesktopGalleryTest -Dui.screenshots=true     # temas, panel desplazado, Ajustes, atajos, paleta
```

```mermaid
flowchart LR
    T1[DesktopScreenshotTest] --> O1["target/screenshots/<br/>dark-* · light-* · narrow-*<br/>form-* · detail-* · pattern-*"]
    T2[DesktopGalleryTest] --> O2["target/screenshots/gallery/<br/>dashboard-&lt;tema&gt; · dashboard-hover<br/>settings-* · shortcuts · palette"]
    O1 & O2 -->|selección| D[docs/img]
```

Si MySQL no está disponible, ambas clases se omiten (`assumeTrue`) en lugar de fallar.

## Galería

### Panel general

| Arriba | Cursor sobre la tendencia |
|---|---|
| ![](img/dashboard-dark.png) | ![](img/dashboard-hover.png) |
| **Mapa de actividad y lecturas** | **Listas** |
| ![](img/dashboard-middle.png) | ![](img/dashboard-bottom.png) |

### Antes y después

![Oscuro](img/antes-despues-dark.png)

![Claro](img/antes-despues-light.png)

### Otros módulos

| Métricas | Pedidos (claro) |
|---|---|
| ![](img/analytics-dark.png) | ![](img/pedidos-light.png) |
| **Ficha de cliente** | **Formulario de pedido** |
| ![](img/clientes-detalle.png) | ![](img/pedidos-formulario.png) |
| **Patrón Decorator** | **Ajustes (parte inferior)** |
| ![](img/patron-decorator.png) | ![](img/ajustes-dark-bottom.png) |
