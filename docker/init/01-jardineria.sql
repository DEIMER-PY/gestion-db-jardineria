-- Forzar UTF-8 en la sesion de carga: evita la doble codificacion de tildes y enies.
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS jardineria;
USE jardineria;

CREATE TABLE oficina (
  codigo_oficina VARCHAR(10) NOT NULL,
  ciudad VARCHAR(30) NOT NULL,
  pais VARCHAR(50) NOT NULL,
  region VARCHAR(50) DEFAULT NULL,
  codigo_postal VARCHAR(10) NOT NULL,
  telefono VARCHAR(20) NOT NULL,
  linea_direccion1 VARCHAR(50) NOT NULL,
  linea_direccion2 VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (codigo_oficina)
);

CREATE TABLE empleado (
  codigo_empleado INTEGER NOT NULL,
  nombre VARCHAR(50) NOT NULL,
  apellido1 VARCHAR(50) NOT NULL,
  apellido2 VARCHAR(50) DEFAULT NULL,
  extension VARCHAR(10) NOT NULL,
  email VARCHAR(100) NOT NULL,
  codigo_oficina VARCHAR(10) NOT NULL,
  codigo_jefe INTEGER DEFAULT NULL,
  puesto VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (codigo_empleado),
  FOREIGN KEY (codigo_oficina) REFERENCES oficina (codigo_oficina),
  FOREIGN KEY (codigo_jefe) REFERENCES empleado (codigo_empleado)
);

CREATE TABLE gama_producto (
  gama VARCHAR(50) NOT NULL,
  descripcion_texto TEXT,
  descripcion_html TEXT,
  imagen VARCHAR(256),
  PRIMARY KEY (gama)
);

CREATE TABLE cliente (
  codigo_cliente INTEGER NOT NULL,
  nombre_cliente VARCHAR(50) NOT NULL,
  nombre_contacto VARCHAR(30) DEFAULT NULL,
  apellido_contacto VARCHAR(30) DEFAULT NULL,
  telefono VARCHAR(15) NOT NULL,
  fax VARCHAR(15) NOT NULL,
  linea_direccion1 VARCHAR(50) NOT NULL,
  linea_direccion2 VARCHAR(50) DEFAULT NULL,
  ciudad VARCHAR(50) NOT NULL,
  region VARCHAR(50) DEFAULT NULL,
  pais VARCHAR(50) DEFAULT NULL,
  codigo_postal VARCHAR(10) DEFAULT NULL,
  codigo_empleado_rep_ventas INTEGER DEFAULT NULL,
  limite_credito NUMERIC(15,2) DEFAULT NULL,
  PRIMARY KEY (codigo_cliente),
  FOREIGN KEY (codigo_empleado_rep_ventas) REFERENCES empleado (codigo_empleado)
);

CREATE TABLE pedido (
  codigo_pedido INTEGER NOT NULL,
  fecha_pedido date NOT NULL,
  fecha_esperada date NOT NULL,
  fecha_entrega date DEFAULT NULL,
  estado VARCHAR(15) NOT NULL,
  comentarios TEXT,
  codigo_cliente INTEGER NOT NULL,
  PRIMARY KEY (codigo_pedido),
  FOREIGN KEY (codigo_cliente) REFERENCES cliente (codigo_cliente)
);

CREATE TABLE producto (
  codigo_producto VARCHAR(15) NOT NULL,
  nombre VARCHAR(70) NOT NULL,
  gama VARCHAR(50) NOT NULL,
  dimensiones VARCHAR(25) NULL,
  proveedor VARCHAR(50) DEFAULT NULL,
  descripcion text NULL,
  cantidad_en_stock SMALLINT NOT NULL,
  precio_venta NUMERIC(15,2) NOT NULL,
  precio_proveedor NUMERIC(15,2) DEFAULT NULL,
  PRIMARY KEY (codigo_producto),
  FOREIGN KEY (gama) REFERENCES gama_producto (gama)
);

CREATE TABLE detalle_pedido (
  codigo_pedido INTEGER NOT NULL,
  codigo_producto VARCHAR(15) NOT NULL,
  cantidad INTEGER NOT NULL,
  precio_unidad NUMERIC(15,2) NOT NULL,
  numero_linea SMALLINT NOT NULL,
  PRIMARY KEY (codigo_pedido, codigo_producto),
  FOREIGN KEY (codigo_pedido) REFERENCES pedido (codigo_pedido),
  FOREIGN KEY (codigo_producto) REFERENCES producto (codigo_producto)
);

CREATE TABLE pago (
  codigo_cliente INTEGER NOT NULL,
  forma_pago VARCHAR(40) NOT NULL,
  id_transaccion VARCHAR(50) NOT NULL,
  fecha_pago date NOT NULL,
  total NUMERIC(15,2) NOT NULL,
  PRIMARY KEY (codigo_cliente, id_transaccion),
  FOREIGN KEY (codigo_cliente) REFERENCES cliente (codigo_cliente)
);

-- Datos
INSERT INTO oficina VALUES ('BCN-ES','Barcelona','España','Barcelona','08019','+34 93 3561182','Avenida Diagonal, 38','3A escalera Derecha');
INSERT INTO oficina VALUES ('BOS-USA','Boston','EEUU','MA','02108','+1 215 837 0825','1550 Court Place','Suite 102');
INSERT INTO oficina VALUES ('LON-UK','Londres','Inglaterra','EMEA','EC2N 1HN','+44 20 78772041','52 Old Broad Street','Ground Floor');
INSERT INTO oficina VALUES ('MAD-ES','Madrid','España','Madrid','28032','+34 91 7514487','Bulevar Indalecio Prieto, 32','');
INSERT INTO oficina VALUES ('PAR-FR','Paris','Francia','EMEA','75017','+33 14 723 4404','29 Rue Jouffroy d''abbans','');
INSERT INTO oficina VALUES ('SFC-USA','San Francisco','EEUU','CA','94080','+1 650 219 4782','100 Market Street','Suite 300');
INSERT INTO oficina VALUES ('SYD-AU','Sydney','Australia','APAC','NSW 2010','+61 2 9264 2451','5-11 Wentworth Avenue','Floor #2');
INSERT INTO oficina VALUES ('TAL-ES','Talavera de la Reina','España','Castilla-LaMancha','45632','+34 925 867231','Francisco Aguirre, 32','5º piso (exterior)');
INSERT INTO oficina VALUES ('TOK-JP','Tokyo','Japón','Chiyoda-Ku','102-8578','+81 33 224 5000','4-1 Kioicho','');

INSERT INTO empleado VALUES (1,'Marcos','Magaña','Perez','3897','marcos@jardineria.es','TAL-ES',NULL,'Director General');
INSERT INTO empleado VALUES (2,'Ruben','López','Martinez','2899','rlopez@jardineria.es','TAL-ES',1,'Subdirector Marketing');
INSERT INTO empleado VALUES (3,'Alberto','Soria','Carrasco','2837','asoria@jardineria.es','TAL-ES',2,'Subdirector Ventas');
INSERT INTO empleado VALUES (4,'Maria','Solís','Jerez','2847','msolis@jardineria.es','TAL-ES',2,'Secretaria');
INSERT INTO empleado VALUES (5,'Felipe','Rosas','Marquez','2844','frosas@jardineria.es','TAL-ES',3,'Representante Ventas');
INSERT INTO empleado VALUES (6,'Juan Carlos','Ortiz','Serrano','2845','cortiz@jardineria.es','TAL-ES',3,'Representante Ventas');
INSERT INTO empleado VALUES (7,'Carlos','Soria','Jimenez','2444','csoria@jardineria.es','MAD-ES',3,'Director Oficina');
INSERT INTO empleado VALUES (8,'Mariano','López','Murcia','2442','mlopez@jardineria.es','MAD-ES',7,'Representante Ventas');
INSERT INTO empleado VALUES (9,'Lucio','Campoamor','Martín','2442','lcampoamor@jardineria.es','MAD-ES',7,'Representante Ventas');
INSERT INTO empleado VALUES (10,'Hilario','Rodriguez','Huertas','2444','hrodriguez@jardineria.es','MAD-ES',7,'Representante Ventas');
INSERT INTO empleado VALUES (11,'Emmanuel','Magaña','Perez','2518','manu@jardineria.es','BCN-ES',3,'Director Oficina');
INSERT INTO empleado VALUES (12,'José Manuel','Martinez','De la Osa','2519','jmmart@hotmail.es','BCN-ES',11,'Representante Ventas');
INSERT INTO empleado VALUES (13,'David','Palma','Aceituno','2519','dpalma@jardineria.es','BCN-ES',11,'Representante Ventas');
INSERT INTO empleado VALUES (14,'Oscar','Palma','Aceituno','2519','opalma@jardineria.es','BCN-ES',11,'Representante Ventas');
INSERT INTO empleado VALUES (15,'Francois','Fignon','','9981','ffignon@gardening.com','PAR-FR',3,'Director Oficina');
INSERT INTO empleado VALUES (16,'Lionel','Narvaez','','9982','lnarvaez@gardening.com','PAR-FR',15,'Representante Ventas');
INSERT INTO empleado VALUES (17,'Laurent','Serra','','9982','lserra@gardening.com','PAR-FR',15,'Representante Ventas');
INSERT INTO empleado VALUES (18,'Michael','Bolton','','7454','mbolton@gardening.com','SFC-USA',3,'Director Oficina');
INSERT INTO empleado VALUES (19,'Walter Santiago','Sanchez','Lopez','7454','wssanchez@gardening.com','SFC-USA',18,'Representante Ventas');
INSERT INTO empleado VALUES (20,'Hilary','Washington','','7565','hwashington@gardening.com','BOS-USA',3,'Director Oficina');
INSERT INTO empleado VALUES (21,'Marcus','Paxton','','7565','mpaxton@gardening.com','BOS-USA',20,'Representante Ventas');
INSERT INTO empleado VALUES (22,'Lorena','Paxton','','7665','lpaxton@gardening.com','BOS-USA',20,'Representante Ventas');
INSERT INTO empleado VALUES (23,'Nei','Nishikori','','8734','nnishikori@gardening.com','TOK-JP',3,'Director Oficina');
INSERT INTO empleado VALUES (24,'Narumi','Riko','','8734','nriko@gardening.com','TOK-JP',23,'Representante Ventas');
INSERT INTO empleado VALUES (25,'Takuma','Nomura','','8735','tnomura@gardening.com','TOK-JP',23,'Representante Ventas');
INSERT INTO empleado VALUES (26,'Amy','Johnson','','3321','ajohnson@gardening.com','LON-UK',3,'Director Oficina');
INSERT INTO empleado VALUES (27,'Larry','Westfalls','','3322','lwestfalls@gardening.com','LON-UK',26,'Representante Ventas');
INSERT INTO empleado VALUES (28,'John','Walton','','3322','jwalton@gardening.com','LON-UK',26,'Representante Ventas');
INSERT INTO empleado VALUES (29,'Kevin','Fallmer','','3210','kfalmer@gardening.com','SYD-AU',3,'Director Oficina');
INSERT INTO empleado VALUES (30,'Julian','Bellinelli','','3211','jbellinelli@gardening.com','SYD-AU',29,'Representante Ventas');
INSERT INTO empleado VALUES (31,'Mariko','Kishi','','3211','mkishi@gardening.com','SYD-AU',29,'Representante Ventas');

INSERT INTO gama_producto VALUES ('Herbaceas','Plantas para jardin decorativas',NULL,NULL);
INSERT INTO gama_producto VALUES ('Herramientas','Herramientas para todo tipo de acción',NULL,NULL);
INSERT INTO gama_producto VALUES ('Aromáticas','Plantas aromáticas',NULL,NULL);
INSERT INTO gama_producto VALUES ('Frutales','Árboles pequeños de producción frutal',NULL,NULL);
INSERT INTO gama_producto VALUES ('Ornamentales','Plantas vistosas para la decoración del jardín',NULL,NULL);

INSERT INTO cliente VALUES (1,'GoldFish Garden','Daniel G','GoldFish','5556901745','5556901746','False Street 52 2 A',NULL,'San Francisco',NULL,'USA','24006',19,3000);
INSERT INTO cliente VALUES (3,'Gardening Associates','Anne','Wright','5557410345','5557410346','Wall-e Avenue',NULL,'Miami','Miami','USA','24010',19,6000);
INSERT INTO cliente VALUES (4,'Gerudo Valley','Link','Flaute','5552323129','5552323128','Oaks Avenue nº22',NULL,'New York',NULL,'USA','85495',22,12000);
INSERT INTO cliente VALUES (5,'Tendo Garden','Akane','Tendo','55591233210','55591233211','Null Street nº69',NULL,'Miami',NULL,'USA','696969',22,600000);
INSERT INTO cliente VALUES (6,'Lasas S.A.','Antonio','Lasas','34916540145','34914851312','C/Leganes 15',NULL,'Fuenlabrada','Madrid','Spain','28945',8,154310);
INSERT INTO cliente VALUES (7,'Beragua','Jose','Bermejo','654987321','916549872','C/pintor segundo','Getafe','Madrid','Madrid','Spain','28942',11,20000);
INSERT INTO cliente VALUES (8,'Club Golf Puerta del hierro','Paco','Lopez','62456810','919535678','C/sinesio delgado','Madrid','Madrid','Madrid','Spain','28930',11,40000);
INSERT INTO cliente VALUES (9,'Naturagua','Guillermo','Rengifo','689234750','916428956','C/majadahonda','Boadilla','Madrid','Madrid','Spain','28947',11,32000);
INSERT INTO cliente VALUES (10,'DaraDistribuciones','David','Serrano','675598001','916421756','C/azores','Fuenlabrada','Madrid','Madrid','Spain','28946',11,50000);
INSERT INTO cliente VALUES (11,'Madrileña de riegos','Jose','Tacaño','655983045','916689215','C/Lagañas','Fuenlabrada','Madrid','Madrid','Spain','28943',11,20000);
INSERT INTO cliente VALUES (12,'Lasas S.A.','Antonio','Lasas','34916540145','34914851312','C/Leganes 15',NULL,'Fuenlabrada','Madrid','Spain','28945',8,154310);
INSERT INTO cliente VALUES (13,'Camunas Jardines S.L.','Pedro','Camunas','34914873241','34914871541','C/Virgenes 45','C/Princesas 2 1ºB','San Lorenzo del Escorial','Madrid','Spain','28145',8,16481);
INSERT INTO cliente VALUES (14,'Dardena S.A.','Juan','Rodriguez','34912453217','34912484764','C/Nueva York 74',NULL,'Madrid','Madrid','Spain','28003',8,321000);
INSERT INTO cliente VALUES (15,'Jardin de Flores','Javier','Villar','654865643','914538776','C/ Oña 34',NULL,'Madrid','Madrid','Spain','28950',30,40000);
INSERT INTO cliente VALUES (16,'Flores Marivi','Maria','Rodriguez','666555444','912458657','C/Leganes24',NULL,'Fuenlabrada','Madrid','Spain','28945',5,1500);
INSERT INTO cliente VALUES (17,'Flowers, S.A','Beatriz','Fernandez','698754159','978453216','C/Luis Salquillo4',NULL,'Montornes del valles','Barcelona','Spain','24586',5,3500);
INSERT INTO cliente VALUES (18,'Naturajardin','Victoria','Cruz','612343529','916548735','Plaza Magallón 15',NULL,'Madrid','Madrid','Spain','28011',30,5050);
INSERT INTO cliente VALUES (19,'Golf S.A.','Luis','Martinez','916458762','912354475','C/Estancado',NULL,'Santa cruz de Tenerife','Islas Canarias','Spain','38297',12,30000);
INSERT INTO cliente VALUES (20,'Americh Golf Management SL','Mario','Suarez','964493072','964493063','C/Letardo',NULL,'Barcelona','Cataluña','Spain','12320',12,20000);
INSERT INTO cliente VALUES (21,'Aloha','Cristian','Rodrigez','916485852','914489898','C/Roman 3',NULL,'Canarias','Canarias','Spain','35488',12,50000);
INSERT INTO cliente VALUES (22,'El Prat','Francisco','Camacho','916882323','916493211','Avenida Tibidabo',NULL,'Barcelona','Cataluña','Spain','12320',12,30000);
INSERT INTO cliente VALUES (23,'Sotogrande','Maria','Santillana','915576622','914825645','C/Paseo del Parque',NULL,'Sotogrande','Cadiz','Spain','11310',12,60000);
INSERT INTO cliente VALUES (24,'Vivero Humanes','Federico','Gomez','654987690','916040875','C/Miguel Echegaray 54',NULL,'Humanes','Madrid','Spain','28970',30,7430);
INSERT INTO cliente VALUES (25,'Fuenla City','Tony','Muñoz Mena','675842139','915483754','C/Callo 52',NULL,'Fuenlabrada','Madrid','Spain','28574',5,4500);
INSERT INTO cliente VALUES (26,'Jardines y Mansiones Cactus SL','Eva María','Sánchez','916877445','914477777','Polígono Industrial Maspalomas, Nº52','Móstoles','Madrid','Madrid','Spain','29874',9,76000);
INSERT INTO cliente VALUES (27,'Jardinerías Matías SL','Matías','San Martín','916544147','917897474','C/Francisco Arce, Nº44','Bustarviejo','Madrid','Madrid','Spain','37845',9,100500);
INSERT INTO cliente VALUES (28,'Agrojardin','Benito','Lopez','675432926','916549264','C/Mar Caspio 43',NULL,'Getafe','Madrid','Spain','28904',30,8040);
INSERT INTO cliente VALUES (29,'Top Campo','Joseluis','Sanchez','685746512','974315924','C/Ibiza 32',NULL,'Humanes','Madrid','Spain','28574',5,5500);
INSERT INTO cliente VALUES (30,'Jardineria Sara','Sara','Marquez','675124537','912475843','C/Lima 1',NULL,'Fuenlabrada','Madrid','Spain','27584',5,7500);
INSERT INTO cliente VALUES (31,'Campohermoso','Luis','Jimenez','645925376','916159116','C/Peru 78',NULL,'Fuenlabrada','Madrid','Spain','28945',30,3250);
INSERT INTO cliente VALUES (32,'france telecom','François','Toulou','(33)5120578961','(33)5120578961','6 place d Alleray 15ème',NULL,'Paris',NULL,'France','75010',16,10000);
INSERT INTO cliente VALUES (33,'Musée du Louvre','Pierre','Delacroux','(33)0140205050','(33)0140205442','Quai du Louvre',NULL,'Paris',NULL,'France','75058',16,30000);
INSERT INTO cliente VALUES (35,'Tutifruti S.A','Jacob','Jones','2 9261-2433','2 9283-1695','level 24, St. Martins Tower.-31 Market St.',NULL,'Sydney','Nueva Gales del Sur','Australia','2000',31,10000);
INSERT INTO cliente VALUES (36,'Flores S.L.','Antonio','Romero','654352981','685249700','Avenida España',NULL,'Madrid','Fuenlabrada','Spain','29643',18,6000);
INSERT INTO cliente VALUES (37,'The Magic Garden','Richard','Mcain','926523468','9364875882','Lihgting Park',NULL,'London','London','United Kingdom','65930',18,10000);
INSERT INTO cliente VALUES (38,'El Jardin Viviente S.L','Justin','Smith','2 8005-7161','2 8005-7162','176 Cumberland Street The rocks',NULL,'Sydney','Nueva Gales del Sur','Australia','2003',31,8000);

INSERT INTO pedido VALUES (1,'2006-01-17','2006-01-19','2006-01-19','Entregado','Pagado a plazos',5);
INSERT INTO pedido VALUES (2,'2007-10-23','2007-10-28','2007-10-26','Entregado','La entrega llego antes de lo esperado',5);
INSERT INTO pedido VALUES (3,'2008-06-20','2008-06-25',NULL,'Rechazado','Limite de credito superado',5);
INSERT INTO pedido VALUES (4,'2009-01-20','2009-01-26',NULL,'Pendiente',NULL,5);
INSERT INTO pedido VALUES (8,'2008-11-09','2008-11-14','2008-11-14','Entregado','El cliente paga la mitad con tarjeta y la otra mitad con efectivo, se le realizan dos facturas',1);
INSERT INTO pedido VALUES (9,'2008-12-22','2008-12-27','2008-12-28','Entregado','El cliente comprueba la integridad del paquete, todo correcto',1);
INSERT INTO pedido VALUES (10,'2009-01-15','2009-01-20',NULL,'Pendiente','El cliente llama para confirmar la fecha - Esperando al proveedor',3);
INSERT INTO pedido VALUES (11,'2009-01-20','2009-01-27',NULL,'Pendiente','El cliente requiere que el pedido se le entregue de 16:00h a 22:00h',1);
INSERT INTO pedido VALUES (12,'2009-01-22','2009-01-27',NULL,'Pendiente','El cliente requiere que el pedido se le entregue de 9:00h a 13:00h',1);
INSERT INTO pedido VALUES (13,'2009-01-12','2009-01-14','2009-01-15','Entregado',NULL,7);
INSERT INTO pedido VALUES (14,'2009-01-02','2009-01-02',null,'Rechazado','mal pago',7);
INSERT INTO pedido VALUES (15,'2009-01-09','2009-01-12','2009-01-11','Entregado',NULL,7);
INSERT INTO pedido VALUES (16,'2009-01-06','2009-01-07','2009-01-15','Entregado',NULL,7);
INSERT INTO pedido VALUES (17,'2009-01-08','2009-01-09','2009-01-11','Entregado','mal estado',7);
INSERT INTO pedido VALUES (18,'2009-01-05','2009-01-06','2009-01-07','Entregado',NULL,9);
INSERT INTO pedido VALUES (19,'2009-01-18','2009-02-12',NULL,'Pendiente','entregar en murcia',9);
INSERT INTO pedido VALUES (20,'2009-01-20','2009-02-15',NULL,'Pendiente',NULL,9);
INSERT INTO pedido VALUES (21,'2009-01-09','2009-01-09','2009-01-09','Rechazado','mal pago',9);
INSERT INTO pedido VALUES (22,'2009-01-11','2009-01-11','2009-01-13','Entregado',NULL,9);
INSERT INTO pedido VALUES (23,'2008-12-30','2009-01-10',NULL,'Rechazado','El pedido fue anulado por el cliente',5);
INSERT INTO pedido VALUES (24,'2008-07-14','2008-07-31','2008-07-25','Entregado',NULL,14);
INSERT INTO pedido VALUES (25,'2009-02-02','2009-02-08',NULL,'Rechazado','El cliente carece de saldo en la cuenta asociada',1);
INSERT INTO pedido VALUES (26,'2009-02-06','2009-02-12',NULL,'Rechazado','El cliente anula la operacion para adquirir mas producto',3);
INSERT INTO pedido VALUES (27,'2009-02-07','2009-02-13',NULL,'Entregado','El pedido aparece como entregado pero no sabemos en que fecha',3);
INSERT INTO pedido VALUES (28,'2009-02-10','2009-02-17','2009-02-20','Entregado','El cliente se queja bastante de la espera asociada al producto',3);
INSERT INTO pedido VALUES (29,'2008-08-01','2008-09-01','2008-09-01','Rechazado','El cliente no está conforme con el pedido',14);
INSERT INTO pedido VALUES (30,'2008-08-03','2008-09-03','2008-08-31','Entregado',NULL,13);
INSERT INTO pedido VALUES (31,'2008-09-04','2008-09-30','2008-10-04','Rechazado','El cliente ha rechazado por llegar 5 dias tarde',13);
INSERT INTO pedido VALUES (32,'2007-01-07','2007-01-19','2007-01-27','Entregado','Entrega tardia, el cliente puso reclamacion',4);
INSERT INTO pedido VALUES (33,'2007-05-20','2007-05-28',NULL,'Rechazado','El pedido fue anulado por el cliente',4);
INSERT INTO pedido VALUES (34,'2007-06-20','2008-06-28','2008-06-28','Entregado','Pagado a plazos',4);
INSERT INTO pedido VALUES (35,'2008-03-10','2009-03-20',NULL,'Rechazado','Limite de credito superado',4);
INSERT INTO pedido VALUES (36,'2008-10-15','2008-12-15','2008-12-10','Entregado',NULL,14);
INSERT INTO pedido VALUES (37,'2008-11-03','2009-11-13',NULL,'Pendiente','El pedido nunca llego a su destino',4);
INSERT INTO pedido VALUES (38,'2009-03-05','2009-03-06','2009-03-07','Entregado',NULL,19);
INSERT INTO pedido VALUES (39,'2009-03-06','2009-03-07','2009-03-09','Pendiente',NULL,19);
INSERT INTO pedido VALUES (40,'2009-03-09','2009-03-10','2009-03-13','Rechazado',NULL,19);
INSERT INTO pedido VALUES (41,'2009-03-12','2009-03-13','2009-03-13','Entregado',NULL,19);
INSERT INTO pedido VALUES (42,'2009-03-22','2009-03-23','2009-03-27','Entregado',NULL,19);
INSERT INTO pedido VALUES (43,'2009-03-25','2009-03-26','2009-03-28','Pendiente',NULL,23);
INSERT INTO pedido VALUES (44,'2009-03-26','2009-03-27','2009-03-30','Pendiente',NULL,23);
INSERT INTO pedido VALUES (45,'2009-04-01','2009-03-04','2009-03-07','Entregado',NULL,23);
INSERT INTO pedido VALUES (46,'2009-04-03','2009-03-04','2009-03-05','Rechazado',NULL,23);
INSERT INTO pedido VALUES (47,'2009-04-15','2009-03-17','2009-03-17','Entregado',NULL,23);
INSERT INTO pedido VALUES (48,'2008-03-17','2008-03-30','2008-03-29','Entregado','Según el Cliente, el pedido llegó defectuoso',26);
INSERT INTO pedido VALUES (49,'2008-07-12','2008-07-22','2008-07-30','Entregado','El pedido llegó 1 día tarde, pero no hubo queja por parte de la empresa compradora',26);
INSERT INTO pedido VALUES (50,'2008-03-17','2008-08-09',NULL,'Pendiente','Al parecer, el pedido se ha extraviado a la altura de Sotalbo (Ávila)',26);

-- Datos de Producto
INSERT INTO producto VALUES ('AR-001','Lavanda Francesa','Aromáticas','20-30 cm','Viveros del Sur','Planta aromática con flores violetas de gran fragancia',120,9.50,5.00);
INSERT INTO producto VALUES ('AR-002','Romero Silvestre','Aromáticas','15-25 cm','Viveros del Sur','Arbusto aromático perenne ideal para setos y cocina',85,6.00,3.20);
INSERT INTO producto VALUES ('FR-001','Limonero 4 Estaciones','Frutales','120-150 cm','Frutales Ibéricos','Árbol frutal cítrico de floración continua y frutos jugosos',45,34.00,20.00);
INSERT INTO producto VALUES ('FR-002','Naranjo Dulce','Frutales','130-160 cm','Frutales Ibéricos','Árbol frutal con naranjas dulces de mesa y zumo',30,38.00,22.50);
INSERT INTO producto VALUES ('HE-001','Albahaca Verde Genovesa','Herbaceas','15-20 cm','BioHuerto','Planta anual aromática muy apreciada en gastronomía',5,3.50,1.80);
INSERT INTO producto VALUES ('HE-002','Menta Piperita','Herbaceas','20-25 cm','BioHuerto','Hierba vivaz vigorosa de agradable aroma mentolado',60,4.20,2.10);
INSERT INTO producto VALUES ('HR-001','Tijera de Podar Profesional','Herramientas','21 cm','Herramientas Bellota','Tijera ergonómica de acero forjado para corte de ramas',25,24.90,14.00);
INSERT INTO producto VALUES ('HR-002','Pala Jardinería Ergonómica','Herramientas','30 cm','Herramientas Bellota','Pala metálica de mano con mango antideslizante',40,15.50,8.50);
INSERT INTO producto VALUES ('OR-001','Orquídea Phalaenopsis','Ornamentales','40-50 cm','Exotic Flora','Orquídea tropical de flores duraderas y gran elegancia',8,19.90,11.00);
INSERT INTO producto VALUES ('OR-002','Bonsai Ficus Retusa','Ornamentales','25 cm','Exotic Flora','Árbol en miniatura de interior muy resistente y decorativo',15,45.00,26.00);

-- Datos de Detalle Pedido
INSERT INTO detalle_pedido VALUES (8,'AR-001',2,9.50,1);
INSERT INTO detalle_pedido VALUES (8,'FR-001',1,34.00,2);
INSERT INTO detalle_pedido VALUES (9,'HR-001',2,24.90,1);
INSERT INTO detalle_pedido VALUES (9,'OR-001',3,19.90,2);
INSERT INTO detalle_pedido VALUES (11,'FR-002',2,38.00,1);
INSERT INTO detalle_pedido VALUES (11,'HE-002',5,4.20,2);
INSERT INTO detalle_pedido VALUES (12,'AR-002',4,6.00,1);
INSERT INTO detalle_pedido VALUES (12,'HR-002',1,15.50,2);
INSERT INTO detalle_pedido VALUES (13,'OR-002',1,45.00,1);
INSERT INTO detalle_pedido VALUES (13,'HE-001',10,3.50,2);

-- Datos de Pago
INSERT INTO pago VALUES (1,'PayPal','ak-std-000001','2008-11-10',53.00);
INSERT INTO pago VALUES (1,'Transferencia','ak-std-000002','2008-12-25',109.50);
INSERT INTO pago VALUES (7,'Tarjeta','ak-std-000003','2009-01-15',80.00);
INSERT INTO pago VALUES (9,'Cheque','ak-std-000004','2009-01-10',150.00);
INSERT INTO pago VALUES (14,'Transferencia','ak-std-000005','2008-08-01',240.00);
