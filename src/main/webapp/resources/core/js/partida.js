// TABLERO
const casilleros = document.querySelectorAll(".casillero");

// Posición actual del jugador
let posicionJugador = 0;

// FICHA DEL JUGADOR
const ficha = document.createElement("div");

ficha.classList.add("ficha");

ficha.setAttribute("aria-label", "Ficha del jugador 1");

// Colocamos la ficha inicialmente en el casillero 0
casilleros[posicionJugador].appendChild(ficha);

// DADOS

const dado1 = document.getElementById("dado-1");
const dado2 = document.getElementById("dado-2");

const botonTirar = document.getElementById("btn-tirar");

// Une el front con el backend. Conecta con controladorPartida metodo tirar dados.
async function obtenerResultadoDados() {
    const respuesta = await fetch("http://localhost:8080/spring/partida/tirar-dados", {
        method: "POST",
    });

    if (!respuesta.ok) {
        throw new Error("No se pudieron tirar los dados");
    }

    return await respuesta.json();
}

botonTirar.addEventListener("click", async () => {
    const resultado = await obtenerResultadoDados();

    dado1.textContent = resultado.dado1;
    dado2.textContent = resultado.dado2;
});

/*// MOVER JUGADOR
async function moverJugador(cantidadPasos) {
    for (let i = 0; i < cantidadPasos; i++) {
        // Avanzamos un casillero
        posicionJugador++;

        // Si llegamos al final del tablero,
        // volvemos al casillero 0
        if (posicionJugador >= casilleros.length) {
            posicionJugador = 0;
        }

        // Movemos visualmente la ficha
        casilleros[posicionJugador].appendChild(ficha);

        // Pequeña pausa para poder ver el movimiento
        await esperar(200);
    }
    // ESPERAR
// Nos permite hacer una pequeña pausa entre cada movimiento
function esperar(milisegundos) {
    return new Promise((resolve) => {
        setTimeout(resolve, milisegundos);
    });
}
 */

/* FUTURA CONEXIÓN CON BACKEND - Cuando tengamos PartidaController,
la idea será que JavaScript llame a un método/end-point del controlador
que obtenga el resultado real de los dados.*/
/*
 * async function obtenerResultadoDadosDesdeBackend() {
 *     const respuesta = await fetch(
 *         "/api/partidas/{idPartida}/tirar-dados",
 *         {
 *             method: "POST"
 *         }
 *     );
 *     const resultado = await respuesta.json();
 *     return resultado;
 * }
 */
