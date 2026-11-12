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

export function textoPremio(monto, puedeElegir = false) {
    if (monto == null) return "";
    return "Premio: $" + monto + (puedeElegir ? " o robar una propiedad" : "");
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
    const elegirPremio = dialogo.querySelector("#trivia-elegir-premio");
    const botonRobar = elegirPremio.querySelector('[data-premio="ROBAR"]');
    const robar = dialogo.querySelector("#trivia-robar");
    const robarLista = dialogo.querySelector("#trivia-robar-lista");
    const robarVolver = dialogo.querySelector("#trivia-robar-volver");

    let detenerReloj = () => {};
    let enCurso = false;
    let espectador = null;
    // Propiedad que eligió robar el ganador, para nombrarla cuando el servidor confirma el premio.
    let ultimoRobo = null;
    let respuestasHabilitadas = false;
    let versionModal = 0;

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

    function iniciarReloj(totalSegundos, alTerminar, venceEn) {
        const fin = venceEn ?? Date.now() + totalSegundos * 1000;
        const actualizar = () => {
            const restante = Math.max(0, Math.ceil((fin - Date.now()) / 1000));
            segundos.textContent = restante;
            reloj.style.setProperty("--restante", totalSegundos > 0 ? restante / totalSegundos : 0);
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
        numeroRonda, totalRondas = 5, segundosParaResponder = 20, espectador: comoEspectador = null, enJuego = [], permitirResponder = true,
        venceEn, alResponder,
    } = {}) {
        versionModal++;
        ultimoRobo = null;
        respuestasHabilitadas = permitirResponder && !comoEspectador;
        botonCerrar.hidden = true;
        botonCerrar.onclick = null;
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
                boton.disabled = Boolean(espectador) || !permitirResponder;
                boton.addEventListener("click", () => responder(indice));
                const item = document.createElement("li");
                item.append(boton);
                return item;
            });
            opciones.replaceChildren(...botones);

            function responder(indice) {
                if (indice !== null && !respuestasHabilitadas) return;
                respuestasHabilitadas = false;
                if (venceEn == null) detenerReloj();
                opciones.querySelectorAll(".trivia__opcion").forEach((boton, i) => {
                    boton.disabled = true;
                    boton.classList.toggle("trivia__opcion--elegida", i === indice);
                });
                estado.textContent = indice === null ? "Se terminó el tiempo" : "Esperando a los demás…";
                if (indice !== null && alResponder) alResponder(indice);
                resolve(indice);
            }

            if (espectador) estado.textContent = "Los demás están respondiendo…";
            abrir();
            iniciarReloj(segundosParaResponder, function () {
                if (venceEn == null) {
                    responder(null);
                    return;
                }
                respuestasHabilitadas = false;
                for (const boton of opciones.querySelectorAll(".trivia__opcion")) boton.disabled = true;
                estado.textContent = "Tiempo agotado. Esperando el cierre del servidor...";
            }, venceEn);
        });
    }

    // Actualiza permisos y nombres sin recrear opciones ni reiniciar el reloj.
    function actualizarParticipantes({ enJuego, espectador: comoEspectador, permitirResponder }) {
        jugadores.textContent = textoEnJuego(enJuego);
        espectador = comoEspectador;
        respuestasHabilitadas = permitirResponder && !espectador;
        dialogo.classList.toggle("trivia--espectador", Boolean(espectador));
        aviso.textContent = AVISOS_ESPECTADOR[espectador] ?? "";
        for (const boton of opciones.querySelectorAll(".trivia__opcion")) {
            boton.disabled = !respuestasHabilitadas;
        }
        // Mientras la ronda sigue abierta, el que mira sabe que los demás todavía responden.
        if (espectador && !dialogo.classList.contains("trivia--resuelta") && estado.textContent === "") {
            estado.textContent = "Los demás están respondiendo…";
        }
    }

    function mostrarMensaje(texto) {
        estado.textContent = texto;
    }

    function mostrarCierre(elegida, correcta, eliminados, clasificados) {
        respuestasHabilitadas = false;
        mostrarResultado(elegida, correcta);
        // Despues de recargar no conocemos la opcion elegida: no inferir un error.
        estado.textContent = "La respuesta correcta era la " + LETRAS[correcta];
        estado.className = "trivia__estado";
        aviso.textContent = "Eliminados: " + (eliminados.join(", ") || "ninguno");
        jugadores.textContent = "Clasificados: " + (clasificados.join(", ") || "ninguno");
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
    // Arma la lista de propiedades para robar, agrupadas por dueño.
    function pintarPropiedadesParaRobar(propiedades, alElegir) {
        const porDueno = new Map();
        for (const propiedad of propiedades) {
            if (!porDueno.has(propiedad.propietario)) porDueno.set(propiedad.propietario, []);
            porDueno.get(propiedad.propietario).push(propiedad);
        }
        robarLista.replaceChildren(...[...porDueno].map(([dueno, suyas]) => {
            const grupo = document.createElement("section");
            const titulo = document.createElement("h3");
            titulo.className = "trivia__robar-dueno";
            titulo.textContent = `De ${dueno}`;
            const lista = document.createElement("ul");
            lista.className = "trivia__robar-propiedades";
            lista.append(...suyas.map(propiedad => {
                const boton = document.createElement("button");
                boton.type = "button";
                boton.className = "trivia__robar-opcion";
                boton.textContent = propiedad.nombre;
                if (propiedad.nivelMejora > 0) {
                    const mejoras = document.createElement("span");
                    mejoras.className = "trivia__robar-mejoras";
                    mejoras.textContent = "Tiene mejoras: se pierden al robarla";
                    boton.append(mejoras);
                }
                boton.addEventListener("click", () => alElegir(propiedad));
                const item = document.createElement("li");
                item.append(boton);
                return item;
            }));
            grupo.append(titulo, lista);
            return grupo;
        }));
    }

    // Si ganaste una Complicada, primero elegís el premio: "PLATA" ($500) o "ROBAR" (una propiedad).
    // obtenerPropiedadesParaRobar: async () => propiedades de los demás ({ posicion, nombre, propietario, nivelMejora }).
    // Devuelve una promesa con { premio, posicion } cuando tocan "Volver al tablero"
    // (premio null si no había que elegir; posicion solo si robó).
    function mostrarFinal({
        ganador = null, soyGanador = false, montoPremio, puedeElegirPremio = false,
        premioPendiente = false, cancelada = false, tipoPremio, alElegirPremio = async () => {}, desempate = [],
        obtenerPropiedadesParaRobar = async () => [],
    } = {}) {
        const version = ++versionModal;
        detenerReloj();
        dialogo.classList.remove("trivia--resuelta", "trivia--cargando", "trivia--espectador");
        dialogo.classList.add("trivia--final");
        dialogo.classList.toggle("trivia--sin-ganador", !ganador);
        dialogo.classList.toggle("trivia--ganaste", Boolean(ganador) && soyGanador);

        if (cancelada) {
            finalTitulo.textContent = "Trivia cancelada";
            finalDetalle.textContent = "El torneo terminó sin entregar premios.";
        } else if (!ganador) {
            finalTitulo.textContent = "Nadie ganó";
            finalDetalle.textContent = "El torneo terminó sin ganador";
        } else {
            finalTitulo.textContent = soyGanador ? "¡Ganaste la trivia!" : `Ganó ${ganador}`;
            finalDetalle.textContent = desempate.length > 1
                ? "Llegaron varios al final: gana el que respondió más rápido"
                : "Fue el último en pie";
        }
        premio.textContent = ganador ? textoPremio(montoPremio, puedeElegirPremio) : "";
        // Solo el ganador sabe cuál eligió; los demás ven el tablero actualizado al volver.
        if (tipoPremio === "ROBAR") {
            premio.textContent = ultimoRobo
                ? `Premio: ${ultimoRobo.nombre}, robada a ${ultimoRobo.propietario}`
                : "Premio: robó una propiedad";
        }

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

        const eligePremio = Boolean(ganador) && soyGanador && puedeElegirPremio && premioPendiente && !cancelada;
        elegirPremio.hidden = !eligePremio;
        robar.hidden = true;
        botonRobar.disabled = false;
        botonCerrar.hidden = premioPendiente;
        elegirPremio.querySelector('[data-premio="PLATA"]').textContent = "Cobrar $" + montoPremio;
        if (eligePremio) premio.textContent = "Elegí tu premio";

        abrir();
        (eligePremio ? elegirPremio.querySelector("button") : botonCerrar).focus();

        return new Promise(resolve => {
            let enviando = false;
            async function confirmar(eleccion) {
                if (enviando || !eligePremio || version !== versionModal) return;
                enviando = true;
                elegirPremio.hidden = true;
                robar.hidden = true;
                premio.textContent = "Esperando confirmación del premio...";
                try {
                    await alElegirPremio(eleccion);
                } catch (error) {
                    if (version !== versionModal) return;
                    enviando = false;
                    volverAElegir("No se pudo confirmar el premio. Intentá de nuevo.");
                }
            }

            function volverAElegir(texto = "Elegí tu premio") {
                premio.textContent = texto;
                robar.hidden = true;
                elegirPremio.hidden = false;
                elegirPremio.querySelector("button").focus();
            }

            elegirPremio.querySelector('[data-premio="PLATA"]').onclick = () => {
                confirmar({ premio: "PLATA" });
            };

            botonRobar.onclick = async () => {
                if (enviando || !eligePremio || version !== versionModal) return;
                elegirPremio.hidden = true;
                premio.textContent = "Cargando propiedades…";
                let propiedades;
                try {
                    propiedades = await obtenerPropiedadesParaRobar();
                } catch (error) {
                    console.error("No se pudieron cargar las propiedades para robar:", error);
                    if (version !== versionModal) return;
                    volverAElegir("No se pudieron cargar las propiedades");
                    return;
                }
                if (version !== versionModal) return;
                if (propiedades.length === 0) {
                    botonRobar.disabled = true;
                    volverAElegir("Nadie tiene propiedades para robar");
                    return;
                }
                premio.textContent = "Elegí qué propiedad robar";
                pintarPropiedadesParaRobar(propiedades, propiedad => {
                    ultimoRobo = propiedad;
                    confirmar({ premio: "ROBAR", posicion: propiedad.posicion });
                });
                robar.hidden = false;
                robarLista.querySelector("button")?.focus();
            };

            robarVolver.onclick = () => volverAElegir();

            botonCerrar.onclick = () => {
                if (premioPendiente || version !== versionModal) return;
                cerrar();
                resolve({ premio: null });
            };
        });
    }

    function cerrar() {
        versionModal++;
        detenerReloj();
        enCurso = false;
        if (dialogo.open) dialogo.close();
    }

    return { mostrarPregunta, mostrarResultado, mostrarFinal, cerrar, actualizarParticipantes, mostrarCierre, mostrarMensaje };
}
