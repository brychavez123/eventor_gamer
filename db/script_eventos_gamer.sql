-- =====================================================================
-- Script de creación e inserción de datos - Eventos Gamer (MySQL 8)
-- Ejecutar como root:   mysql -u root -p < script_eventos_gamer.sql
-- En Docker este mismo contenido se carga automáticamente
-- (backend/src/main/resources/schema.sql + data.sql vía db/02_cargar_datos.sh, y db/03_usuario_app.sh).
-- =====================================================================

DROP DATABASE IF EXISTS eventos_gamer;
CREATE DATABASE eventos_gamer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eventos_gamer;

-- =====================================================================
-- Esquema de la base de datos de Eventos Gamer
-- Compatible con MySQL 8 (y con H2 en modo MySQL para pruebas locales)
-- =====================================================================

CREATE TABLE IF NOT EXISTS usuario (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,          -- hash BCrypt, nunca texto plano
    nombre        VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL DEFAULT 'USER',
    activo        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS evento (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre            VARCHAR(120) NOT NULL,
    videojuego        VARCHAR(80)  NOT NULL,
    categoria         VARCHAR(40)  NOT NULL,      -- Torneo, LAN Party, Convención, Meetup, Lanzamiento
    pais              VARCHAR(40)  NOT NULL,
    ciudad            VARCHAR(40)  NOT NULL,
    fecha_hora        DATETIME     NOT NULL,
    modalidad         VARCHAR(12)  NOT NULL,      -- PRESENCIAL u ONLINE
    ubicacion         VARCHAR(200),
    enlace_acceso     VARCHAR(200),
    organizador       VARCHAR(100) NOT NULL,
    descripcion_corta VARCHAR(255) NOT NULL,
    descripcion       VARCHAR(2000) NOT NULL,
    requisitos        VARCHAR(1000),
    inscripcion       VARCHAR(1000),
    cupos             INT          NOT NULL DEFAULT 0,
    inscritos         INT          NOT NULL DEFAULT 0,
    imagen_portada    VARCHAR(200) NOT NULL,
    fecha_publicacion DATETIME     NOT NULL,
    CONSTRAINT ck_evento_modalidad CHECK (modalidad IN ('PRESENCIAL', 'ONLINE'))
);

CREATE TABLE IF NOT EXISTS evento_foto (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    evento_id         BIGINT       NOT NULL,
    url               VARCHAR(200) NOT NULL,
    texto_alternativo VARCHAR(150) NOT NULL,
    orden             INT          NOT NULL DEFAULT 1,
    CONSTRAINT fk_foto_evento FOREIGN KEY (evento_id) REFERENCES evento (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS banner (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(80)  NOT NULL,
    texto  VARCHAR(200) NOT NULL,
    imagen VARCHAR(200) NOT NULL,
    enlace VARCHAR(200),
    activo BOOLEAN      NOT NULL DEFAULT TRUE,
    orden  INT          NOT NULL DEFAULT 1
);

-- ---------------------------------------------------------------------
-- Semana 5: fotos/videos subidos por usuarios, comentarios con valoración
-- y registro de eventos compartidos (en el sitio y en redes sociales).
-- ---------------------------------------------------------------------

-- Metadatos del archivo subido. El contenido va en otra tabla para que listar
-- la galería nunca cargue los bytes de los archivos.
CREATE TABLE IF NOT EXISTS evento_medio (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    evento_id    BIGINT       NOT NULL,
    usuario_id   BIGINT       NOT NULL,
    tipo         VARCHAR(10)  NOT NULL,           -- FOTO o VIDEO
    content_type VARCHAR(40)  NOT NULL,           -- detectado por el servidor (firma del archivo), no el que envía el cliente
    descripcion  VARCHAR(150) NOT NULL,
    tamano       INT          NOT NULL,
    fecha_subida DATETIME     NOT NULL,
    CONSTRAINT ck_medio_tipo CHECK (tipo IN ('FOTO', 'VIDEO')),
    CONSTRAINT ck_medio_content_type CHECK (content_type IN ('image/jpeg', 'image/png', 'video/mp4', 'video/webm')),
    CONSTRAINT fk_medio_evento FOREIGN KEY (evento_id) REFERENCES evento (id) ON DELETE CASCADE,
    CONSTRAINT fk_medio_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE IF NOT EXISTS evento_medio_contenido (
    medio_id BIGINT   PRIMARY KEY,
    datos    LONGBLOB NOT NULL,
    CONSTRAINT fk_contenido_medio FOREIGN KEY (medio_id) REFERENCES evento_medio (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS comentario (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    evento_id  BIGINT       NOT NULL,
    usuario_id BIGINT       NOT NULL,
    valoracion TINYINT      NOT NULL,             -- 1 a 5 estrellas
    texto      VARCHAR(500) NOT NULL,
    fecha      DATETIME     NOT NULL,
    CONSTRAINT ck_comentario_valoracion CHECK (valoracion BETWEEN 1 AND 5),
    CONSTRAINT fk_comentario_evento FOREIGN KEY (evento_id) REFERENCES evento (id) ON DELETE CASCADE,
    CONSTRAINT fk_comentario_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE IF NOT EXISTS evento_compartido (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    evento_id  BIGINT       NOT NULL,
    usuario_id BIGINT       NOT NULL,
    destino    VARCHAR(12)  NOT NULL,             -- SITIO (muro de la comunidad), FACEBOOK, X o WHATSAPP
    mensaje    VARCHAR(280),
    fecha      DATETIME     NOT NULL,
    CONSTRAINT ck_compartido_destino CHECK (destino IN ('SITIO', 'FACEBOOK', 'X', 'WHATSAPP')),
    CONSTRAINT fk_compartido_evento FOREIGN KEY (evento_id) REFERENCES evento (id) ON DELETE CASCADE,
    CONSTRAINT fk_compartido_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE INDEX idx_evento_fecha ON evento (fecha_hora);
CREATE INDEX idx_evento_publicacion ON evento (fecha_publicacion);

-- =====================================================================
-- Datos de prueba de Eventos Gamer (juegos de estrategia y títulos de nicho)
-- Usuarios de prueba (contraseña en texto plano SOLO en este comentario,
-- en la base de datos se guarda el hash BCrypt):
--   jugador1    / Jugador1#2026
--   jugador2    / Jugador2#2026
--   organizador / Organiza#2026
-- =====================================================================

INSERT INTO usuario (username, password_hash, nombre, rol, activo) VALUES
('jugador1',    '$2a$12$Jc5rbwQtUeD/V4aehvwAYeH8m7erfqmVjURjZ700clgDnud9XjXna', 'Camila Rojas',  'USER', TRUE),
('jugador2',    '$2a$12$ydjQ1rtlSsu5x4NM2iJ0XO8L/m/edgY7QJdX4Soc.e17pB/2tPDJK', 'Diego Muñoz',   'USER', TRUE),
('organizador', '$2a$12$G6MacSgvIKRrRdKRvQU2fu2itaZH2oN.tEbp3lu7B8/s5b5xsiyjm', 'Valentina Soto','USER', TRUE);

INSERT INTO evento (nombre, videojuego, categoria, pais, ciudad, fecha_hora, modalidad, ubicacion, enlace_acceso,
                    organizador, descripcion_corta, descripcion, requisitos, inscripcion, cupos, inscritos,
                    imagen_portada, fecha_publicacion) VALUES
('Copa Imperium: Rome Total War', 'Rome: Total War', 'Torneo', 'Chile', 'Santiago', '2026-11-21 15:00:00', 'PRESENCIAL',
 'Centro Cultural Estación Mapocho, Santiago', NULL, 'Legión Estratega Chile',
 'Torneo 1v1 de batallas personalizadas con facciones históricas y presupuesto fijo de denarios.',
 'La Copa Imperium reúne a 32 generales en batallas terrestres personalizadas de Rome: Total War. Formato de doble eliminación, mapas aleatorios entre llanura, bosque y río, y presupuesto fijo de 10.000 denarios por ejército. Las finales se transmiten con comentarios en vivo y repaso táctico de cada batalla.',
 'Mayores de 15 años. Traer mouse propio. Se juega la versión Remastered con reglas de la comunidad (sin unidades de elefantes en ronda 1).',
 'Inscripción en el mesón de acreditación o con el organizador. Valor: $5.000.',
 32, 30, 'img/eventos/torneo-1.jpg', '2026-10-30 10:00:00'),

('LAN Party Valparaíso: Age of Empires II', 'Age of Empires II', 'LAN Party', 'Chile', 'Valparaíso', '2026-12-05 10:00:00', 'PRESENCIAL',
 'Centro de Eventos Duoc UC, sede Valparaíso', NULL, 'Club Estrategia Porteño',
 '48 horas de LAN con partidas 4v4 de Age of Empires II y torneo Arabia 1v1.',
 'Trae tu PC y vive 48 horas de Age of Empires II: torneo 1v1 en Arabia, partidas 4v4 en Black Forest, desafíos de "regicidio" y una noche temática con mapas de la campaña de Juana de Arco. Red local de 10 Gbps y energía respaldada.',
 'Traer PC o notebook con Age of Empires II Definitive Edition, audífonos y cable de red. Mayores de 15 años (menores con autorización).',
 'Entrada general $15.000 por los dos días. Cupos limitados por puesto de red.',
 120, 104, 'img/eventos/lan.jpg', '2026-10-25 09:00:00'),

('Asedio en Concepción: Stronghold Crusader', 'Stronghold Crusader', 'Torneo', 'Chile', 'Concepción', '2026-12-12 11:00:00', 'PRESENCIAL',
 'SurActivo, Talcahuano', NULL, 'Castellanos del Sur',
 'Torneo de asedios en Stronghold Crusader: construye, defiende y derriba murallas.',
 'Torneo de Stronghold Crusader en formato todos contra todos de cuatro jugadores por mesa. Gana quien resista más tiempo o derribe el último castillo. Incluye charla sobre economía de castillos y exhibición de mapas creados por la comunidad.',
 'Abierto a mayores de 12 años. Se usa Stronghold Crusader HD o Definitive Edition.',
 'Inscripción gratuita con registro previo en el mesón del recinto.',
 64, 51, 'img/eventos/torneo-2.jpg', '2026-10-15 12:00:00'),

('Liga Online Medieval II: Total War', 'Medieval II: Total War', 'Torneo', 'Argentina', 'Buenos Aires', '2026-11-28 18:00:00', 'ONLINE',
 NULL, 'https://discord.gg/ligamedieval2', 'Comunidad Total War Latam',
 'Liga online por equipos de 2v2 con facciones europeas y orientales.',
 'Liga online de Medieval II: Total War en formato 2v2. Cada equipo elige facciones antes de la ronda y no puede repetirlas. Las mejores batallas se analizan en un stream semanal con jugadores veteranos de la comunidad.',
 'Duplas con Medieval II: Total War (versión Steam), Discord obligatorio y conexión estable.',
 'Inscripción gratuita. El capitán registra a la dupla en el servidor de Discord de la liga.',
 48, 39, 'img/eventos/online-1.jpg', '2026-11-02 08:30:00'),

('Meetup Heroes of Might and Magic III', 'Heroes of Might and Magic III', 'Meetup', 'Chile', 'Santiago', '2026-11-14 17:00:00', 'PRESENCIAL',
 'Café Pixel, Providencia', NULL, 'Gremio de Héroes Chile',
 'Tarde de partidas hot-seat, mapas clásicos y charla sobre el template Jebus Cross.',
 'Encuentro de la comunidad de Heroes III con partidas hot-seat, mapas aleatorios y una charla sobre la estrategia del template Jebus Cross. Trae tu facción favorita y tus mejores historias de castillos perdidos en el turno 112.',
 'Ninguno. Si quieres jugar, trae tu notebook con Heroes III (HotA recomendado).',
 'Entrada liberada con registro previo. Cupo máximo de 40 personas.',
 40, 33, 'img/eventos/meetup-1.jpg', '2026-11-04 19:00:00'),

('Lanzamiento de mod: Europa Barbarorum Reforged', 'Rome: Total War', 'Lanzamiento', 'Perú', 'Lima', '2026-12-18 20:00:00', 'ONLINE',
 NULL, 'https://www.youtube.com/@europabarbarorum', 'Equipo EB Reforged',
 'Presentación en vivo de la nueva versión del mod histórico para Rome: Total War.',
 'El equipo de desarrollo presenta en vivo la nueva versión del mod: facciones rediseñadas, mapa de campaña ampliado y unidades con nombres en su idioma original. Habrá ronda de preguntas y partidas de demostración.',
 'Tener Rome: Total War original instalado para probar el mod después de la presentación.',
 'Acceso libre a la transmisión. No requiere registro.',
 1000, 412, 'img/eventos/lanzamiento.jpg', '2026-11-05 14:00:00'),

('Torneo Empire Earth: Edades de la Humanidad', 'Empire Earth', 'Torneo', 'Chile', 'Santiago', '2026-11-29 12:00:00', 'PRESENCIAL',
 'Duoc UC, sede San Joaquín', NULL, 'Estrategas Retro',
 'Torneo 1v1 desde la Edad de Piedra hasta la Era Digital.',
 'Partidas 1v1 con inicio en la Edad de Piedra y límite en la Era Digital. Formato de grupos y eliminación directa. Exhibición de mapas clásicos y premios para los tres primeros lugares.',
 'Traer notebook con Empire Earth (Gold Edition). Edad mínima 13 años.',
 'Inscripción $3.000 en el mesón de acreditación.',
 32, 22, 'img/eventos/torneo-3.jpg', '2026-10-28 11:00:00'),

('Copa Online Company of Heroes', 'Company of Heroes', 'Torneo', 'México', 'Ciudad de México', '2026-12-03 20:00:00', 'ONLINE',
 NULL, 'https://www.twitch.tv/copacoh', 'Frente Táctico MX',
 'Torneo 1v1 de Company of Heroes con mapas competitivos de la comunidad.',
 'Torneo online de Company of Heroes en mapas 1v1 competitivos. Fase de grupos y eliminación directa. Semifinales y final transmitidas con comentarios y análisis de posiciones.',
 'Company of Heroes (Steam), conexión estable y cuenta en el servidor del torneo.',
 'Inscripción gratuita en el servidor de Discord del torneo.',
 64, 47, 'img/eventos/online-2.jpg', '2026-11-01 16:00:00'),

('Retro Estrategia Day', 'Command & Conquer: Red Alert 2', 'Convención', 'Chile', 'Viña del Mar', '2027-01-09 11:00:00', 'PRESENCIAL',
 'Quinta Vergara, Viña del Mar', NULL, 'Retro Gamers V Región',
 'Jornada de estrategia en tiempo real clásica: Red Alert 2, Warcraft III y Rise of Nations.',
 'Un día dedicado a la estrategia en tiempo real clásica: freeplay de Red Alert 2, Warcraft III y Rise of Nations, feria de intercambio de cajas originales, torneo relámpago de Red Alert 2 y charla sobre la historia del género.',
 'Abierto a todo público.',
 'Entrada general $4.000, menores de 10 años gratis.',
 300, 158, 'img/eventos/convencion.jpg', '2026-10-20 10:00:00'),

('Campaña Online Crusader Kings III', 'Crusader Kings III', 'Meetup', 'Colombia', 'Bogotá', '2026-11-22 19:00:00', 'ONLINE',
 NULL, 'https://discord.gg/ck3latam', 'Casa Real Latam',
 'Partida multijugador de rol dinástico con reinos en guerra y alianzas.',
 'Partida multijugador de Crusader Kings III con 12 jugadores, cada uno a cargo de una dinastía. Sesiones semanales con narrador que documenta matrimonios, traiciones y guerras santas.',
 'Crusader Kings III con todos los jugadores en la misma versión, Discord con micrófono.',
 'Registro gratuito en el servidor de Discord; cupos asignados por orden de llegada.',
 12, 12, 'img/eventos/online-3.jpg', '2026-10-29 13:00:00'),

('Meetup Mount & Blade: Warband', 'Mount & Blade: Warband', 'Meetup', 'Chile', 'Santiago', '2026-12-10 19:00:00', 'PRESENCIAL',
 'Café Pixel, Providencia', NULL, 'Calradia Chile',
 'Batallas multijugador de caballería y asedios en LAN.',
 'Noche de Mount & Blade: Warband con batallas multijugador de hasta 30 jugadores en LAN, asedios con escaleras y torres, y una charla sobre los mods más queridos por la comunidad.',
 'Traer notebook con Mount & Blade: Warband y mouse.',
 'Entrada liberada con registro previo.',
 30, 26, 'img/eventos/meetup-2.jpg', '2026-11-03 09:00:00'),

('Anno 1800: Desafío de Ciudades', 'Anno 1800', 'Torneo', 'Chile', 'Antofagasta', '2027-01-23 16:00:00', 'PRESENCIAL',
 'Centro de Convenciones Antofagasta', NULL, 'Norte Estratega',
 'Competencia de construcción económica: la isla más próspera en 3 horas gana.',
 'Cada participante parte con la misma isla y recursos. Gana quien alcance la mayor población de inversionistas en tres horas de juego. El público puede seguir las islas en pantallas gigantes.',
 'Notebook con Anno 1800 y partida guardada inicial entregada por la organización.',
 'Inscripción $3.000; público general gratis.',
 24, 14, 'img/eventos/torneo-4.jpg', '2026-10-10 10:00:00');

INSERT INTO evento_foto (evento_id, url, texto_alternativo, orden) VALUES
(1, 'img/fotos/escenario.jpg', 'Escenario principal de la Copa Imperium', 1),
(1, 'img/fotos/publico.jpg',   'Público siguiendo las batallas', 2),
(2, 'img/fotos/lan.jpg',       'Puestos de juego en la LAN Party', 1),
(2, 'img/fotos/publico.jpg',   'Asistentes en la zona de descanso', 2),
(3, 'img/fotos/escenario.jpg', 'Mesas del torneo de asedios', 1),
(3, 'img/fotos/publico.jpg',   'Asistentes recorriendo el recinto', 2),
(4, 'img/fotos/stream.jpg',    'Transmisión online de la liga', 1),
(5, 'img/fotos/publico.jpg',   'Comunidad de Heroes III reunida', 1),
(6, 'img/fotos/stream.jpg',    'Presentación del mod en vivo', 1),
(7, 'img/fotos/escenario.jpg', 'Final del torneo de Empire Earth', 1),
(8, 'img/fotos/stream.jpg',    'Transmisión de la Copa Company of Heroes', 1),
(9, 'img/fotos/retro.jpg',   'Feria de cajas originales y retro', 1),
(10, 'img/fotos/stream.jpg',   'Mapa de la campaña multijugador', 1),
(11, 'img/fotos/lan.jpg',      'Jugadores en la batalla LAN', 1),
(12, 'img/fotos/escenario.jpg','Pantallas con las islas de los participantes', 1);

-- Comentarios y valoraciones de ejemplo (usuario_id: 1 = jugador1, 2 = jugador2, 3 = organizador)
INSERT INTO comentario (evento_id, usuario_id, valoracion, texto, fecha) VALUES
(1, 2, 5, 'Excelente nivel de los generales y muy buena transmisión de las finales.', '2026-11-01 18:20:00'),
(1, 3, 4, 'Faltó un poco de espacio en la zona de espera, pero el torneo estuvo impecable.', '2026-11-02 10:05:00'),
(2, 1, 5, 'La red no se cayó ni una vez en 48 horas. ¡Volveré el próximo año!', '2026-11-03 21:40:00'),
(5, 2, 4, 'Muy buena charla sobre Jebus Cross, aprendí bastante.', '2026-11-06 16:10:00');

-- Eventos compartidos de ejemplo (SITIO = aparece en el muro de la comunidad)
INSERT INTO evento_compartido (evento_id, usuario_id, destino, mensaje, fecha) VALUES
(2, 1, 'SITIO', '¿Alguien se anima a armar equipo 4v4 para la LAN?', '2026-11-03 21:45:00'),
(4, 2, 'SITIO', 'Busco dupla para la liga 2v2, juego con facciones orientales.', '2026-11-05 12:30:00'),
(1, 3, 'FACEBOOK', NULL, '2026-11-02 10:10:00');

INSERT INTO banner (titulo, texto, imagen, enlace, activo, orden) VALUES
('PixelGear Week',     '20% de descuento en mouse y teclados para estrategas.',     'img/banners/perifericos.jpg', NULL, TRUE, 1),
('Fibra Gamer 1 Gbps', 'Internet con baja latencia para tus partidas online.',      'img/banners/internet.jpg',    NULL, TRUE, 2),
('Tienda Retro Box',   'Juegos de estrategia clásicos en caja original.',           'img/banners/retro.jpg',       NULL, TRUE, 3);

-- ---------------------------------------------------------------------
-- Usuario de la aplicación con privilegio mínimo:
--   SELECT en todo; INSERT solo en las tablas de la semana 5; DELETE solo en comentario.
-- Cambiar la contraseña antes de ejecutar.
-- ---------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'eventos_app'@'%' IDENTIFIED BY 'CAMBIAR_ESTA_CLAVE';
GRANT SELECT ON eventos_gamer.* TO 'eventos_app'@'%';
GRANT INSERT ON eventos_gamer.evento_medio TO 'eventos_app'@'%';
GRANT INSERT ON eventos_gamer.evento_medio_contenido TO 'eventos_app'@'%';
GRANT INSERT, DELETE ON eventos_gamer.comentario TO 'eventos_app'@'%';
GRANT INSERT ON eventos_gamer.evento_compartido TO 'eventos_app'@'%';
FLUSH PRIVILEGES;
