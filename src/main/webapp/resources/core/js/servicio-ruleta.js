
export async function solicitarDificultad(basePath, fetchFuncion = fetch) {

    const respuesta = await fetchFuncion(
        `${basePath}/ruleta/sortear`,
        {
            method: "POST"
        }
    );

    if (!respuesta.ok) {
        throw new Error("No se pudo sortear la dificultad");
    }

    const dificultad = (await respuesta.text()).trim();

    if (!["FACIL", "MEDIO", "DIFICIL"].includes(dificultad)) {
        throw new Error("El backend devolvió una dificultad inválida");
    }

    return dificultad;
}
