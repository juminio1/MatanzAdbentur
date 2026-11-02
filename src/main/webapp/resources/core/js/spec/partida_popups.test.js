import { readFileSync } from "node:fs";
import { Script, runInContext } from "node:vm";
import { JSDOM } from "jsdom";

const plantilla = readFileSync(new URL("../../../../WEB-INF/views/thymeleaf/partida.html", import.meta.url), "utf8");
const codigoPartida = readFileSync(new URL("../partida.js", import.meta.url), "utf8");

describe("Popups de eventos globales y deudas", function () {
    let pagina;
    let ventana;
    let contexto;

    beforeEach(async function () {
        pagina = new JSDOM(plantilla, {
            url: "http://localhost/spring/partida?codigo=ABC",
            runScripts: "outside-only",
        });
        ventana = pagina.window;
        await new Promise((resolver) => ventana.addEventListener("DOMContentLoaded", resolver, { once: true }));
        ventana.usuarioSesionId = 1;
        ventana.dineroUsuarioSesion = 500;
        ventana.fetch = jasmine.createSpy("fetch");
        const jugadores = ventana.document.querySelector(".jugadores");
        jugadores.innerHTML = '<article class="jugador" id="1"><h3 class="nombre-jugador">messi10L</h3><span class="dinero">$500</span></article>'
            + '<article class="jugador" id="2"><h3 class="nombre-jugador">test123</h3><span class="dinero">$200</span></article>';
        ventana.HTMLDialogElement.prototype.showModal = function () { this.open = true; };
        ventana.HTMLDialogElement.prototype.close = function () {
            this.open = false;
            this.dispatchEvent(new ventana.Event("close"));
        };
        contexto = pagina.getInternalVMContext();
        new Script(codigoPartida).runInContext(contexto);
    });

    afterEach(function () {
        ventana.close();
    });

    function mostrarDeuda(motivo, acreedor = null) {
        ventana.respuestaDeuda = { tieneDeudaPendiente: true, montoPendiente: 1000, motivo, acreedor };
        runInContext("estadoDeudaLocal = { estado: 'actualizado', deuda: validarRespuestaDeuda(respuestaDeuda), error: null }; actualizarAvisoDeuda();", contexto);
    }

    function prepararEstadoGlobal(rondaActivacion, fase = "FINALIZADO", tipo = "ACCIDENTE_SARMIENTO", etapa = 0) {
        return {
            codigoPartida: "ABC", rondaActivacion, fase, tipo, etapa, idJugadorElegido: 2,
            participantes: [1, 2, 3, 4], tiradas: {}, confirmados: [], segundosRestantes: 30,
            posiciones: {}, saldos: {},
        };
    }

    function prepararConsultasGlobales() {
        ventana.eventoConsultado = null;
        for (const id of [1, 2]) {
            const ficha = ventana.document.createElement("div");
            ficha.className = "ficha";
            ficha.dataset.jugadorId = String(id);
            ventana.document.querySelector('[data-casillero="20"] .fichas-en-casillero').appendChild(ficha);
        }
        ventana.fetch.and.callFake(async (url) => ({
            ok: true,
            status: url.endsWith("/evento-global") && ventana.eventoConsultado === null ? 204 : 200,
            json: async () => url.endsWith("/evento-global") ? ventana.eventoConsultado
                : url.endsWith("/evento-global/posiciones") ? [{ idJugador: 1, posicion: 20 }, { idJugador: 2, posicion: 17 }]
                : { tieneDeudaPendiente: false, montoPendiente: null, motivo: null, acreedor: null },
        }));
    }

    it("dadoQueSalePiqueteCuandoSeMuestraElEventoEntoncesSeTitulaNoSePasa", function () {
        expect(runInContext("obtenerNombreEventoGlobal('PIQUETE')", contexto)).toBe("No se pasa");
    });

    it("dadoQueParticipanCuatroCuandoSePresentaElResultadoEntoncesElAvisoPideLaConfirmacionDeTodos", function () {
        ventana.resultadoGlobal = prepararEstadoGlobal(5);

        runInContext("procesarEstadoEventoGlobal(resultadoGlobal)", contexto);

        expect(ventana.document.getElementById("evento-global-mensaje").textContent)
            .toContain("Todos los jugadores activos deben confirmar");
    });

    for (const idJugador of [1, 2, 3, 4]) {
        it(`dadoQueElJugador${idJugador}CerroSarmientoEnRondaCincoCuandoSeRepiteEnDiezEntoncesVeElNuevoPopup`, async function () {
            prepararConsultasGlobales();
            ventana.usuarioSesionId = idJugador;
            runInContext("window.turnoActualId = window.usuarioSesionId; posicionesInicialesSincronizadas = true; estadoEventoGlobalSincronizado = true; estadoDeudaLocal = { estado: 'actualizado', deuda: null, error: null }; actualizarBotonTirada();", contexto);
            expect(ventana.document.getElementById("btn-tirar").disabled).toBeFalse();
            ventana.primerEvento = prepararEstadoGlobal(5);
            ventana.segundoEvento = prepararEstadoGlobal(10);
            runInContext("procesarEstadoEventoGlobal(primerEvento)", contexto);
            await runInContext("consultarEstadoEventoGlobal()", contexto);
            await runInContext("consultaDeudaEnCurso", contexto);
            expect(ventana.document.getElementById("btn-tirar").disabled).toBeFalse();

            const aceptado = runInContext("procesarEstadoEventoGlobal(segundoEvento)", contexto);

            expect(aceptado).toBeTrue();
            expect(ventana.document.getElementById("modal-evento-global").open).toBeTrue();
            expect(ventana.document.getElementById("btn-tirar").disabled).toBeTrue();
            expect(ventana.document.getElementById("cronometro-nro").textContent).toBe("PAUSA");
            expect(runInContext("estadoEventoGlobal?.rondaActivacion", contexto)).toBe(10);
        });
    }

    it("dadoQueSeCerroSarmientoEnCincoCuandoRestDevuelveOtroIgualEnDiezEntoncesSeMuestraElPopup", async function () {
        prepararConsultasGlobales();
        ventana.primerEvento = prepararEstadoGlobal(5);
        runInContext("procesarEstadoEventoGlobal(primerEvento)", contexto);
        await runInContext("consultarEstadoEventoGlobal()", contexto);
        ventana.eventoConsultado = prepararEstadoGlobal(10);

        await runInContext("consultarEstadoEventoGlobal()", contexto);

        expect(ventana.document.getElementById("modal-evento-global").open).toBeTrue();
        expect(runInContext("estadoEventoGlobal?.rondaActivacion", contexto)).toBe(10);
    });

    it("dadoQueElPrimerEventoTuvoDesempatesCuandoComienzaElDeRondaDiezEntoncesAceptaSuEtapaCero", function () {
        ventana.primerEvento = prepararEstadoGlobal(5, "FINALIZADO", "TRAMITES_AFIP", 2);
        ventana.segundoEvento = prepararEstadoGlobal(10, "INICIADO", "INUNDACION");
        runInContext("procesarEstadoEventoGlobal(primerEvento)", contexto);

        const aceptado = runInContext("procesarEstadoEventoGlobal(segundoEvento)", contexto);

        expect(aceptado).toBeTrue();
        expect(runInContext("estadoEventoGlobal.rondaActivacion", contexto)).toBe(10);
        expect(ventana.document.getElementById("btn-tirar-evento-global").disabled).toBeFalse();
    });

    it("dadoQueSigueVisibleElResultadoAnteriorCuandoLlegaOtroSarmientoEnDiezEntoncesRenuevaElPopup", function () {
        ventana.primerEvento = prepararEstadoGlobal(5);
        ventana.segundoEvento = prepararEstadoGlobal(10);
        runInContext("procesarEstadoEventoGlobal(primerEvento); procesarEstadoEventoGlobal(segundoEvento)", contexto);

        expect(runInContext("estadoEventoGlobal.rondaActivacion", contexto)).toBe(10);
        expect(ventana.document.querySelectorAll(".historial__lista li").length).toBe(2);
    });

    it("dadoQueComenzoElEventoDeDiezCuandoLlegaUnMensajeViejoDeCincoEntoncesConservaElActual", function () {
        ventana.primerEvento = prepararEstadoGlobal(5, "TIRADA", "INUNDACION", 2);
        ventana.segundoEvento = prepararEstadoGlobal(10, "INICIADO", "INUNDACION");
        runInContext("procesarEstadoEventoGlobal(segundoEvento)", contexto);

        const aceptado = runInContext("procesarEstadoEventoGlobal(primerEvento)", contexto);

        expect(aceptado).toBeFalse();
        expect(runInContext("estadoEventoGlobal.rondaActivacion", contexto)).toBe(10);
    });

    it("dadoQueSeCerroUnResultadoCuandoLlegaSuWebSocketRetrasadoEntoncesNoLoReabre", async function () {
        prepararConsultasGlobales();
        ventana.primerEvento = prepararEstadoGlobal(5);
        runInContext("procesarEstadoEventoGlobal(primerEvento)", contexto);
        await runInContext("consultarEstadoEventoGlobal()", contexto);

        const aceptado = runInContext("procesarEstadoEventoGlobal(primerEvento)", contexto);

        expect(aceptado).toBeFalse();
        expect(ventana.document.getElementById("modal-evento-global").open).toBeFalse();
    });

    it("dadoQueElServidorConservaLosNombresDeLasFasesCuandoSeMuestraElEstadoEntoncesElFrontendReconoceTodasLasFases", function () {
        const fases = {
            INICIADO: "Evento iniciado", TIRADA: "Tiradas en curso", EMPATE: "Empate",
            DESEMPATE: "Desempate", FINALIZADO: "Evento finalizado",
        };
        for (const [fase, etiqueta] of Object.entries(fases)) {
            ventana.resultadoGlobal = {
                fase, tipo: "TRAMITES_AFIP", etapa: 0, participantes: [1, 2],
                tiradas: {}, confirmados: [], segundosRestantes: 8,
            };

            runInContext("mostrarEstadoEventoGlobal(resultadoGlobal)", contexto);

            expect(ventana.document.getElementById("evento-global-fase").textContent).toBe(etiqueta);
            expect(ventana.document.getElementById("modal-evento-global").classList
                .contains("modal-evento-global--finalizado")).toBe(fase === "FINALIZADO");
        }
    });

    it("dadoQueElGanadorYaEstaPrimeroCuandoSeMuestraTramitesEntoncesSeExplicaElMotivoConSuNombre", function () {
        ventana.resultadoGlobal = {
            fase: "FINALIZADO", tipo: "TRAMITES_AFIP", etapa: 0, idJugadorElegido: 2,
            participantes: [1, 2], tiradas: { 1: 3, 2: 5 }, confirmados: [], segundosRestantes: 35,
            detalle: "no avanza porque ya es el jugador más adelantado del tablero.",
        };

        runInContext("mostrarEstadoEventoGlobal(resultadoGlobal)", contexto);

        expect(ventana.document.getElementById("evento-global-resultado").textContent)
            .toBe("test123 no avanza porque ya es el jugador más adelantado del tablero.");
    });

    it("dadoQueElGanadorDebeAvanzarCuandoSeMuestraTramitesEntoncesSeConservaElAnuncioDeMovimiento", function () {
        ventana.resultadoGlobal = {
            fase: "FINALIZADO", tipo: "TRAMITES_AFIP", etapa: 0, idJugadorElegido: 1,
            participantes: [1, 2], tiradas: { 1: 6, 2: 3 }, confirmados: [], segundosRestantes: 35,
            detalle: "se moverá del casillero 5 al 20 cuando se reanude la partida.",
        };

        runInContext("mostrarEstadoEventoGlobal(resultadoGlobal)", contexto);

        const mensaje = ventana.document.getElementById("evento-global-resultado").textContent;
        expect(mensaje).toContain("messi10L se moverá");
        expect(mensaje).toContain("cuando se reanude la partida.");
        expect(mensaje).not.toContain("no avanza");
    });

    it("dadoQueKevinGeneraUnaDeudaCuandoSeAbreElAvisoEntoncesSeMuestranElMotivoElSaldoYElImporteReales", function () {
        mostrarDeuda("EVENTO_GLOBAL_ROBO");

        expect(ventana.document.getElementById("deuda-motivo").textContent).toBe("Robo de dinero");
        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("$500");
        expect(ventana.document.getElementById("deuda-importe").textContent).toBe("$1.000");
        expect(ventana.document.getElementById("deuda-acreedor-contenedor").hidden).toBeTrue();
    });

    it("dadoQueLaDeudaEsUnAlquilerCuandoSeAbreElAvisoEntoncesSeMuestranElOrigenYElAcreedor", function () {
        mostrarDeuda("ALQUILER", { idJugador: 2, apodo: "José" });

        expect(ventana.document.getElementById("deuda-motivo").textContent).toBe("Alquiler de propiedad");
        expect(ventana.document.getElementById("deuda-acreedor").textContent).toBe("José");
        expect(ventana.document.getElementById("deuda-acreedor-contenedor").hidden).toBeFalse();
    });

    for (const motivo of ["Robo de dinero — El Kevin", "Robo de dinero - El Kevin", "El Kevin", "ROBO"]) {
        it(`dadoQueUnaDeudaAntiguaGuarda${motivo}CuandoSeMuestraEntoncesDiceExactamenteRoboDeDinero`, function () {
            mostrarDeuda(motivo);

            expect(ventana.document.getElementById("deuda-motivo").textContent).toBe("Robo de dinero");
            expect(ventana.document.getElementById("modal-deuda-pendiente").textContent).not.toContain("El Kevin");
        });
    }

    it("dadoQueSoloYoConfirmeCuandoElServidorMantieneElResultadoEntoncesElPopupSigueAbiertoYLosDadosNormalesOcultos", async function () {
        prepararConsultasGlobales();
        ventana.eventoConsultado = prepararEstadoGlobal(5);
        ventana.eventoConsultado.confirmados = [1];
        runInContext("procesarEstadoEventoGlobal(eventoConsultado)", contexto);

        await runInContext("continuarPartidaDespuesDeEventoGlobal()", contexto);

        expect(ventana.document.getElementById("modal-evento-global").open).toBeTrue();
        expect(ventana.document.getElementById("btn-tirar").hidden).toBeTrue();
        expect(ventana.document.querySelector(".dados").hidden).toBeTrue();
        expect(ventana.document.getElementById("btn-continuar-partida-global").textContent).toBe("Esperando a los demás...");
        expect(ventana.document.getElementById("cronometro-nro").textContent).toBe("PAUSA");
    });

    it("dadoQueHayUnResultadoCuandoVenceElRelojLocalEntoncesNoCierraSinAutorizacionDelServidor", function () {
        ventana.resultadoGlobal = prepararEstadoGlobal(5);
        runInContext("procesarEstadoEventoGlobal(resultadoGlobal)", contexto);

        runInContext("cerrarResultadoEventoGlobal()", contexto);

        expect(ventana.document.getElementById("modal-evento-global").open).toBeTrue();
        expect(ventana.document.getElementById("btn-tirar").hidden).toBeTrue();
    });

    it("dadoQueSePerdioLaNotificacionCuandoRestAutorizaReanudarEntoncesRecuperaLasPosicionesAntesDeMostrarLosDados", async function () {
        prepararConsultasGlobales();
        ventana.resultadoGlobal = prepararEstadoGlobal(5, "FINALIZADO", "INUNDACION");
        const ficha = ventana.document.querySelector('.ficha[data-jugador-id="2"]');
        runInContext("procesarEstadoEventoGlobal(resultadoGlobal)", contexto);

        await runInContext("consultarEstadoEventoGlobal()", contexto);

        expect(ventana.fetch).toHaveBeenCalledWith("/spring/partida/ABC/evento-global/posiciones", jasmine.any(Object));
        expect(ficha.closest(".casillero").dataset.casillero).toBe("17");
        expect(ventana.document.getElementById("modal-evento-global").open).toBeFalse();
        expect(ventana.document.getElementById("btn-tirar").hidden).toBeFalse();
        expect(ventana.document.querySelector(".dados").hidden).toBeFalse();
    });

    it("dadoQueUnaNotificacionLlegaDuranteUnaConsultaViejaCuandoElServidorReanudaEntoncesConsultaOtraVezYActualizaLaFicha", async function () {
        prepararConsultasGlobales();
        ventana.resultadoGlobal = prepararEstadoGlobal(5, "FINALIZADO", "INUNDACION");
        runInContext("procesarEstadoEventoGlobal(resultadoGlobal)", contexto);
        let responderConsulta;
        const respuestaVieja = new Promise((resolver) => { responderConsulta = resolver; });
        let consultas = 0;
        ventana.fetch.and.callFake(async (url) => {
            if (url.endsWith("/evento-global") && consultas++ === 0) return respuestaVieja;
            return { ok: true, status: url.endsWith("/evento-global") ? 204 : 200,
                json: async () => url.endsWith("/posiciones") ? [{ idJugador: 2, posicion: 17 }]
                    : { tieneDeudaPendiente: false, montoPendiente: null, motivo: null, acreedor: null } };
        });
        const consulta = runInContext("consultarEstadoEventoGlobal()", contexto);
        const notificacion = runInContext("procesarReanudacionEventoGlobal({codigoPartida: 'ABC'})", contexto);

        responderConsulta({ ok: true, status: 200, json: async () => ventana.resultadoGlobal });
        await consulta;
        await notificacion;

        expect(consultas).toBe(2);
        expect(ventana.document.getElementById("modal-evento-global").open).toBeFalse();
        expect(ventana.document.querySelector('.ficha[data-jugador-id="2"]').closest(".casillero").dataset.casillero).toBe("17");
        expect(runInContext("procesarEstadoEventoGlobal(resultadoGlobal)", contexto)).toBeFalse();
    });

    it("dadoQueFallaRecuperarLasPosicionesCuandoSeConsultaLaReanudacionEntoncesNoHabilitaLosDadosConUnTableroViejo", async function () {
        prepararConsultasGlobales();
        ventana.resultadoGlobal = prepararEstadoGlobal(5);
        runInContext("procesarEstadoEventoGlobal(resultadoGlobal)", contexto);
        ventana.fetch.and.resolveTo({ ok: false, status: 204 });

        await runInContext("consultarEstadoEventoGlobal()", contexto);

        expect(ventana.document.getElementById("modal-evento-global").open).toBeTrue();
        expect(ventana.document.getElementById("btn-tirar").hidden).toBeTrue();
    });

    it("dadoQueLaAnimacionAnteriorSigueEnCursoCuandoSeConfirmaInundacionEntoncesAlTerminarConservaLaPosicionPersistida", async function () {
        prepararConsultasGlobales();
        ventana.animacionDemorada = new Promise((resolver) => { ventana.terminarAnimacion = resolver; });
        runInContext("animarMovimientoFicha = async () => animacionDemorada; atenderCaidaEnCasillero = async () => {};", contexto);
        const movimiento = runInContext("procesarMovimiento({idJugador: 2, posicionAnterior: 15, posicionCaidaEvento: 20, nuevaPosicion: 20, dado1: 2, dado2: 3, saldos: []})", contexto);
        ventana.resultadoGlobal = prepararEstadoGlobal(5, "FINALIZADO", "INUNDACION");
        runInContext("procesarEstadoEventoGlobal(resultadoGlobal)", contexto);
        await runInContext("consultarEstadoEventoGlobal()", contexto);

        ventana.terminarAnimacion();
        await movimiento;

        expect(ventana.document.querySelector('.ficha[data-jugador-id="2"]').closest(".casillero").dataset.casillero).toBe("17");
        expect(runInContext("posicionesGlobalesPendientesDeAnimacion.size", contexto)).toBe(0);
    });

    it("dadoQueElMotivoTieneUnaDescripcionCuandoSeAbreElAvisoEntoncesSeConservaLaDescripcion", function () {
        mostrarDeuda("Multa por control de tránsito");

        expect(ventana.document.getElementById("deuda-motivo").textContent).toBe("Multa por control de tránsito");
    });

    it("dadoQueUnaDeudaAntiguaNoGuardaElEventoCuandoSeAbreElAvisoEntoncesNoSeInventaSuOrigen", function () {
        mostrarDeuda("EVENTO_RESTA");

        expect(ventana.document.getElementById("deuda-motivo").textContent).toBe("Pago por un evento del tablero");
        expect(ventana.document.getElementById("deuda-motivo").textContent).not.toContain("Kevin");
    });

    it("dadoQueElMotivoEsUnCodigoDesconocidoCuandoSeAbreElAvisoEntoncesSeMuestraUnTextoEntendible", function () {
        mostrarDeuda("ORIGEN_DESCONOCIDO");

        expect(ventana.document.getElementById("deuda-motivo").textContent).toBe("Pago pendiente del juego");
    });

    it("dadoQueElSaldoRealEsCeroCuandoSeAbreElAvisoEntoncesSeMuestraCero", function () {
        ventana.dineroUsuarioSesion = 0;

        mostrarDeuda("ALQUILER");

        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("$0");
    });

    it("dadoQueNoSeConoceElSaldoCuandoSeAbreElAvisoEntoncesNoSeInventaUnImporte", function () {
        ventana.dineroUsuarioSesion = undefined;

        mostrarDeuda("ALQUILER");

        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("No disponible");
    });

    it("dadoQueLlegaUnSaldoPorWebSocketCuandoSeActualizaElJugadorEntoncesElAvisoReflejaSoloElSaldoPropio", function () {
        mostrarDeuda("ALQUILER");

        runInContext("actualizarDineroJugador(2, 9000)", contexto);
        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("$500");
        runInContext("actualizarDineroJugador(1, 750)", contexto);

        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("$750");
        expect(ventana.document.getElementById("deuda-importe").textContent).toBe("$1.000");
    });

    it("dadoQueSeVendeUnaPropiedadCuandoElServidorActualizaElDineroEntoncesElAvisoMuestraElNuevoSaldo", async function () {
        mostrarDeuda("EVENTO_GLOBAL_ROBO");
        ventana.confirm = () => true;
        ventana.actualizarEtiquetaPrecioPropiedad = jasmine.createSpy("actualizarEtiquetaPrecioPropiedad");
        ventana.cargarPropiedadesPropias = jasmine.createSpy("cargarPropiedadesPropias").and.resolveTo();
        ventana.fetch.and.callFake(async (url) => ({
            ok: true,
            json: async () => url.endsWith("/vender")
                ? { idJugador: 1, dineroJugador: 750 }
                : ventana.respuestaDeuda,
        }));

        await runInContext("venderPropiedadBono({nombre:'Propiedad de prueba', posicion:5, precioVenta:250})", contexto);

        expect(ventana.fetch).toHaveBeenCalledWith("/spring/partida/ABC/propiedades/5/vender",
            { method: "POST", credentials: "same-origin" });
        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("$750");
        expect(ventana.document.getElementById("deuda-importe").textContent).toBe("$1.000");
        expect(ventana.cargarPropiedadesPropias).toHaveBeenCalled();
    });

    it("dadoQueElJugadorEstaPresoCuandoLlegaSuSaldoEntoncesSeActualizanElImporteYElBotonDeFianza", function () {
        ventana.turnoActualId = 1;
        ventana.document.getElementById("1").dataset.encarcelado = "true";

        runInContext("actualizarDineroJugador(1, 50)", contexto);

        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("$50");
        expect(ventana.document.getElementById("carcel-turno").hidden).toBeFalse();
        expect(ventana.document.getElementById("btn-pagar-fianza").disabled).toBeTrue();

        runInContext("actualizarDineroJugador(2, 9000)", contexto);

        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("$50");
        expect(ventana.document.getElementById("btn-pagar-fianza").disabled).toBeTrue();

        runInContext("actualizarDineroJugador(1, 150)", contexto);

        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("$150");
        expect(ventana.document.getElementById("btn-pagar-fianza").disabled).toBeFalse();
    });

    it("dadoQueHayUnResultadoGlobalCuandoLlegaElEstadoDeCarcelEntoncesSeConservanAmbasPresentaciones", function () {
        const tarjeta = ventana.document.getElementById("1");
        tarjeta.dataset.encarcelado = "true";
        tarjeta.dataset.intentosCarcel = "1";
        const etiqueta = ventana.document.createElement("span");
        etiqueta.className = "estado-carcel";
        tarjeta.appendChild(etiqueta);
        ventana.resultadoGlobal = {
            fase: "FINALIZADO", tipo: "TRAMITES_AFIP", etapa: 0, idJugadorElegido: 2,
            participantes: [1, 2], tiradas: { 1: 3, 2: 5 }, confirmados: [1], segundosRestantes: 35,
            detalle: "se moverá del casillero 5 al 20 cuando se reanude la partida.",
        };
        runInContext("mostrarEstadoEventoGlobal(resultadoGlobal)", contexto);

        runInContext("actualizarEstadoCarcel(1, true, 2, {tipo: 'saldo'})", contexto);

        expect(etiqueta.hidden).toBeFalse();
        expect(etiqueta.textContent).toBe("Preso (2/3)");
        expect(ventana.document.getElementById("modal-evento-global").open).toBeTrue();
        expect(ventana.document.getElementById("evento-global-fase").textContent).toBe("Evento finalizado");
        expect(ventana.document.getElementById("evento-global-resultado").textContent)
            .toBe("test123 se moverá de «UNLaM» a «EVENTOS» cuando se reanude la partida.");
    });

    it("dadoQueHayDeudaCuandoSeUsanLosBotonesEntoncesSeConservanLaConfirmacionYElAccesoAPropiedades", function () {
        mostrarDeuda("ALQUILER");
        ventana.cargarPropiedadesPropias = jasmine.createSpy("cargarPropiedadesPropias").and.resolveTo();

        ventana.document.getElementById("btn-declarar-quiebra").click();

        expect(ventana.document.getElementById("deuda-confirmacion").hidden).toBeFalse();
        expect(ventana.fetch).not.toHaveBeenCalled();
        ventana.document.getElementById("btn-cancelar-quiebra").click();
        expect(ventana.document.getElementById("deuda-confirmacion").hidden).toBeTrue();
        ventana.document.getElementById("btn-vender-desde-deuda").click();
        expect(ventana.document.getElementById("modal-deuda-pendiente").open).toBeFalse();
        expect(ventana.document.getElementById("modal-bono-propiedades").open).toBeTrue();
        expect(ventana.cargarPropiedadesPropias).toHaveBeenCalled();
    });
});
