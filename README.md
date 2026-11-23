# Arena Estratega - Plataforma de eventos gamer

Aplicación web para publicar y buscar eventos de juegos de estrategia (torneos, LAN parties, meetups).
Asignatura ISY2202 - Seguridad y calidad en el desarrollo de software - Semana 3.

## Arquitectura

```
Navegador ──► nginx :443 ──► frontend :8081 (Spring Boot + Spring Security + Thymeleaf)
                  │                 │  JWT (Authorization: Bearer)
                  │                 ▼
                  └──────────► backend :8080 (API REST Spring Boot + JWT) ──► MySQL 8 (solo red interna)
```

| Carpeta | Contenido |
|---|---|
| `frontend/` | Código fuente de la capa frontend (páginas Thymeleaf, Spring Security, cliente de la API) |
| `backend/`  | Código fuente de la capa backend (API REST, login con JWT, JPA) |
| `db/script_eventos_gamer.sql` | Script de creación e inserción de datos |
| `nginx/` | Proxy inverso (publica la app en `/eventos`) |
| `zap/` | Plan de ZAP, definición OpenAPI y resultados del análisis |
| `docker-compose.yml` | Despliegue completo con Docker |

## Funcionalidades

| Tipo | Ruta | Descripción |
|---|---|---|
| Pública | `/eventos/` | Eventos más recientes y populares, banners comerciales y acceso al login |
| Pública | `/eventos/buscar` | Búsqueda por nombre, videojuego, categoría, país, ciudad, fecha y modalidad |
| Privada | `/eventos/evento/{id}` | Detalle completo (descripción, ubicación o enlace, organizador, requisitos, inscripción, fotos) |

| Privada | `/eventos/evento/{id}` (sección Fotos y videos) | Agregar fotos (JPG/PNG, 5 MB) y videos (MP4/WebM, 20 MB) al evento |
| Privada | `/eventos/evento/{id}` (sección Comentarios) | Comentar y valorar (1 a 5 estrellas); el autor puede eliminar su comentario |
| Privada | `/eventos/evento/{id}` (panel Compartir) | Compartir en el muro del sitio o en Facebook, X y WhatsApp |
| Privada | `/eventos/compartidos` | Muro de la comunidad con los eventos compartidos en el sitio |

API del backend: `POST /login` (público), `GET /api/publico/**` (público) y, con JWT (`Authorization: Bearer <token>`):

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/eventos/{id}` | Detalle del evento |
| GET / POST | `/api/eventos/{id}/comentarios` | Listar (con promedio) / crear comentario `{"valoracion":5,"texto":"..."}` |
| DELETE | `/api/eventos/{id}/comentarios/{cid}` | Eliminar comentario (solo el autor, si no 403) |
| GET / POST | `/api/eventos/{id}/medios` | Listar / subir foto o video (multipart: `archivo`, `descripcion`) |
| GET | `/api/eventos/medios/{medioId}/contenido` | Contenido del archivo |
| GET / POST | `/api/eventos/{id}/compartidos` | Resumen por destino / compartir `{"destino":"SITIO\|FACEBOOK\|X\|WHATSAPP","mensaje":"..."}` |
| GET | `/api/eventos/compartidos` | Muro de la comunidad |

> Si la base de datos ya existía antes de la semana 5, recrearla para que se carguen las tablas y permisos nuevos:
> `docker compose down -v && docker compose up -d --build`. En la VM definir `URL_PUBLICA=https://[IP]/eventos` en `.env`.

## Usuarios de prueba

| Usuario | Contraseña |
|---|---|
| jugador1 | Jugador1#2026 |
| jugador2 | Jugador2#2026 |
| organizador | Organiza#2026 |

## Despliegue con Docker

Requisitos: Docker Desktop (o Docker Engine + Compose).

```bash
cp .env.example .env        # y cambiar las claves
docker compose up -d --build
```

Abrir **https://localhost/eventos** (desde otro equipo de la red: `https://[IP-del-equipo]/eventos`).

El sitio se entrega solo por HTTPS (puerto 443) con un certificado autofirmado que el servicio `certificado`
genera la primera vez (el navegador mostrará un aviso; en un servidor real se usa un certificado de una CA).
El puerto 80 solo redirige a HTTPS, nginx envía HSTS y la cookie de sesión es `Secure`.

Comandos útiles:

```bash
docker compose ps            # estado de los contenedores
docker compose logs -f backend
docker compose down          # detener
docker compose down -v       # detener y borrar la base de datos (se recrea con el script)
```

## Análisis con ZAP

Requisitos: OWASP ZAP 2.17 instalado en `C:\Program Files\ZAP\Zed Attack Proxy` y la app levantada.

```powershell
cd zap
.\ejecutar-zap.ps1 -Target http://localhost -Salida resultados\despues
```

El script ejecuta dos planes del Automation Framework:

1. `plan-zap-sin-sesion.yaml`: zona pública y login como visitante anónimo.
2. `plan-zap.yaml`: escaneo completo autenticado. El script inicia sesión como `jugador1`
   (respetando CSRF) y obtiene un JWT; ZAP los agrega con el Replacer. Incluye spider, OpenAPI y escaneo activo
   de la web y de la API.

Para reproducir el escaneo "antes", usar `PERFIL_SEGURIDAD=inseguro` y `NGINX_CONF=nginx-inseguro.conf` en `.env`
y ejecutar `docker compose up -d --force-recreate`. **La entrega final usa `PERFIL_SEGURIDAD=default` y `NGINX_CONF=nginx.conf`.**

Resultados reales en `zap/resultados/antes` y `zap/resultados/despues`; capturas en `docs/evidencias`.

El informe usa el escaneo hecho desde la interfaz de ZAP (proxy en localhost:8090, reglas del Replacer con la
cookie de sesión y el JWT, Escaneo Automatizado con Default Policy y escaneo activo de /api y /login).
Sus reportes están en `zap/resultados/gui` y las capturas en `docs/evidencias/gui_*.png`.
