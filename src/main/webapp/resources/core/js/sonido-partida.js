// Botón para silenciar la partida. Todos los sonidos usan Howler, así que alcanza con su mute global.
// La preferencia queda guardada en el navegador para que dure al recargar o en la próxima partida.
const CLAVE_SILENCIO = "matancero.sonidoSilenciado";

export function leerSilencio(almacen) {
    try {
        return almacen?.getItem(CLAVE_SILENCIO) === "1";
    } catch {
        return false;
    }
}

export function guardarSilencio(almacen, silenciado) {
    try {
        almacen?.setItem(CLAVE_SILENCIO, silenciado ? "1" : "0");
    } catch {
        // Sin almacenamiento (modo privado o bloqueado): el silencio vale solo para esta página.
    }
}

export function aplicarSilencio(silenciado, howler, boton) {
    howler?.mute(silenciado);
    if (!boton) return;
    const texto = silenciado ? "Activar sonido" : "Silenciar sonido";
    boton.setAttribute("aria-pressed", String(silenciado));
    boton.setAttribute("aria-label", texto);
    boton.title = texto;
}

function obtenerAlmacen() {
    try {
        return window.localStorage;
    } catch {
        return null;
    }
}

const boton = typeof document === "undefined" ? null : document.getElementById("btn-sonido");

if (boton) {
    const almacen = obtenerAlmacen();
    let silenciado = leerSilencio(almacen);
    aplicarSilencio(silenciado, window.Howler, boton);

    boton.addEventListener("click", () => {
        silenciado = !silenciado;
        guardarSilencio(almacen, silenciado);
        aplicarSilencio(silenciado, window.Howler, boton);
    });
}
