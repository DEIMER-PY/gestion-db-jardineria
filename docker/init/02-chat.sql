-- Mensajería del equipo: canales, mensajes y última lectura por empleado.
-- Tablas nuevas; no modifica las tablas originales de jardinería.
-- Es idempotente: en un volumen ya creado se aplica con
--   docker exec -i jardineria-mysql mysql -uroot -ppassword jardineria < docker/init/02-chat.sql
SET NAMES utf8mb4;
USE jardineria;

CREATE TABLE IF NOT EXISTS canal (
  id_canal INTEGER NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(60) NOT NULL,
  tipo ENUM('GENERAL', 'OFICINA', 'DIRECTO') NOT NULL,
  codigo_oficina VARCHAR(10) DEFAULT NULL,
  descripcion VARCHAR(150) DEFAULT NULL,
  creado DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_canal),
  UNIQUE KEY uk_canal_nombre (nombre),
  FOREIGN KEY (codigo_oficina) REFERENCES oficina (codigo_oficina)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mensaje (
  id_mensaje BIGINT NOT NULL AUTO_INCREMENT,
  id_canal INTEGER NOT NULL,
  codigo_empleado INTEGER NOT NULL,
  texto VARCHAR(2000) NOT NULL,
  enviado DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id_mensaje),
  KEY idx_mensaje_canal (id_canal, id_mensaje),
  FOREIGN KEY (id_canal) REFERENCES canal (id_canal) ON DELETE CASCADE,
  FOREIGN KEY (codigo_empleado) REFERENCES empleado (codigo_empleado)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lectura (
  id_canal INTEGER NOT NULL,
  codigo_empleado INTEGER NOT NULL,
  ultimo_mensaje BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id_canal, codigo_empleado),
  FOREIGN KEY (id_canal) REFERENCES canal (id_canal) ON DELETE CASCADE,
  FOREIGN KEY (codigo_empleado) REFERENCES empleado (codigo_empleado)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Canal general y uno por oficina (los mensajes directos se crean al escribir el primero)
INSERT IGNORE INTO canal (nombre, tipo, descripcion)
VALUES ('general', 'GENERAL', 'Avisos para todo el equipo');

INSERT IGNORE INTO canal (nombre, tipo, codigo_oficina, descripcion)
SELECT CONCAT('oficina-', LOWER(codigo_oficina)), 'OFICINA', codigo_oficina, CONCAT('Equipo de ', ciudad)
FROM oficina;

-- Conversación de ejemplo, solo si el canal general está vacío.
-- Las fechas se guardan en UTC (el servidor y la conexión JDBC usan UTC); la aplicación las muestra en hora local.
INSERT INTO mensaje (id_canal, codigo_empleado, texto, enviado)
SELECT c.id_canal, m.codigo_empleado, m.texto, m.enviado
FROM canal c
JOIN (
  SELECT 1 AS codigo_empleado, 'Buenos días, equipo. Esta semana revisamos los pedidos pendientes de enero.' AS texto,
         TIMESTAMP('2026-11-12 13:02:00') AS enviado
  UNION ALL SELECT 3, 'El #14 sigue pendiente; @felipe.rosas, ¿puedes llamar al cliente?', TIMESTAMP('2026-11-12 13:10:00')
  UNION ALL SELECT 5, 'Hecho: entregan el lunes. Actualizo el estado cuando llegue.', TIMESTAMP('2026-11-12 13:24:00')
  UNION ALL SELECT 2, 'Recordad que el stock de Herbáceas está bajo mínimos.', TIMESTAMP('2026-11-12 14:05:00')
) m
WHERE c.nombre = 'general'
  AND NOT EXISTS (SELECT 1 FROM mensaje x WHERE x.id_canal = c.id_canal);
