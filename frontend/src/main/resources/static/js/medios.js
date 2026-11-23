/*
 * Validación en el navegador del formulario de fotos y videos (comodidad para el usuario).
 * La validación real se repite en el servidor: el backend revisa la firma del archivo.
 * Archivo externo (no en línea) porque la CSP solo permite script-src 'self'.
 */
(function () {
    'use strict';
    var form = document.getElementById('form-medio');
    if (!form) {
        return;
    }
    var input = form.querySelector('input[type="file"]');
    var error = document.getElementById('error-archivo');
    var maxFoto = parseInt(form.getAttribute('data-max-foto'), 10);
    var maxVideo = parseInt(form.getAttribute('data-max-video'), 10);
    var limites = {
        'image/jpeg': maxFoto,
        'image/png': maxFoto,
        'video/mp4': maxVideo,
        'video/webm': maxVideo
    };

    function validar() {
        var archivo = input.files && input.files[0];
        if (!archivo) {
            return '';
        }
        var limite = limites[archivo.type];
        if (!limite) {
            return 'Formato no permitido. Solo se aceptan JPG, PNG, MP4 o WebM.';
        }
        if (archivo.size > limite) {
            return archivo.type.indexOf('video/') === 0
                ? 'El video supera el máximo de 20 MB.'
                : 'La imagen supera el máximo de 5 MB.';
        }
        return '';
    }

    function mostrar(mensaje) {
        error.textContent = mensaje;   // textContent: nunca se interpreta como HTML
        error.hidden = !mensaje;
    }

    input.addEventListener('change', function () {
        mostrar(validar());
    });
    form.addEventListener('submit', function (e) {
        var mensaje = validar();
        if (mensaje) {
            e.preventDefault();
            mostrar(mensaje);
        }
    });
})();
