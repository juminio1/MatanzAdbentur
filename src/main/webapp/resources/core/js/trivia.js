// Modal de trivia: muestra una pregunta con cuenta regresiva y espera la respuesta.
// No sabe nada del back: recibe la pregunta ya armada y devuelve lo que eligió el jugador.

const LETRAS = ["A", "B", "C", "D"];
const NOMBRES_DIFICULTAD = { FACIL: "Simple", MEDIO: "Media", DIFICIL: "Complicada" };
const SEGUNDOS_URGENTES = 5;

// Los presos y los que ya quedaron afuera ven la trivia, pero no responden.
const AVISOS_ESPECTADOR = {
    preso: "Estás en la cárcel: mirás la trivia sin jugar",
    eliminado: "Quedaste afuera: mirás cómo sigue la trivia",
};
const AVISO_QUEDASTE_AFUERA = "Quedaste afuera de la trivia";
const PREMIOS = { FACIL: "$100", MEDIO: "$250", DIFICIL: "$500 o robar una propiedad" };

export function textoPremio(dificultad) {
    return PREMIOS[dificultad] ? `Premio: ${PREMIOS[dificultad]}` : "";
}

// 12345 ms -> "12,3 s"
export function formatearTiempo(milisegundos) {
    const segundos = (milisegundos / 1000).toLocaleString("es-AR", {
        minimumFractionDigits: 1, maximumFractionDigits: 1,
    });
    return `${segundos} s`;
}

export function nombreDificultad(dificultad) {
    return NOMBRES_DIFICULTAD[dificultad] ?? "";
}

// ["Ana", "Beto", "Caro"] -> "En juego (3): Ana, Beto y Caro"
export function textoEnJuego(nombres = []) {
    if (nombres.length === 0) return "";
    const lista = nombres.length === 1
        ? nombres[0]
        : `${nombres.slice(0, -1).join(", ")} y ${nombres.at(-1)}`;
    return `En juego (${nombres.length}): ${lista}`;
}

export function crearModalTrivia(dialogo = document.getElementById("trivia")) {
    const ronda = dialogo.querySelector("#trivia-ronda");
    const dificultad = dialogo.querySelector("#trivia-dificultad");
    const reloj = dialogo.querySelector(".trivia__reloj");
    const segundos = dialogo.querySelector("#trivia-segundos");
    const aviso = dialogo.querySelector("#trivia-aviso");
    const enunciado = dialogo.querySelector("#trivia-enunciado");
    const opciones = dialogo.querySelector("#trivia-opciones");
    const estado = dialogo.querySelector("#trivia-estado");
    const jugadores = dialogo.querySelector("#trivia-jugadores");
    const finalTitulo = dialogo.querySelector("#trivia-final-titulo");
    const finalDetalle = dialogo.querySelector("#trivia-final-detalle");
    const premio = dialogo.querySelector("#trivia-premio");
    const tiempos = dialogo.querySelector("#trivia-tiempos");
    const botonCerrar = dialogo.querySelector("#trivia-cerrar");

    let detenerReloj = () => {};
    let enCurso = false;
    let espectador = null;

    // La trivia bloquea la pantalla hasta que termina: Escape no la cierra.
    // Chrome a veces ignora el preventDefault del "cancel", así que si igual se cierra, se reabre.
    dialogo.addEventListener("cancel", evento => evento.preventDefault());
    dialogo.addEventListener("close", () => {
        if (enCurso) dialogo.showModal();
    });

    function abrir() {
        enCurso = true;
        if (!dialogo.open) dialogo.showModal();
    }

    function iniciarReloj(totalSegundos, alTerminar) {
        const fin = Date.now() + totalSegundos * 1000;
        const actualizar = () => {
            const restante = Math.max(0, Math.ceil((fin - Date.now()) / 1000));
            segundos.textContent = restante;
            reloj.style.setProperty("--restante", restante / totalSegundos);
            reloj.classList.toggle("trivia__reloj--urgente", restante <= SEGUNDOS_URGENTES);
            if (restante === 0) {
                detenerReloj();
                alTerminar();
            }
        };
        const intervalo = setInterval(actualizar, 250);
        detenerReloj = () => clearInterval(intervalo);
        actualizar();
    }

    // Devuelve una promesa con el índice elegido (0..3), o null si se acabó el tiempo.
    // Con espectador "preso" o "eliminado" las opciones quedan apagadas y siempre devuelve null.
    // enJuego: nombres de los que responden esta ronda.
    function mostrarPregunta(pregunta, {
        numeroRonda, totalRondas = 5, segundosParaResponder = 20, espectador: comoEspectador = null, enJuego = [],
    } = {}) {
        jugadores.textContent = textoEnJuego(enJuego);
        detenerReloj();
        espectador = comoEspectador;
        dialogo.classList.remove("trivia--resuelta", "trivia--cargando", "trivia--final");
        dialogo.classList.toggle("trivia--espectador", Boolean(espectador));
        aviso.textContent = AVISOS_ESPECTADOR[espectador] ?? "";
        ronda.textContent = numeroRonda ? `Ronda ${numeroRonda} de ${totalRondas}` : "";
        dificultad.textContent = nombreDificultad(pregunta.dificultad);
        dificultad.dataset.dificultad = pregunta.dificultad ?? "";
        enunciado.textContent = pregunta.enunciado;
        estado.textContent = "";
        estado.className = "trivia__estado";

        return new Promise(resolve => {
            const botones = pregunta.opciones.map((texto, indice) => {
                const boton = document.createElement("button");
                boton.type = "button";
                boton.className = "trivia__opcion";
                const letra = document.createElement("span");
                letra.className = "trivia__letra";
                letra.textContent = LETRAS[indice];
                boton.append(letra, texto);
                boton.disabled = Boolean(espectador);
                boton.addEventListener("click", () => responder(indice));
                const item = document.createElement("li");
                item.append(boton);
                return item;
            });
            opciones.replaceChildren(...botones);

            function responder(indice) {
                detenerReloj();
                opciones.querySelectorAll(".trivia__opcion").forEach((boton, i) => {
                    boton.disabled = true;
                    boton.classList.toggle("trivia__opcion--elegida", i === indice);
                });
                estado.textContent = indice === null ? "Se terminó el tiempo" : "Esperando a los demás…";
                resolve(indice);
            }

            if (espectador) estado.textContent = "Los demás están respondiendo…";
            abrir();
            iniciarReloj(segundosParaResponder, () => responder(null));
        });
    }

    // Pinta la correcta en verde y, si el jugador le erró, su opción en rojo.
    // La opción correcta la manda el servidor recién cuando se cierra la ronda.
    // Devuelve si acertó; el espectador nunca acierta, solo ve cuál era.
    function mostrarResultado(opcionElegida, opcionCorrecta) {
        detenerReloj();
        dialogo.classList.add("trivia--resuelta");
        opciones.querySelectorAll(".trivia__opcion").forEach((boton, indice) => {
            boton.disabled = true;
            boton.classList.remove("trivia__opcion--elegida");
            boton.classList.toggle("trivia__opcion--correcta", indice === opcionCorrecta);
            boton.classList.toggle("trivia__opcion--incorrecta",
                indice === opcionElegida && indice !== opcionCorrecta);
        });

        if (espectador) {
            estado.className = "trivia__estado";
            estado.textContent = `La respuesta correcta era la ${LETRAS[opcionCorrecta]}`;
            return false;
        }

        const acerto = opcionElegida === opcionCorrecta;
        estado.className = `trivia__estado trivia__estado--${acerto ? "correcta" : "incorrecta"}`;
        if (acerto) estado.textContent = "¡Correcto!";
        else if (opcionElegida === null) estado.textContent = "Se terminó el tiempo";
        else estado.textContent = "Incorrecto";
        if (!acerto) aviso.textContent = AVISO_QUEDASTE_AFUERA;
        return acerto;
    }

    // Pantalla final. ganador: nombre o null si le erraron todos.
    // desempate: [{ nombre, milisegundos }] cuando llegaron varios al final de la última ronda.
    // Devuelve una promesa que se resuelve cuando tocan "Volver al tablero".
    function mostrarFinal({ ganador = null, soyGanador = false, dificultad, desempate = [] } = {}) {
        detenerReloj();
        dialogo.classList.remove("trivia--resuelta", "trivia--cargando", "trivia--espectador");
        dialogo.classList.add("trivia--final");
        dialogo.classList.toggle("trivia--sin-ganador", !ganador);
        dialogo.classList.toggle("trivia--ganaste", Boolean(ganador) && soyGanador);

        if (!ganador) {
            finalTitulo.textContent = "Nadie ganó";
            finalDetalle.textContent = "Todos le erraron a la pregunta";
        } else {
            finalTitulo.textContent = soyGanador ? "¡Ganaste la trivia!" : `Ganó ${ganador}`;
            finalDetalle.textContent = desempate.length > 1
                ? "Llegaron varios al final: gana el que respondió más rápido"
                : "Fue el último en pie";
        }
        premio.textContent = ganador ? textoPremio(dificultad) : "";

        const filas = desempate.length > 1
            ? [...desempate].sort((a, b) => a.milisegundos - b.milisegundos)
            : [];
        tiempos.replaceChildren(...filas.map(({ nombre, milisegundos }) => {
            const item = document.createElement("li");
            const tiempo = document.createElement("span");
            tiempo.className = "trivia__tiempo";
            tiempo.textContent = formatearTiempo(milisegundos);
            item.append(nombre, tiempo);
            return item;
        }));

        abrir();
        botonCerrar.focus();
        return new Promise(resolve => {
            botonCerrar.addEventListener("click", () => {
                cerrar();
                resolve();
            }, { once: true });
        });
    }

    function cerrar() {
        detenerReloj();
        enCurso = false;
        if (dialogo.open) dialogo.close();
    }

    return { mostrarPregunta, mostrarResultado, mostrarFinal, cerrar };
}
