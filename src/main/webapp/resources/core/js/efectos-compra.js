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

// Al comprarse una propiedad cae un sello de "VENDIDO" con el color del comprador y el casillero destella.
export function animarCompraCasillero(casillero, color) {
    if (!casillero) return;
    casillero.querySelector(".sello-compra")?.remove();
    const sello = document.createElement("span");
    sello.className = "sello-compra";
    sello.textContent = "Vendido";
    if (color) sello.dataset.color = color;
    sello.setAttribute("aria-hidden", "true");
    casillero.appendChild(sello);
    const colorDestello = getComputedStyle(sello).getPropertyValue("--color-sello").trim() || "#ffda6a";

    const sinMovimiento = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const golpe = sello.animate(sinMovimiento
        ? [{ opacity: 0 }, { opacity: 1, offset: 0.15 }, { opacity: 1, offset: 0.8 }, { opacity: 0 }]
        : [
            // Cae desde arriba grande, golpea, rebota un poquito y se queda; al final se desvanece.
            { scale: 2.6, opacity: 0, easing: "ease-in" },
            { scale: 0.9, opacity: 1, offset: 0.14, easing: "ease-out" },
            { scale: 1.06, offset: 0.2, easing: "ease-in-out" },
            { scale: 1, offset: 0.26 },
            { scale: 1, opacity: 1, offset: 0.82 },
            { scale: 1.08, opacity: 0 },
        ], { duration: 1500 });
    golpe.finished.then(() => sello.remove(), () => sello.remove());

    if (!sinMovimiento) {
        casillero.animate([
            { boxShadow: `inset 0 0 0 0 ${colorDestello}` },
            { boxShadow: `inset 0 0 0 4px ${colorDestello}, 0 0 14px ${colorDestello}`, offset: 0.2 },
            { boxShadow: `inset 0 0 0 0 transparent`, offset: 0.45 },
            { boxShadow: `inset 0 0 0 4px ${colorDestello}, 0 0 14px ${colorDestello}`, offset: 0.65 },
            { boxShadow: "inset 0 0 0 0 transparent" },
        ], { duration: 1100, delay: 180, easing: "ease-out" });
    }
}

// Pago entre jugadores (alquiler o venta de una propiedad): monedas que vuelan en arco de la tarjeta
// del que paga a la del que cobra, con el monto flotando en rojo sobre uno y en verde sobre el otro.
export function animarPagoEntreJugadores(tarjetaPagador, tarjetaPropietario, monto) {
    if (!tarjetaPagador || !tarjetaPropietario) return;
    const texto = `$${Number(monto).toLocaleString("es-AR")}`;
    mostrarMontoFlotante(tarjetaPagador, `−${texto}`, "pierde");
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        mostrarMontoFlotante(tarjetaPropietario, `+${texto}`, "gana");
        return;
    }

    // Las tarjetas están arriba de todo, así que el arco baja (pasa por debajo de ellas) para no salirse de la pantalla.
    const desde = centroDelDinero(tarjetaPagador);
    const hasta = centroDelDinero(tarjetaPropietario);
    volarMonedas(desde, hasta, -Math.max(50, Math.abs(hasta.x - desde.x) * 0.22));
    // El "+$" aparece cuando llega la primera moneda.
    setTimeout(() => mostrarMontoFlotante(tarjetaPropietario, `+${texto}`, "gana"), 780);
}

// Pasar por Inicio: el casillero brilla, salta un confeti chico y las monedas van de Inicio al jugador.
export function animarPasoPorInicio(casilleroInicio, tarjetaJugador, monto) {
    if (!casilleroInicio || !tarjetaJugador) return;
    const texto = `+$${Number(monto).toLocaleString("es-AR")}`;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        mostrarMontoFlotante(tarjetaJugador, texto, "gana");
        return;
    }

    casilleroInicio.animate([
        { boxShadow: "inset 0 0 0 0 #ffda6a" },
        { boxShadow: "inset 0 0 0 4px #ffda6a, 0 0 18px #ffda6a", offset: 0.2 },
        { boxShadow: "inset 0 0 0 0 transparent", offset: 0.45 },
        { boxShadow: "inset 0 0 0 4px #ffda6a, 0 0 18px #ffda6a", offset: 0.65 },
        { boxShadow: "inset 0 0 0 0 transparent" },
    ], { duration: 1100, easing: "ease-out" });

    const caja = casilleroInicio.getBoundingClientRect();
    const origen = { x: caja.left + caja.width / 2, y: caja.top + caja.height / 2 };
    lanzarConfeti(origen);
    // Inicio está abajo y las tarjetas arriba: el arco se abre hacia arriba.
    volarMonedas(origen, centroDelDinero(tarjetaJugador), 80);
    setTimeout(() => mostrarMontoFlotante(tarjetaJugador, texto, "gana"), 780);
}

function centroDelDinero(tarjeta) {
    const caja = (tarjeta.querySelector(".dinero") ?? tarjeta).getBoundingClientRect();
    return { x: caja.left + caja.width / 2, y: caja.top + caja.height / 2 };
}

// Seis monedas que viajan en arco de un punto a otro de la pantalla; "altura" positiva curva hacia arriba.
function volarMonedas(desde, hasta, altura) {
    const dx = hasta.x - desde.x;
    const dy = hasta.y - desde.y;
    const cantidad = 6;
    for (let i = 0; i < cantidad; i++) {
        const moneda = document.createElement("span");
        moneda.className = "moneda-alquiler";
        moneda.setAttribute("aria-hidden", "true");
        moneda.style.left = `${desde.x}px`;
        moneda.style.top = `${desde.y}px`;
        document.body.appendChild(moneda);
        const desvio = (Math.random() - 0.5) * 24;
        const vuelo = moneda.animate([
            { transform: "translate(0, 0) scale(0.6) rotateY(0deg)", opacity: 0 },
            { opacity: 1, offset: 0.1 },
            { transform: `translate(${dx * 0.5 + desvio}px, ${dy * 0.5 - altura}px) scale(1.15) rotateY(540deg)`, offset: 0.5 },
            { transform: `translate(${dx}px, ${dy}px) scale(0.8) rotateY(1080deg)`, opacity: 1, offset: 0.92 },
            { transform: `translate(${dx}px, ${dy}px) scale(0.5) rotateY(1080deg)`, opacity: 0 },
        ], { duration: 850, delay: i * 90, easing: "cubic-bezier(.45,0,.55,1)", fill: "backwards" });
        vuelo.finished.then(() => moneda.remove(), () => moneda.remove());
    }
}

// Confeti chico con los colores de los barrios, que salta desde un punto y cae.
function lanzarConfeti(origen) {
    const colores = ["#91d64b", "#ed5da8", "#42b6e8", "#ffbd42", "#a17be8", "#f16c5e", "#38c8b5", "#f0df4c"];
    for (let i = 0; i < 18; i++) {
        const papel = document.createElement("span");
        papel.className = "confeti";
        papel.setAttribute("aria-hidden", "true");
        papel.style.left = `${origen.x}px`;
        papel.style.top = `${origen.y}px`;
        papel.style.background = colores[i % colores.length];
        document.body.appendChild(papel);
        const angulo = -Math.PI / 2 + (Math.random() - 0.5) * Math.PI * 1.1;
        const fuerza = 45 + Math.random() * 55;
        const x = Math.cos(angulo) * fuerza;
        const y = Math.sin(angulo) * fuerza;
        const giro = (Math.random() - 0.5) * 720;
        const caida = papel.animate([
            { transform: "translate(0, 0) rotate(0deg)", opacity: 1 },
            { transform: `translate(${x}px, ${y}px) rotate(${giro * 0.5}deg)`, opacity: 1, offset: 0.4, easing: "ease-in" },
            { transform: `translate(${x * 1.3}px, ${y + 70}px) rotate(${giro}deg)`, opacity: 0 },
        ], { duration: 1100 + Math.random() * 300, easing: "ease-out" });
        caida.finished.then(() => papel.remove(), () => papel.remove());
    }
}

function mostrarMontoFlotante(tarjeta, texto, tipo) {
    const caja = (tarjeta.querySelector(".dinero") ?? tarjeta).getBoundingClientRect();
    const etiqueta = document.createElement("span");
    etiqueta.className = `monto-flotante monto-flotante--${tipo}`;
    etiqueta.textContent = texto;
    etiqueta.setAttribute("aria-hidden", "true");
    etiqueta.style.left = `${caja.left + caja.width / 2}px`;
    etiqueta.style.top = `${caja.top}px`;
    document.body.appendChild(etiqueta);
    const sube = etiqueta.animate([
        { translate: "-50% 0", opacity: 0 },
        { translate: "-50% -14px", opacity: 1, offset: 0.2 },
        { translate: "-50% -26px", opacity: 1, offset: 0.75 },
        { translate: "-50% -36px", opacity: 0 },
    ], { duration: 1600, easing: "ease-out" });
    sube.finished.then(() => etiqueta.remove(), () => etiqueta.remove());
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
