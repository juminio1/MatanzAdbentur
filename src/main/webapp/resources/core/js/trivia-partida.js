import { crearModalTrivia } from "./trivia.js?v=1";
import { crearApiTriviaPrueba } from "./trivia-api.js?v=1";

// Cuando la ruleta frena, arranca la trivia con la dificultad que salió.
// Provisorio: usa las preguntas de prueba y juega una sola pregunta. Cuando el back tenga
// sus endpoints y avisos por WebSocket, las rondas y el ganador los va a mandar el servidor.

const PAUSA_TRAS_RULETA = 1500;
const PAUSA_TRAS_RESULTADO = 2500;

function esperarMs(milisegundos) {
    return new Promise(resolve => setTimeout(resolve, milisegundos));
}

export function conectarTriviaConRuleta({
    documento = document,
    modal = crearModalTrivia(),
    api = crearApiTriviaPrueba(),
    esperar = esperarMs,
} = {}) {
    let jugando = false;

    async function jugar(dificultad) {
        // Deja ver un momento dónde frenó la ruleta antes de taparla.
        await esperar(PAUSA_TRAS_RULETA);
        const panelRuleta = documento.getElementById("panel-ruleta");
        if (panelRuleta) panelRuleta.hidden = true;

        const pregunta = await api.pedirPregunta(dificultad);
        const elegida = await modal.mostrarPregunta(pregunta);
        const { opcionCorrecta } = await api.enviarRespuesta(pregunta.id, elegida);
        const acerto = modal.mostrarResultado(elegida, opcionCorrecta);

        await esperar(PAUSA_TRAS_RESULTADO);
        await modal.mostrarFinal({ ganador: acerto ? "vos" : null, soyGanador: acerto, dificultad });
    }

    documento.addEventListener("ruleta:frenada", async evento => {
        if (jugando) return;
        jugando = true;
        try {
            await jugar(evento.detail.dificultad);
        } catch (error) {
            console.error("No se pudo jugar la trivia:", error);
            modal.cerrar();
        } finally {
            jugando = false;
        }
    });
}

if (typeof document !== "undefined" && document.getElementById("trivia")) {
    conectarTriviaConRuleta();
}
