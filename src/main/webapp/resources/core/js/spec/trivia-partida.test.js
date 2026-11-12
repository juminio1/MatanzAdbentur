import { JSDOM } from "jsdom";
import { conectarTriviaPartida, filtrarPropiedadesParaRobar } from "../trivia-partida.js";
import { crearApiTrivia } from "../trivia-api.js";

const PREGUNTA = {
    id: "p1",
    dificultad: "DIFICIL",
    enunciado: "¿En qué año Argentina ganó su primer Mundial de fútbol?",
    opciones: ["1974", "1978", "1982", "1986"],
};

describe("Filtro de propiedades para robar", () => {

    const propiedades = [
        { posicion: 1, nombre: "Feria La Salada", propietarioId: 2 },
        { posicion: 2, nombre: "Mi casa", propietarioId: 1 },
        { posicion: 3, nombre: "Sin dueño", propietarioId: null },
        { posicion: 4, nombre: "Estación Laferrere", propietarioId: 3 },
    ];

    it("deberiaDejarSoloLasPropiedadesDeOtrosJugadores", () => {
        const robables = filtrarPropiedadesParaRobar(propiedades, 1);

        expect(robables.map(p => p.posicion)).toEqual([1, 4]);
    });

    it("deberiaReconocerAlJugadorAunqueElIdLlegueComoTexto", () => {
        const robables = filtrarPropiedadesParaRobar(propiedades, "1");

        expect(robables.map(p => p.posicion)).toEqual([1, 4]);
    });
});

describe("Trivia real", () => {
 let recibir, modal, api, conexion, preso;
 const estado = { finalizada: false, ronda: 1, enJuego: [{id: 1, apodo: "Ana"}] };
 const pregunta = {...PREGUNTA, id: 1, ronda: 1, totalRondas: 5, venceEn: Date.now()+10000};
 beforeEach(() => {
  preso = false;
  const cliente = {subscribe: function(ruta, callback) {
   expect(ruta).toBe("/topic/partida/ABC/trivia");
   recibir = datos => callback({body: JSON.stringify(datos)});
  }};
  modal = jasmine.createSpyObj("modal", ["mostrarPregunta", "cerrar", "actualizarParticipantes", "mostrarCierre", "mostrarMensaje", "mostrarFinal"]);
  api = jasmine.createSpyObj("api", ["obtenerEstado", "obtenerPregunta", "enviarRespuesta", "elegirPremio", "obtenerPropiedades"]);
  api.enviarRespuesta.and.resolveTo({correcta: true});
  api.obtenerEstado.and.resolveTo(null);
  api.obtenerPregunta.and.resolveTo(pregunta);
  conexion = conectarTriviaPartida({cliente, codigo: "ABC", programarCierre: function (cerrar) { cerrar(); }, idJugador: 1, estaPreso: function () { return preso; }, modal, api,
   documento: new JSDOM('<section id="panel-ruleta"></section>').window.document});
 });
 it("muestraLaPreguntaComunDelServidorSinEsperarLaRuleta", () => {
  recibir({tipo: "RONDA_NUEVA", estado, pregunta});
  expect(modal.mostrarPregunta).toHaveBeenCalledWith(pregunta, jasmine.objectContaining({numeroRonda: 1, totalRondas: 5, enJuego: ["Ana"]}));
 });
 it("recuperaLaPreguntaAlRecargar", async () => {
  api.obtenerEstado.and.resolveTo(estado);
  await conexion.recuperar();
  expect(modal.mostrarPregunta).toHaveBeenCalledWith(pregunta, jasmine.any(Object));
 });
 it("noReemplazaEventosNuevosConConsultasViejas", async () => {
  let resolver;
  api.obtenerEstado.and.returnValue(new Promise(resolve => {resolver=resolve;}));
  const pendiente=conexion.recuperar();
  recibir({tipo: "RONDA_NUEVA", estado, pregunta});
  resolver(estado);
  await pendiente;
  expect(modal.mostrarPregunta).toHaveBeenCalledTimes(1);
 });
 it("recuperaDeNuevoSiLaRondaCambioEntreLasDosConsultas", async () => {
  api.obtenerEstado.and.returnValues(Promise.resolve({...estado, ronda: 0}), Promise.resolve(estado));
  await conexion.recuperar();
  expect(modal.mostrarPregunta).toHaveBeenCalledWith(pregunta, jasmine.any(Object));
 });
 it("usaElVencimientoRealSinReiniciarLosVeinteSegundos", () => {
  spyOn(Date, "now").and.returnValue(1000);
  recibir({tipo: "RONDA_NUEVA", estado, pregunta: {...pregunta, venceEn: 4000}});
  expect(modal.mostrarPregunta).toHaveBeenCalledWith(jasmine.any(Object), jasmine.objectContaining({venceEn: 4000}));
 });
 it("noBuscaPreguntaSiNoHayTrivia", async () => {
  await conexion.recuperar();
  expect(api.obtenerPregunta).not.toHaveBeenCalled();
 });
 it("noReiniciaPreguntasDuplicadas", () => {
  recibir({tipo: "RONDA_NUEVA", estado, pregunta});
  recibir({tipo: "RONDA_NUEVA", estado, pregunta});
  expect(modal.mostrarPregunta).toHaveBeenCalledTimes(1);
 });
 function elegir(opcion = 1) {
  return modal.mostrarPregunta.calls.mostRecent().args[1].alResponder(opcion);
 }
 it("enviaUnaSolaRespuestaSinRevelarLaCorrectaAntesDelCierre", async () => {
  recibir({tipo:"RONDA_NUEVA",estado,pregunta});
  await elegir(); await elegir(2);
  expect(api.enviarRespuesta).toHaveBeenCalledOnceWith(1,1);
  expect(modal.mostrarCierre).not.toHaveBeenCalled();
 });
 it("recuperaQueYaRespondioSinPermitirOtroEnvio", async () => {
  api.obtenerEstado.and.resolveTo({...estado,yaRespondio:true});
  await conexion.recuperar();
  await elegir();
  expect(api.enviarRespuesta).not.toHaveBeenCalled();
  expect(modal.actualizarParticipantes).toHaveBeenCalledWith(jasmine.objectContaining({permitirResponder:false}));
 });
 it("actualizaEliminadosSinReiniciarPreguntaNiReloj", async () => {
  recibir({tipo:"RONDA_NUEVA",estado,pregunta});
  recibir({tipo:"PARTICIPANTES_ACTUALIZADOS",estado:{...estado,enJuego:[]}});
  await elegir();
  expect(api.enviarRespuesta).not.toHaveBeenCalled();
  expect(modal.mostrarPregunta).toHaveBeenCalledTimes(1);
  expect(modal.actualizarParticipantes).toHaveBeenCalledWith(jasmine.objectContaining({permitirResponder:false,espectador:"eliminado"}));
 });
 it("muestraElCierreRealYHabilitaLaSiguienteRonda", async () => {
  recibir({tipo:"RONDA_NUEVA",estado,pregunta});
  const eliminados=[{id:2,apodo:"Luna"}];
  recibir({tipo:"RONDA_CERRADA",estado, rondaCerrada:1, opcionCorrecta:2,eliminados});
  await elegir();
  expect(api.enviarRespuesta).not.toHaveBeenCalled();
  expect(modal.mostrarCierre).toHaveBeenCalledWith(null,2,["Luna"],["Ana"]);
  recibir({tipo:"RONDA_NUEVA",estado:{...estado,ronda:2},pregunta:{...pregunta,id:2,ronda:2}});
  await elegir(3);
  expect(api.enviarRespuesta).toHaveBeenCalledOnceWith(2,3);
 });
 it("ignoraUnErrorHttpDeUnaRondaAnterior", async () => {
  let rechazar;
  api.enviarRespuesta.and.returnValue(new Promise((resolve,reject)=>{rechazar=reject;}));
  recibir({tipo:"RONDA_NUEVA",estado,pregunta});
  const envio=elegir();
  recibir({tipo:"RONDA_NUEVA",estado:{...estado,ronda:2},pregunta:{...pregunta,id:2,ronda:2}});
  rechazar(new Error("HTTP 409"));
  await envio;
  expect(modal.mostrarMensaje).not.toHaveBeenCalled();
  expect(modal.mostrarPregunta).toHaveBeenCalledTimes(2);
 });
 it("permiteObservarAlPresoSinEnviarRespuestas", async () => {
  preso=true;
  recibir({tipo:"RONDA_NUEVA",estado,pregunta});
  await elegir();
  expect(api.enviarRespuesta).not.toHaveBeenCalled();
  expect(modal.actualizarParticipantes).toHaveBeenCalledWith(jasmine.objectContaining({espectador:"preso",permitirResponder:false}));
 });
 it("recuperaAlEliminadoComoEspectador", async () => {
  api.obtenerEstado.and.resolveTo({...estado,enJuego:[],yaRespondio:false});
  await conexion.recuperar();
  await elegir();
  expect(api.enviarRespuesta).not.toHaveBeenCalled();
  expect(modal.actualizarParticipantes).toHaveBeenCalledWith(jasmine.objectContaining({espectador:"eliminado",permitirResponder:false}));
 });
 it("consultaSiElServidorAceptoLaRespuestaAunqueFalleLaRed", async () => {
  api.enviarRespuesta.and.rejectWith(new Error("Red interrumpida"));
  api.obtenerEstado.and.resolveTo({...estado,yaRespondio:true});
  recibir({tipo:"RONDA_NUEVA",estado,pregunta});
  await elegir();
  await elegir(3);
  expect(api.enviarRespuesta).toHaveBeenCalledTimes(1);
  expect(modal.mostrarPregunta).toHaveBeenCalledTimes(1);
 });
 it("invalidaUnaConsultaIniciadaAntesDeResponder", async () => {
  recibir({tipo:"RONDA_NUEVA",estado,pregunta});
  let resolver;
  api.obtenerEstado.and.returnValue(new Promise(resolve=>{resolver=resolve;}));
  const consulta=conexion.recuperar();
  await elegir();
  resolver({...estado,yaRespondio:false});
  await consulta;
  await elegir(3);
  expect(api.enviarRespuesta).toHaveBeenCalledTimes(1);
 });
 const finalPendiente = {...estado, finalizada:true, idGanador:1, ganador:"Ana",
  premioPendiente:true, puedeElegirPremio:true, montoPremio:500, cancelada:false, premioEntregado:false};
 it("muestraElPremioRealYEnviaUnaSolaEleccion", async () => {
  api.elegirPremio.and.returnValue(new Promise(()=>{}));
  recibir({tipo:"FINALIZADA",estado:finalPendiente});
  const opciones=modal.mostrarFinal.calls.mostRecent().args[0];
  expect(opciones).toEqual(jasmine.objectContaining({ganador:"Ana",soyGanador:true,montoPremio:500,premioPendiente:true}));
  opciones.alElegirPremio({premio:"PLATA"});
  opciones.alElegirPremio({premio:"ROBAR",posicion:4});
  expect(api.elegirPremio).toHaveBeenCalledOnceWith({premio:"PLATA"});
  expect(modal.cerrar).not.toHaveBeenCalled();
 });
 it("noReabreElModalSiFinalizadaLlegaDespuesDeLaConfirmacion", () => {
  recibir({tipo:"PREMIO_ENTREGADO",estado:{...finalPendiente,premioPendiente:false,premioEntregado:true}});
  recibir({tipo:"FINALIZADA",estado:finalPendiente});
  expect(modal.cerrar).toHaveBeenCalled();
  expect(modal.mostrarFinal).toHaveBeenCalledTimes(1);
 });
 it("recuperaElSelectorYFiltraPropiedadesReales", async () => {
  api.obtenerEstado.and.resolveTo(finalPendiente);
  api.obtenerPropiedades.and.resolveTo([{posicion:1,propietarioId:1},{posicion:2,propietarioId:2},{posicion:3,propietarioId:null}]);
  await conexion.recuperar();
  const opciones=modal.mostrarFinal.calls.mostRecent().args[0];
  expect(await opciones.obtenerPropiedadesParaRobar()).toEqual([{posicion:2,propietarioId:2}]);
 });
 it("muestraCancelacionYSinGanadorSinPermitirPremios", async () => {
  recibir({tipo:"FINALIZADA",estado:{...finalPendiente,cancelada:true,idGanador:null,ganador:null,premioPendiente:false,puedeElegirPremio:false}});
  const opciones=modal.mostrarFinal.calls.mostRecent().args[0];
  expect(opciones.cancelada).toBeTrue();
  await opciones.alElegirPremio({premio:"PLATA"});
  expect(api.elegirPremio).not.toHaveBeenCalled();
 });
 it("losOtrosJugadoresNoPuedenElegir", async () => {
  recibir({tipo:"FINALIZADA",estado:{...finalPendiente,idGanador:2}});
  const opciones=modal.mostrarFinal.calls.mostRecent().args[0];
  expect(opciones.soyGanador).toBeFalse();
  await opciones.alElegirPremio({premio:"PLATA"});
  expect(api.elegirPremio).not.toHaveBeenCalled();
 });
 it("noReabreUnaCancelacionConUnaRespuestaHttpTardia", async () => {
  let resolver;
  api.elegirPremio.and.returnValue(new Promise(resolve=>{resolver=resolve;}));
  recibir({tipo:"FINALIZADA",estado:finalPendiente});
  const envio=modal.mostrarFinal.calls.mostRecent().args[0].alElegirPremio({premio:"PLATA"});
  recibir({tipo:"FINALIZADA",estado:{...finalPendiente,cancelada:true,ganador:null,idGanador:null,premioPendiente:false}});
  resolver({...finalPendiente,premioEntregado:true,premioPendiente:false});
  await envio;
  recibir({tipo:"FINALIZADA",estado:finalPendiente});
  expect(modal.mostrarFinal.calls.mostRecent().args[0].cancelada).toBeTrue();
  expect(modal.mostrarFinal).toHaveBeenCalledTimes(2);
 });
 it("recuperaUnaEntregaConfirmadaSinVolverAMostrarElSelector", async () => {
  api.obtenerEstado.and.resolveTo({...finalPendiente,premioEntregado:true,premioPendiente:false});
  await conexion.recuperar();
  expect(modal.mostrarFinal).not.toHaveBeenCalled();
  expect(modal.cerrar).toHaveBeenCalled();
 });
 it("esperaLaConfirmacionDelPremioAutomaticoSinPermitirElegir", () => {
  recibir({tipo:"FINALIZADA",estado:{...finalPendiente,puedeElegirPremio:false,montoPremio:250}});
  expect(modal.mostrarFinal).toHaveBeenCalledWith(jasmine.objectContaining({montoPremio:250,puedeElegirPremio:false,premioPendiente:true}));
  expect(modal.cerrar).not.toHaveBeenCalled();
  recibir({tipo:"PREMIO_ENTREGADO",estado:{...finalPendiente,puedeElegirPremio:false,montoPremio:250,premioEntregado:true,premioPendiente:false}});
  expect(modal.cerrar).toHaveBeenCalled();
 });
 it("esperaElPremioAntesDeCerrar", () => {
  recibir({tipo: "FINALIZADA", estado: {...estado, finalizada: true, premioPendiente: true}});
  expect(modal.cerrar).not.toHaveBeenCalled();
  recibir({tipo: "PREMIO_ENTREGADO", estado: {...estado, finalizada: true, premioPendiente: false,premioEntregado:true}});
  expect(modal.cerrar).toHaveBeenCalled();
 });
});
describe("API real de trivia", () => {
 it("usaLosEndpointsExistentesDePropiedadesYPremio", async () => {
  const solicitar=jasmine.createSpy("fetch").and.resolveTo({ok:true,json:async()=>[]});
  const api=crearApiTrivia({base:"/spring",codigo:"ABC",solicitar});
  await api.obtenerPropiedades();
  await api.elegirPremio({premio:"ROBAR",posicion:4});
  expect(solicitar.calls.allArgs()).toEqual([
   ["/spring/partida/ABC/propiedades",{cache:"no-store",credentials:"same-origin"}],
   ["/spring/partida/ABC/trivia/premio",{method:"POST",credentials:"same-origin",headers:{"Content-Type":"application/json"},body:'{"premio":"ROBAR","posicion":4}'}]
  ]);
 });
 it("enviaSoloRondaYOpcionAlEndpointReal", async () => {
  const solicitar=jasmine.createSpy("fetch").and.resolveTo({ok:true,json:async()=>({correcta:true})});
  const api=crearApiTrivia({base:"/spring",codigo:"ABC",solicitar});
  await api.enviarRespuesta(2,3);
  expect(solicitar).toHaveBeenCalledOnceWith("/spring/partida/ABC/trivia/respuesta",{
   method:"POST",credentials:"same-origin",headers:{"Content-Type":"application/json"},body:JSON.stringify({id:2,opcion:3})
  });
 });
 it("consultaLasRutasExistentesConSesion", async () => {
  const solicitar=jasmine.createSpy("fetch").and.resolveTo({ok:true,json:async()=>({})});
  const api=crearApiTrivia({base:"/spring",codigo:"ABC",solicitar});
  await api.obtenerEstado(); await api.obtenerPregunta();
  expect(solicitar.calls.allArgs()).toEqual([
   ["/spring/partida/ABC/trivia",{cache:"no-store",credentials:"same-origin"}],
   ["/spring/partida/ABC/trivia/pregunta",{cache:"no-store",credentials:"same-origin"}]
  ]);
 });
 it("acepta404PeroInformaOtrosErrores", async () => {
  const solicitar=jasmine.createSpy("fetch").and.resolveTo({ok:false,status:404});
  const api=crearApiTrivia({codigo:"ABC",solicitar});
  expect(await api.obtenerEstado()).toBeNull();
  solicitar.and.resolveTo({ok:false,status:500});
  await expectAsync(api.obtenerEstado()).toBeRejected();
 });
});
