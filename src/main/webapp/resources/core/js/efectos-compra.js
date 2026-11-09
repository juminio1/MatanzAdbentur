const aviso = document.getElementById("aviso-compra");
const mensaje = document.getElementById("mensaje-compra");
const icono = document.getElementById("animacion-compra");
let animacion;

// Grabaciones CC0 de Freesound: Gerent (558991) e ilyaShevelev (847351).
const moneda = window.Howl && new window.Howl({
    src: [new URL("../audio/cobro-monedas.mp3", import.meta.url).href],
    volume: 0.35,
});
const descuento = window.Howl && new window.Howl({
    src: [new URL("../audio/pago-monedas.mp3", import.meta.url).href],
    volume: 0.35,
});

const pasoFicha = crearSonido("paso-ficha.wav", 0.22);
const llegadaFicha = crearSonido("llegada-ficha.wav", 0.28);
const sonidoAviso = crearSonido("aviso-partida.wav", 0.24);
const sonidoContinuar = crearSonido("accion-confirmar.wav", 0.24);
const sonidoPasar = crearSonido("accion-pasar.wav", 0.24);
const sonidoCaidaPropiedad = crearSonido("llegada-propiedad.wav", 0.28);
const sonidoIntercambio = crearSonido("intercambio-confirmado.wav", 0.32);
const sonidoDados = crearSonido("dados-globales.wav", 0.30);
const sonidoDadosTablero = crearSonido("dados-tablero.wav", 0.34);

export function reproducirDadosTablero() {
    sonidoDadosTablero?.play();
}
const sonidoComisaria = crearSonido("comisaria.wav", 0.32);
const sonidoChat = crearSonido("chat-entrante.wav?v=2", 0.26);

export function reproducirComisaria() {
    sonidoComisaria?.play();
}

export function reproducirChatEntrante() {
    sonidoChat?.play();
}

export function reproducirDadosGlobales() {
    sonidoDados?.play();
}

export function reproducirIntercambioConfirmado() {
    sonidoIntercambio?.play();
}

export function reproducirAccionPartida(pasar) {
    (pasar ? sonidoPasar : sonidoContinuar)?.play();
}

function crearSonido(archivo, volume) {
    return window.Howl && new window.Howl({
        src: [new URL(`../audio/${archivo}`, import.meta.url).href],
        volume,
    });
}

export function animarPasoFicha(ficha, llegada, esPropiedad) {
    const sonido = llegada ? (esPropiedad ? sonidoCaidaPropiedad : llegadaFicha) : pasoFicha;
    sonido?.play();
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    ficha.animate([
        { scale: 0.82 },
        { scale: llegada ? 1.12 : 1.04 },
        { scale: 1 },
    ], { duration: 180, easing: "ease-out" });
}

export function reproducirCambioDeSaldo(saldoAnterior, saldoNuevo) {
    if (!Number.isFinite(saldoAnterior) || !Number.isFinite(saldoNuevo)) return;
    if (saldoNuevo > saldoAnterior) moneda?.play();
    if (saldoNuevo < saldoAnterior) descuento?.play();
}

export function presentarAvisoPartida(texto, compra, silencioso = false) {
    aviso.showPopover?.();
    aviso.classList.add("aviso-compra--visible");
    aviso.querySelector(".aviso-compra__simbolo").textContent = compra ? "✓" : "ℹ";
    mensaje.textContent = texto;
    if (!compra && !silencioso) sonidoAviso?.play();

    if (compra && !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        animacion = window.lottie?.loadAnimation({
            container: icono,
            renderer: "svg",
            loop: false,
            autoplay: true,
            path: new URL("./compra-confirmada.json", import.meta.url).href,
        });
    }

}

export function ocultarAvisoPartida() {
    aviso.classList.remove("aviso-compra--visible");
    aviso.hidePopover?.();
    mensaje.textContent = "";
    animacion?.destroy();
    animacion = undefined;
}
