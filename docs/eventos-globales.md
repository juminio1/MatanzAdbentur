# Eventos globales

El catálogo se carga al arrancar mediante `DatabaseInitializationConfig` y `data.sql`.
No hay que insertar eventos, jugadores ni partidas manualmente para habilitarlo.

## Modelo y SQL

`Evento` sigue siendo abstracta, con `@Inheritance(JOINED)`, y no cambió sus
atributos. `EventoCasillero` conserva su entidad, repositorio y consultas.
`EventoGlobal extends Evento` declara únicamente `tipoEfecto`, `valor` y
`tipoEventoGlobal`. Los enums se guardan como texto; el tipo global es único.

El nuevo bloque SQL conserva los seis textos originales completos. Busca los
registros por tipo, actualiza la descripción del padre y los efectos del hijo,
conservando sus identificadores y nombres existentes. Si falta el hijo, puede
reutilizar el padre de nombre canónico. Las inserciones comparten el identificador
generado de `Evento`. Ejecutar otra vez el script no duplica el catálogo. El bloque
de eventos de casillero permanece sin cambios. La carga usa UTF-8 explícitamente.

## Implementación e integración

En el main recibido no existían `EventoGlobal`, `TipoEventoGlobal`, su repositorio,
el coordinador de eventos, sus endpoints ni una ventana global. Se incorporaron
esas piezas siguiendo los servicios actuales. Se reutilizan las reglas de
participación, rondas, dados, movimientos, economía, deuda y finalización.

Las clases principales nuevas son `EventoGlobal`, sus dos enums,
`RepositorioEventoGlobal`/`Impl`, `ServicioEvento`/`Impl` y
`ControladorEventoGlobal`. Las tres clases de apoyo son:

- `EstadoEventoGlobal`: estado persistido, etapas, historial de dados,
  habilitados, confirmaciones, seleccionado, revisión y vencimiento.
- `AplicadorConsecuenciaEventoGlobal`: usa los servicios existentes para aplicar
  movimientos, premios, robo y penalización de turno.
- `EstadoRondas`: agrupa las dos propiedades existentes de ronda sin cambiar sus
  columnas, para mantener `Partida` dentro del límite de campos.

Se modificaron `Partida`, `ServicioPartidaImpl` y su interfaz,
`RepositorioPartida`/`Impl`, `ServicioJugadaImpl`, `ServicioParticipacion`,
`ServicioPropiedadImpl`, `ServicioOfertaPropiedadImpl`,
`ServicioGestionQuiebraImpl`, `ServicioFinalizacionPartidaImpl`,
`ObservadorTurnoAbandono`, `ObservadorTurnoQuiebra`,
`GestorTemporizadorPartida`, `ControladorPartida` y `NotificadorPartida` para
coordinar la pausa, las transacciones, la reanudación y los participantes activos.

En presentación se añadieron controles al HTML, lógica a `partida.js`, el módulo
`eventos-globales.js` y cuatro reglas de CSS. La ventana usa los estilos del aviso
de evento existente. El modal de deuda conserva sus acciones, muestra el saldo
disponible e identifica el motivo como «Robo del evento global».

## Turnos, efectos únicos y concurrencia

Al entrar en una ronda múltiplo de cinco se elige uniformemente uno de los seis
eventos persistidos. La última ronda activada queda guardada en la partida.
Los jugadores seleccionables son únicamente los activos, ordenados por turno
antes de indexar la elección aleatoria. El generador se puede sustituir en pruebas.

La partida se bloquea en la base antes de modificarla. Se toma primero el bloqueo
de su fila y luego se recarga el grafo, evitando colecciones leídas antes de una
petición concurrente. Los servicios anidados reutilizan el bloqueo. El resultado
se registra en el componente administrado actual tras las consultas económicas.

La transición a resultado aplica la consecuencia dentro de esa transacción.
Consultar, confirmar o cerrar no vuelve a aplicarla. Las tiradas se identifican
por partida, etapa y jugador. Sólo los empatados entran en la siguiente etapa;
el historial anterior permanece disponible. Los vencimientos completan las
tiradas faltantes con `ServicioDado` y excluyen a los inactivos.

El temporizador normal se cancela después del commit. Sus callbacks comprueban
que el turno siga vigente y que no haya un evento activo. Los temporizadores del
evento usan otra clave y una revisión creciente para impedir que una programación
atrasada reemplace una nueva. Las notificaciones y los temporizadores se publican
después del commit; un rollback no produce esos efectos externos.

Para continuar se esperan todas las confirmaciones activas o el vencimiento de
30 segundos. La existencia de deuda impide reanudar. El robo insuficiente registra
`ROBO_EVENTO_GLOBAL` mediante `DeudaPendiente`, sin saldo negativo ni un sistema
paralelo de deuda. Vender o declarar quiebra reutiliza el flujo existente.

Reanudar no completa otra vez el turno anterior. Reconsidera la oportunidad
pendiente mediante `ServicioTurnoImpl`, que consume una penalización y actualiza
las rondas. Por eso Sarmiento también omite al próximo jugador si resulta elegido.
Se programa un único temporizador normal para el jugador que finalmente puede jugar.

El estado HTTP contiene una revisión y el turno resultante al cerrar. El cliente
descarta estados anteriores, bloquea controles durante la pausa y recupera el
estado al recargar. WebSocket solicita una nueva lectura; una consulta periódica
sirve también para recuperar mensajes perdidos y reprogramar vencimientos tras
una reconexión.

## Verificación

Las pruebas cubren los 18 escenarios base (seis eventos con 2, 3 y 4 jugadores),
Sarmiento en todos los órdenes, desempates consecutivos, participantes inactivos,
movimientos circulares, casos sin desplazamiento, deuda, venta, quiebra,
autenticación, rollback y peticiones concurrentes de tirada y confirmación.

`RepositorioEventoGlobalTest` verifica la persistencia JOINED con Hibernate/HSQLDB.
`EventosGlobalesPostgreSQLTest` ejecuta los scripts reales en esquemas aislados y
comprueba atributos, descripciones completas, IDs, repetición del SQL, actualización
de datos anteriores y conservación de los eventos de casillero.

Para ejecutar las pruebas de PostgreSQL se usa la configuración existente:

```powershell
$env:RUN_POSTGRES_TESTS = 'true'
# DB_HOST, DB_PORT, DB_NAME, DB_USER y DB_PASSWORD deben apuntar a una base de pruebas.
mvn test
```

La prueba de navegador parte de un resultado persistido y controlado de Sarmiento.
Usa dos sesiones de Edge, la aplicación real y PostgreSQL; comprueba recarga,
bloqueo de dados normales, confirmación, sincronización y recuperación del derecho
a jugar. No depende de tiradas aleatorias y no agrega endpoints de prueba.

```powershell
# Iniciar Jetty contra la misma base aislada, en otra terminal:
mvn '-Djetty.http.port=18081' jetty:run
$env:RUN_EVENTOS_E2E = 'true'
$env:EVENTOS_E2E_URL = 'http://127.0.0.1:18081/spring'
mvn '-Dtest=EventosGlobalesE2E' test-compile surefire:test
```

El E2E requiere Edge instalado y un servidor en ejecución. Los seis eventos y las
tres cantidades de jugadores se verifican en las pruebas de servicios; el E2E
cubre el flujo de sincronización y reanudación de Sarmiento con dos clientes.

## Resultados de la entrega

Verificado el 3 de noviembre de 2026:

- `mvn verify` con PostgreSQL 17.11 aislado: **649 pruebas, cero fallos, cero
  errores y cero pruebas omitidas**. Incluye 59 pruebas nuevas de Java respecto
  de la suite inicial.
- PMD, CPD y Checkstyle: verificaciones aprobadas sin cambiar reglas ni límites.
  Checkstyle conserva advertencias del proyecto; no impiden la verificación.
- JaCoCo, con el archivo de ejecución reiniciado: **92,74 % de líneas cubiertas**.
- JavaScript: `npm run lint` aprobado y **11 pruebas Jasmine aprobadas**.
- `EventosGlobalesE2E` con PostgreSQL 17.11, Jetty y dos sesiones de Edge:
  **una prueba aprobada**, sin errores JavaScript de página. Se verificaron también
  la recarga y el rechazo HTTP de una tirada normal durante el evento.
- WAR actualizado: `target/tallerwebi-base-1.0-SNAPSHOT.war`.

Los servidores y bases auxiliares de esta verificación se detuvieron al finalizar.
El contenedor de desarrollo del proyecto y sus datos se conservaron.
