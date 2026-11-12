-- =====================================================================
-- Reparación de texto con doble codificación (mojibake) en la BD jardineria
-- ---------------------------------------------------------------------
-- Síntoma : 'EspaÃ±a', 'AromÃ¡ticas', 'JapÃ³n' en lugar de 'España', 'Aromáticas', 'Japón'.
-- Causa   : el script de carga se ejecutó con un cliente latin1, de modo que cada
--           byte UTF-8 se volvió a codificar como si fuera un carácter latin1.
-- Solución: revertir la conversión (utf8mb4 -> latin1 -> bytes -> utf8mb4) SOLO
--           en las filas afectadas. Es idempotente: re-ejecutarlo no daña datos sanos.
--
-- Uso (contenedor del proyecto):
--   docker exec -i jardineria-mysql mysql -uroot -ppassword --default-character-set=utf8mb4 jardineria < docker/maintenance/repair-encoding.sql
-- O bien: phpMyAdmin > base 'jardineria' > pestaña SQL > pegar y ejecutar.
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Pasada 1
UPDATE oficina SET ciudad = CONVERT(CAST(CONVERT(ciudad USING latin1) AS BINARY) USING utf8mb4) WHERE ciudad REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(ciudad USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE oficina SET pais = CONVERT(CAST(CONVERT(pais USING latin1) AS BINARY) USING utf8mb4) WHERE pais REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(pais USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE oficina SET region = CONVERT(CAST(CONVERT(region USING latin1) AS BINARY) USING utf8mb4) WHERE region REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(region USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE oficina SET codigo_postal = CONVERT(CAST(CONVERT(codigo_postal USING latin1) AS BINARY) USING utf8mb4) WHERE codigo_postal REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(codigo_postal USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE oficina SET telefono = CONVERT(CAST(CONVERT(telefono USING latin1) AS BINARY) USING utf8mb4) WHERE telefono REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(telefono USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE oficina SET linea_direccion1 = CONVERT(CAST(CONVERT(linea_direccion1 USING latin1) AS BINARY) USING utf8mb4) WHERE linea_direccion1 REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(linea_direccion1 USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE oficina SET linea_direccion2 = CONVERT(CAST(CONVERT(linea_direccion2 USING latin1) AS BINARY) USING utf8mb4) WHERE linea_direccion2 REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(linea_direccion2 USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE empleado SET nombre = CONVERT(CAST(CONVERT(nombre USING latin1) AS BINARY) USING utf8mb4) WHERE nombre REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(nombre USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE empleado SET apellido1 = CONVERT(CAST(CONVERT(apellido1 USING latin1) AS BINARY) USING utf8mb4) WHERE apellido1 REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(apellido1 USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE empleado SET apellido2 = CONVERT(CAST(CONVERT(apellido2 USING latin1) AS BINARY) USING utf8mb4) WHERE apellido2 REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(apellido2 USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE empleado SET extension = CONVERT(CAST(CONVERT(extension USING latin1) AS BINARY) USING utf8mb4) WHERE extension REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(extension USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE empleado SET email = CONVERT(CAST(CONVERT(email USING latin1) AS BINARY) USING utf8mb4) WHERE email REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(email USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE empleado SET puesto = CONVERT(CAST(CONVERT(puesto USING latin1) AS BINARY) USING utf8mb4) WHERE puesto REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(puesto USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE gama_producto SET gama = CONVERT(CAST(CONVERT(gama USING latin1) AS BINARY) USING utf8mb4) WHERE gama REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(gama USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE gama_producto SET descripcion_texto = CONVERT(CAST(CONVERT(descripcion_texto USING latin1) AS BINARY) USING utf8mb4) WHERE descripcion_texto REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(descripcion_texto USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE gama_producto SET descripcion_html = CONVERT(CAST(CONVERT(descripcion_html USING latin1) AS BINARY) USING utf8mb4) WHERE descripcion_html REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(descripcion_html USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE gama_producto SET imagen = CONVERT(CAST(CONVERT(imagen USING latin1) AS BINARY) USING utf8mb4) WHERE imagen REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(imagen USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET nombre_cliente = CONVERT(CAST(CONVERT(nombre_cliente USING latin1) AS BINARY) USING utf8mb4) WHERE nombre_cliente REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(nombre_cliente USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET nombre_contacto = CONVERT(CAST(CONVERT(nombre_contacto USING latin1) AS BINARY) USING utf8mb4) WHERE nombre_contacto REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(nombre_contacto USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET apellido_contacto = CONVERT(CAST(CONVERT(apellido_contacto USING latin1) AS BINARY) USING utf8mb4) WHERE apellido_contacto REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(apellido_contacto USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET telefono = CONVERT(CAST(CONVERT(telefono USING latin1) AS BINARY) USING utf8mb4) WHERE telefono REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(telefono USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET fax = CONVERT(CAST(CONVERT(fax USING latin1) AS BINARY) USING utf8mb4) WHERE fax REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(fax USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET linea_direccion1 = CONVERT(CAST(CONVERT(linea_direccion1 USING latin1) AS BINARY) USING utf8mb4) WHERE linea_direccion1 REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(linea_direccion1 USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET linea_direccion2 = CONVERT(CAST(CONVERT(linea_direccion2 USING latin1) AS BINARY) USING utf8mb4) WHERE linea_direccion2 REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(linea_direccion2 USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET ciudad = CONVERT(CAST(CONVERT(ciudad USING latin1) AS BINARY) USING utf8mb4) WHERE ciudad REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(ciudad USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET region = CONVERT(CAST(CONVERT(region USING latin1) AS BINARY) USING utf8mb4) WHERE region REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(region USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET pais = CONVERT(CAST(CONVERT(pais USING latin1) AS BINARY) USING utf8mb4) WHERE pais REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(pais USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET codigo_postal = CONVERT(CAST(CONVERT(codigo_postal USING latin1) AS BINARY) USING utf8mb4) WHERE codigo_postal REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(codigo_postal USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE pedido SET estado = CONVERT(CAST(CONVERT(estado USING latin1) AS BINARY) USING utf8mb4) WHERE estado REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(estado USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE pedido SET comentarios = CONVERT(CAST(CONVERT(comentarios USING latin1) AS BINARY) USING utf8mb4) WHERE comentarios REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(comentarios USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE producto SET nombre = CONVERT(CAST(CONVERT(nombre USING latin1) AS BINARY) USING utf8mb4) WHERE nombre REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(nombre USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE producto SET gama = CONVERT(CAST(CONVERT(gama USING latin1) AS BINARY) USING utf8mb4) WHERE gama REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(gama USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE producto SET dimensiones = CONVERT(CAST(CONVERT(dimensiones USING latin1) AS BINARY) USING utf8mb4) WHERE dimensiones REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(dimensiones USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE producto SET proveedor = CONVERT(CAST(CONVERT(proveedor USING latin1) AS BINARY) USING utf8mb4) WHERE proveedor REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(proveedor USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE producto SET descripcion = CONVERT(CAST(CONVERT(descripcion USING latin1) AS BINARY) USING utf8mb4) WHERE descripcion REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(descripcion USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE pago SET forma_pago = CONVERT(CAST(CONVERT(forma_pago USING latin1) AS BINARY) USING utf8mb4) WHERE forma_pago REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(forma_pago USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE pago SET id_transaccion = CONVERT(CAST(CONVERT(id_transaccion USING latin1) AS BINARY) USING utf8mb4) WHERE id_transaccion REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(id_transaccion USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;

-- Pasada 2
UPDATE oficina SET ciudad = CONVERT(CAST(CONVERT(ciudad USING latin1) AS BINARY) USING utf8mb4) WHERE ciudad REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(ciudad USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE oficina SET pais = CONVERT(CAST(CONVERT(pais USING latin1) AS BINARY) USING utf8mb4) WHERE pais REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(pais USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE oficina SET region = CONVERT(CAST(CONVERT(region USING latin1) AS BINARY) USING utf8mb4) WHERE region REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(region USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE oficina SET codigo_postal = CONVERT(CAST(CONVERT(codigo_postal USING latin1) AS BINARY) USING utf8mb4) WHERE codigo_postal REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(codigo_postal USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE oficina SET telefono = CONVERT(CAST(CONVERT(telefono USING latin1) AS BINARY) USING utf8mb4) WHERE telefono REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(telefono USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE oficina SET linea_direccion1 = CONVERT(CAST(CONVERT(linea_direccion1 USING latin1) AS BINARY) USING utf8mb4) WHERE linea_direccion1 REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(linea_direccion1 USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE oficina SET linea_direccion2 = CONVERT(CAST(CONVERT(linea_direccion2 USING latin1) AS BINARY) USING utf8mb4) WHERE linea_direccion2 REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(linea_direccion2 USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE empleado SET nombre = CONVERT(CAST(CONVERT(nombre USING latin1) AS BINARY) USING utf8mb4) WHERE nombre REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(nombre USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE empleado SET apellido1 = CONVERT(CAST(CONVERT(apellido1 USING latin1) AS BINARY) USING utf8mb4) WHERE apellido1 REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(apellido1 USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE empleado SET apellido2 = CONVERT(CAST(CONVERT(apellido2 USING latin1) AS BINARY) USING utf8mb4) WHERE apellido2 REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(apellido2 USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE empleado SET extension = CONVERT(CAST(CONVERT(extension USING latin1) AS BINARY) USING utf8mb4) WHERE extension REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(extension USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE empleado SET email = CONVERT(CAST(CONVERT(email USING latin1) AS BINARY) USING utf8mb4) WHERE email REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(email USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE empleado SET puesto = CONVERT(CAST(CONVERT(puesto USING latin1) AS BINARY) USING utf8mb4) WHERE puesto REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(puesto USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE gama_producto SET gama = CONVERT(CAST(CONVERT(gama USING latin1) AS BINARY) USING utf8mb4) WHERE gama REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(gama USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE gama_producto SET descripcion_texto = CONVERT(CAST(CONVERT(descripcion_texto USING latin1) AS BINARY) USING utf8mb4) WHERE descripcion_texto REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(descripcion_texto USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE gama_producto SET descripcion_html = CONVERT(CAST(CONVERT(descripcion_html USING latin1) AS BINARY) USING utf8mb4) WHERE descripcion_html REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(descripcion_html USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE gama_producto SET imagen = CONVERT(CAST(CONVERT(imagen USING latin1) AS BINARY) USING utf8mb4) WHERE imagen REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(imagen USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET nombre_cliente = CONVERT(CAST(CONVERT(nombre_cliente USING latin1) AS BINARY) USING utf8mb4) WHERE nombre_cliente REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(nombre_cliente USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET nombre_contacto = CONVERT(CAST(CONVERT(nombre_contacto USING latin1) AS BINARY) USING utf8mb4) WHERE nombre_contacto REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(nombre_contacto USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET apellido_contacto = CONVERT(CAST(CONVERT(apellido_contacto USING latin1) AS BINARY) USING utf8mb4) WHERE apellido_contacto REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(apellido_contacto USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET telefono = CONVERT(CAST(CONVERT(telefono USING latin1) AS BINARY) USING utf8mb4) WHERE telefono REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(telefono USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET fax = CONVERT(CAST(CONVERT(fax USING latin1) AS BINARY) USING utf8mb4) WHERE fax REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(fax USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET linea_direccion1 = CONVERT(CAST(CONVERT(linea_direccion1 USING latin1) AS BINARY) USING utf8mb4) WHERE linea_direccion1 REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(linea_direccion1 USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET linea_direccion2 = CONVERT(CAST(CONVERT(linea_direccion2 USING latin1) AS BINARY) USING utf8mb4) WHERE linea_direccion2 REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(linea_direccion2 USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET ciudad = CONVERT(CAST(CONVERT(ciudad USING latin1) AS BINARY) USING utf8mb4) WHERE ciudad REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(ciudad USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET region = CONVERT(CAST(CONVERT(region USING latin1) AS BINARY) USING utf8mb4) WHERE region REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(region USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET pais = CONVERT(CAST(CONVERT(pais USING latin1) AS BINARY) USING utf8mb4) WHERE pais REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(pais USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE cliente SET codigo_postal = CONVERT(CAST(CONVERT(codigo_postal USING latin1) AS BINARY) USING utf8mb4) WHERE codigo_postal REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(codigo_postal USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE pedido SET estado = CONVERT(CAST(CONVERT(estado USING latin1) AS BINARY) USING utf8mb4) WHERE estado REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(estado USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE pedido SET comentarios = CONVERT(CAST(CONVERT(comentarios USING latin1) AS BINARY) USING utf8mb4) WHERE comentarios REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(comentarios USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE producto SET nombre = CONVERT(CAST(CONVERT(nombre USING latin1) AS BINARY) USING utf8mb4) WHERE nombre REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(nombre USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE producto SET gama = CONVERT(CAST(CONVERT(gama USING latin1) AS BINARY) USING utf8mb4) WHERE gama REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(gama USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE producto SET dimensiones = CONVERT(CAST(CONVERT(dimensiones USING latin1) AS BINARY) USING utf8mb4) WHERE dimensiones REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(dimensiones USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE producto SET proveedor = CONVERT(CAST(CONVERT(proveedor USING latin1) AS BINARY) USING utf8mb4) WHERE proveedor REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(proveedor USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE producto SET descripcion = CONVERT(CAST(CONVERT(descripcion USING latin1) AS BINARY) USING utf8mb4) WHERE descripcion REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(descripcion USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE pago SET forma_pago = CONVERT(CAST(CONVERT(forma_pago USING latin1) AS BINARY) USING utf8mb4) WHERE forma_pago REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(forma_pago USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;
UPDATE pago SET id_transaccion = CONVERT(CAST(CONVERT(id_transaccion USING latin1) AS BINARY) USING utf8mb4) WHERE id_transaccion REGEXP 'Ã|Â' AND CONVERT(CAST(CONVERT(id_transaccion USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;
UPDATE cliente SET nombre_contacto = 'François' WHERE nombre_contacto = 'Fraçois';
UPDATE gama_producto SET descripcion_texto = 'Plantas para jardín decorativas' WHERE gama = 'Herbaceas' AND descripcion_texto = 'Plantas para jardin decorativas';
