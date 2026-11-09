// Datos de prueba de la trivia mientras el back no tiene sus endpoints.
//
// Contrato que espera el front (para acordar con el back):
//   pedirPregunta()            -> { id, enunciado, opciones: [4 textos], dificultad: "FACIL" | "MEDIO" | "DIFICIL" }
//   enviarRespuesta(id, opcion) -> { correcta: boolean, opcionCorrecta: 0..3 }
// La respuesta correcta nunca viaja con la pregunta: la comprueba el servidor.
// Cuando el back esté listo, solo hay que reemplazar esta API por llamadas fetch.

const PREGUNTAS_PRUEBA = [
    {
        id: "prueba-1",
        dificultad: "FACIL",
        enunciado: "¿En qué año se declaró la independencia argentina?",
        opciones: ["1810", "1816", "1853", "1880"],
        respuestaCorrecta: 1,
    },
    {
        id: "prueba-2",
        dificultad: "FACIL",
        enunciado: "¿Cuál es la ciudad cabecera del partido de La Matanza?",
        opciones: ["Ramos Mejía", "González Catán", "San Justo", "Laferrere"],
        respuestaCorrecta: 2,
    },
    {
        id: "prueba-3",
        dificultad: "MEDIO",
        enunciado: "¿Qué río separa a Buenos Aires de Uruguay?",
        opciones: ["Río Paraná", "Río de la Plata", "Río Salado", "Río Matanza"],
        respuestaCorrecta: 1,
    },
    {
        id: "prueba-4",
        dificultad: "MEDIO",
        enunciado: "¿Quién compuso la música del Himno Nacional Argentino?",
        opciones: ["Vicente López y Planes", "Juan Pedro Esnaola", "Blas Parera", "Carlos Gardel"],
        respuestaCorrecta: 2,
    },
    {
        id: "prueba-5",
        dificultad: "DIFICIL",
        enunciado: "¿En qué año Argentina ganó su primer Mundial de fútbol?",
        opciones: ["1974", "1978", "1982", "1986"],
        respuestaCorrecta: 1,
    },
];

function esperar(milisegundos) {
    return new Promise(resolve => setTimeout(resolve, milisegundos));
}

export function crearApiTriviaPrueba({ preguntas = PREGUNTAS_PRUEBA, demora = 700, azar = Math.random } = {}) {
    return {
        async pedirPregunta() {
            await esperar(demora);
            const { respuestaCorrecta: _oculta, ...pregunta } =
                preguntas[Math.floor(azar() * preguntas.length)];
            return { ...pregunta, opciones: [...pregunta.opciones] };
        },
        async enviarRespuesta(id, opcion) {
            await esperar(demora);
            const pregunta = preguntas.find(p => p.id === id);
            if (!pregunta) throw new Error("La pregunta no existe");
            return {
                correcta: pregunta.respuestaCorrecta === opcion,
                opcionCorrecta: pregunta.respuestaCorrecta,
            };
        },
    };
}
