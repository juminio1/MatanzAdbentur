
import { solicitarDificultad } from "../servicio-ruleta.js";

describe("Servicio REST de ruleta", () => {

    let fetchMock;

    beforeEach(() => {
        fetchMock = jasmine.createSpy("fetch");
    });

    it("deberiaSolicitarLaDificultadAlBackendMediantePost", async () => { //Verifica q se utilice el endpoint correcto y el método post

        fetchMock.and.resolveTo({
            ok: true,
            text: async () => "FACIL"
        });

        await solicitarDificultad("/spring", fetchMock);

        expect(fetchMock).toHaveBeenCalledOnceWith(
            "/spring/ruleta/sortear",
            {
                method: "POST"
            }
        );
    });

    it("deberiaDevolverLaDificultadSorteadaPorElBackend", async () => { //Comproba que el front respete el resultado de Java

        fetchMock.and.resolveTo({
            ok: true,
            text: async () => "MEDIO"
        });

        const dificultad = await solicitarDificultad(
            "/spring",
            fetchMock
        );

        expect(dificultad).toBe("MEDIO");
    });

    it("deberiaInformarUnErrorCuandoElBackendNoRespondeCorrectamente", async () => { //Evita q la ruleta gire con un resultado inválido cuando falla la petición

        fetchMock.and.resolveTo({
            ok: false,
            status: 500
        });

        await expectAsync(
            solicitarDificultad("/spring", fetchMock)
        ).toBeRejectedWithError(
            "No se pudo sortear la dificultad"
        );
    });

});
