
import { solicitarDificultad, confirmarPresentacionRuleta } from "../servicio-ruleta.js";

describe("Servicio REST de ruleta", () => {
    it("confirmaLaPresentacionConLaSesionSinEnviarUnIdDeJugador", async () => {
        const solicitar = jasmine.createSpy("fetch").and.resolveTo({ok:true,status:204});
        await confirmarPresentacionRuleta("/spring","ABC","ACT-1",solicitar);
        expect(solicitar).toHaveBeenCalledOnceWith("/spring/ruleta/ABC/ACT-1/presentacion-finalizada",
            {method:"POST",credentials:"same-origin"});
    });

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
