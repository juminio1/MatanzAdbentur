// Cebar el mate de la mesa: la pava se levanta, se inclina hacia el mate, cae un chorrito y sube vapor.
// Cuando la pava vuelve a su lugar suena un sorbo de mate.
// Es solo un detalle visual: no avisa al servidor y lo ve únicamente quien hace clic.
//
// Sonido del sorbo: "ruido de mate" de ninoing en Freesound (https://freesound.org/s/693366/), licencia CC0.

const DURACION = 2600;

const boton = document.querySelector(".mesa-mate__boton");
const pava = document.querySelector(".mesa-mate__pava");
const chorro = document.querySelector(".mesa-mate__chorro");
const sombra = document.querySelector(".mesa-mate__sombra");
const vapores = [...document.querySelectorAll(".mesa-mate__vapor")];

let cebando = false;
let sorbo;

if (boton && pava && chorro) {
    boton.addEventListener("click", cebarMate);
}

async function cebarMate() {
    if (cebando) return;
    cebando = true;
    const animaciones = [];

    if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        // La pava sube un poco, se inclina 24° hacia el mate, se queda cebando y vuelve a su lugar.
        animaciones.push(pava.animate([
            { transform: "translateY(0) rotate(0deg)" },
            { transform: "translateY(-4%) rotate(6deg)", offset: 0.16, easing: "ease-out" },
            { transform: "translateY(-4.7%) rotate(24deg)", offset: 0.32, easing: "ease-in-out" },
            { transform: "translateY(-4.7%) rotate(22.5deg)", offset: 0.52, easing: "ease-in-out" },
            { transform: "translateY(-4.7%) rotate(24deg)", offset: 0.7, easing: "ease-in-out" },
            { transform: "translateY(-2%) rotate(4deg)", offset: 0.88, easing: "ease-in" },
            { transform: "translateY(0) rotate(0deg)" },
        ], { duration: DURACION }));

        // La sombra sobre la mesa se achica y se aclara mientras la pava está levantada.
        if (sombra) {
            animaciones.push(sombra.animate([
                { scale: "1", opacity: 1 },
                { scale: "0.8", opacity: 0.55, offset: 0.16, easing: "ease-out" },
                { scale: "0.72", opacity: 0.45, offset: 0.32 },
                { scale: "0.72", opacity: 0.45, offset: 0.7 },
                { scale: "0.88", opacity: 0.75, offset: 0.88, easing: "ease-in" },
                { scale: "1", opacity: 1 },
            ], { duration: DURACION }));
        }

        // El chorro aparece cuando la pava ya está inclinada y se corta antes de que vuelva.
        animaciones.push(chorro.animate([
            { scale: "1 0", opacity: 0 },
            { scale: "1 0", opacity: 0, offset: 0.32 },
            { scale: "1 1", opacity: 1, offset: 0.4 },
            { scale: "1 1", opacity: 1, offset: 0.68 },
            { scale: "1 0.2", opacity: 0, offset: 0.76 },
            { scale: "1 0", opacity: 0 },
        ], { duration: DURACION }));
    }

    // El sorbo suena cuando la pava ya volvió, como si alguien tomara el mate recién cebado.
    setTimeout(reproducirSorbo, DURACION * 0.9);

    // Vapor saliendo del mate recién cebado: es el único vapor, el fondo ya no tiene uno fijo.
    // Cada bocanada sube ondulando hacia un costado distinto, se agranda y se desvanece.
    const alto = pava.offsetHeight;
    vapores.forEach((vapor, indice) => {
        const lado = (indice % 2 === 0 ? -1 : 1) * (0.02 + Math.random() * 0.03) * alto;
        const subida = (0.3 + Math.random() * 0.1) * alto;
        animaciones.push(vapor.animate([
            { translate: "0 0", scale: 0.5, opacity: 0 },
            { translate: `${-lado * 0.5}px ${-subida * 0.3}px`, scale: 1.3, opacity: 0.85, offset: 0.25 },
            { translate: `${lado}px ${-subida * 0.65}px`, scale: 2.1, opacity: 0.55, offset: 0.6 },
            { translate: `${lado * 0.4}px ${-subida}px`, scale: 2.8, opacity: 0 },
        ], { duration: 1800, delay: DURACION * 0.42 + indice * 230, easing: "ease-out" }));
    });

    await Promise.allSettled(animaciones.map((animacion) => animacion.finished));
    cebando = false;
}

function reproducirSorbo() {
    if (!window.Howl) return;
    sorbo ??= new window.Howl({
        src: [new URL("../audio/sorbo-mate.mp3", import.meta.url).href],
        volume: 0.6,
    });
    sorbo.play();
}
