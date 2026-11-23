package cl.sumativa.eventosapi.security;

import cl.sumativa.eventosapi.domain.TipoMedio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Validación de archivos subidos (CWE-434, subida de archivos sin restricción):
 *  - Se ignoran el nombre y el Content-Type que envía el cliente (se pueden falsificar).
 *  - El tipo se detecta por la firma real del archivo ("magic bytes"): solo JPG, PNG, MP4 y WebM.
 *  - Límite de tamaño por tipo y de dimensiones de imagen (evita "bombas" de descompresión).
 *  - Las imágenes se vuelven a codificar: se eliminan metadatos EXIF (por ejemplo GPS) y cualquier
 *    contenido oculto (archivos políglotas imagen + HTML/JS).
 *  - Los archivos se guardan en la base de datos, nunca en una ruta derivada del nombre del cliente
 *    (sin path traversal ni ejecución en el servidor).
 */
@Component
public class ValidadorArchivos {

    public static final int MAX_BYTES_FOTO = 5 * 1024 * 1024;
    public static final int MAX_BYTES_VIDEO = 20 * 1024 * 1024;
    private static final int MAX_PIXELES_LADO = 6000;

    public record ArchivoValidado(TipoMedio tipo, String contentType, byte[] datos) { }

    public ArchivoValidado validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw rechazo("Debe adjuntar un archivo");
        }
        if (archivo.getSize() > MAX_BYTES_VIDEO) {
            throw demasiadoGrande();
        }
        byte[] datos;
        try {
            datos = archivo.getBytes();
        } catch (IOException e) {
            throw rechazo("No se pudo leer el archivo");
        }

        if (esJpeg(datos)) {
            return foto(datos, "jpg", "image/jpeg");
        }
        if (esPng(datos)) {
            return foto(datos, "png", "image/png");
        }
        if (esMp4(datos)) {
            return video(datos, "video/mp4");
        }
        if (esWebm(datos)) {
            return video(datos, "video/webm");
        }
        throw rechazo("Formato no permitido. Solo se aceptan JPG, PNG, MP4 o WebM");
    }

    private ArchivoValidado foto(byte[] datos, String formato, String contentType) {
        if (datos.length > MAX_BYTES_FOTO) {
            throw demasiadoGrande();
        }
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(datos))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                throw rechazo("La imagen no es válida");
            }
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                // Se leen las dimensiones del encabezado ANTES de decodificar la imagen completa.
                int ancho = lector.getWidth(0);
                int alto = lector.getHeight(0);
                if (ancho <= 0 || alto <= 0 || ancho > MAX_PIXELES_LADO || alto > MAX_PIXELES_LADO) {
                    throw rechazo("La imagen supera las dimensiones permitidas (máximo 6000 x 6000 px)");
                }
                BufferedImage imagen = lector.read(0);
                ByteArrayOutputStream salida = new ByteArrayOutputStream();
                if (!ImageIO.write(imagen, formato, salida)) {
                    throw rechazo("La imagen no es válida");
                }
                return new ArchivoValidado(TipoMedio.FOTO, contentType, salida.toByteArray());
            } finally {
                lector.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw rechazo("La imagen está dañada o no es válida");
        }
    }

    private ArchivoValidado video(byte[] datos, String contentType) {
        if (datos.length > MAX_BYTES_VIDEO) {
            throw demasiadoGrande();
        }
        return new ArchivoValidado(TipoMedio.VIDEO, contentType, datos);
    }

    private static boolean esJpeg(byte[] d) {
        return d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF;
    }

    private static boolean esPng(byte[] d) {
        byte[] firma = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        return empiezaCon(d, firma, 0);
    }

    /** MP4: la caja "ftyp" va en el byte 4. */
    private static boolean esMp4(byte[] d) {
        return d.length > 12 && empiezaCon(d, "ftyp".getBytes(StandardCharsets.US_ASCII), 4);
    }

    /** WebM: encabezado EBML (1A 45 DF A3) y DocType "webm" al inicio. */
    private static boolean esWebm(byte[] d) {
        byte[] ebml = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3};
        if (!empiezaCon(d, ebml, 0)) {
            return false;
        }
        String cabecera = new String(d, 0, Math.min(d.length, 64), StandardCharsets.ISO_8859_1);
        return cabecera.contains("webm");
    }

    private static boolean empiezaCon(byte[] d, byte[] firma, int desde) {
        if (d.length < desde + firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (d[desde + i] != firma[i]) {
                return false;
            }
        }
        return true;
    }

    private static ResponseStatusException rechazo(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }

    private static ResponseStatusException demasiadoGrande() {
        return new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo supera el tamaño permitido (fotos 5 MB, videos 20 MB)");
    }
}
