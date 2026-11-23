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
