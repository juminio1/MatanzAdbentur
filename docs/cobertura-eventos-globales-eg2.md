# Pruebas de eventos globales en eg2

Validación del 4 de noviembre de 2026 sobre `d9dfc0d4c23bec515fbcd730dc9172d7ecdfd071`.

Se agregaron **54 casos ejecutables**, definidos en 45 métodos nuevos de prueba, incluidos cinco `@TestFactory`. Además, se reforzaron los cuatro casos existentes del temporizador. La suite pasó de 701 a **755 casos**: 748 ejecutados correctamente, cero fallos, cero errores y siete omitidos.

Los cambios comprenden pruebas y este informe. El código productivo, las dependencias y las reglas de calidad no cambiaron. Se conservó la rama `eg2`, sin commits, push, merges ni rebase. La referencia de respaldo `eg` permaneció en `10b7420d214070593628dcaf73552ea8f5fe6177`.

## Clases y cantidad de casos

Los números incluyen las ejecuciones de los tests dinámicos, no solamente sus métodos contenedores.

| Clase | Tipo de cambio | Antes | Después | Agregados |
| --- | --- | ---: | ---: | ---: |
| `ServicioEventoImplTest` | Ampliada | 39 | 66 | 27 |
| `ControladorEventoGlobalTest` | Ampliada | 2 | 9 | 7 |
| `ServicioGestionQuiebraImplTest` | Ampliada | 27 | 33 | 6 |
| `GestorTemporizadorPartidaTest` | Ampliada y reforzada | 4 | 8 | 4 |
| `EventosGlobalesIntegracionTest` | Ampliada | 52 | 53 | 1 |
| `AplicadorConsecuenciaEventoGlobalTest` | Renombrada y ampliada | 4 | 5 | 1 |
| `ServicioEventoCasilleroImplTest` | Nueva | 0 | 6 | 6 |
| `EstadoEventoGlobalTest` | Nueva | 0 | 2 | 2 |
| **Total de estas clases** | | **128** | **182** | **54** |

El archivo `AplicadorConsecuenciaEventoGlobalReglasTest.java`, cuya clase se llamaba `AplicadorConsecuenciaGlobalDestinoTest`, pasó a llamarse `AplicadorConsecuenciaEventoGlobalTest.java`, con su clase correspondiente. Se conservaron sus cuatro pruebas anteriores.

Las clases permanecen en los paquetes existentes. `EstadoEventoGlobalTest` utiliza `com.tallerwebi.dominio.entidades`, que ya contenía pruebas de entidades.

## Escenarios agregados

- **Activación:** rondas 4, 6, 9 y 11 sin evento; activación en 10 y 15; catálogo incompleto; deuda pendiente; cero o un jugador activo. Se conservan los casos de ronda cinco, partida finalizada, evento activo y activación única por ronda.
- **Dados y confirmaciones:** los tres eventos automáticos rechazan tiradas sin consumir dados; solicitudes con ronda o etapa anteriores; tirada fuera de plazo; confirmación antes de la resolución; confirmaciones repetidas o de otra ronda sin adelantar el cierre.
- **Participantes:** selección automática del único empatado que continúa activo; cierre cuando se retiran todos los empatados; cierre desde el callback del temporizador cuando queda un único jugador activo, sin generar tiradas ni consecuencias.
- **Callbacks anteriores:** una etapa, ronda o fecha de vencimiento anterior no altera el evento vigente; un callback de un evento cerrado no repite la recompensa ni reanuda otra vez la partida.
- **Deudas:** un Robo pendiente mantiene la pausa después del plazo sin volver a programar temporizadores; rechazo de otra deuda o alquiler sobre un pago pendiente; rechazo de ventas sin deuda o sin propiedad elegida; evaluación sin deuda; rechazo del código de otra partida sin descontar dinero.
- **Tarjetas:** rechazo del código de otra partida; posición nula sin tarjeta; montos RESTA nulos, cero o negativos sin cobro; pago previo pendiente sin generar otra deuda. La selección se controla usando un catálogo de una sola tarjeta.
- **Estado persistente:** una nueva ronda limpia tarjeta, compra, resultado, tiradas y confirmaciones anteriores; la exclusión de inactivos conserva el historial y modifica la revisión una sola vez.
- **Controlador:** consulta y confirmación válidas con `no-store`; identidad de sesión frente a IDs enviados manualmente; falta de sesión y pertenencia; respuestas HTTP JSON 409 y 403 mediante MockMvc. Los nuevos casos utilizan mocks de las dependencias y verifican las llamadas al servicio.
- **Aislamiento:** dos partidas reales comparten los mismos servicios y repositorios HSQLDB; confirmar y cerrar una conserva el estado de la otra, y resolver la segunda conserva los saldos y posiciones de la primera.
- **Temporizador:** ejecución dentro de una transacción y commit; rollback y conservación de la causa original ante errores; cancelación efectiva; revisiones antiguas o repetidas; independencia por clave; renovación de tareas terminadas. Se eliminaron las esperas `Thread.sleep()` de esta clase.

Se aprovecharon las pruebas unitarias y de integración existentes para AFIP, Inundación, Piquete, Robo, Fiat 600 y Sarmiento: selección y desempates, retroceso circular sin premio, destinos con alquiler/tarjeta/cárcel/compra, deudas y quiebras, ventas al banco y entre jugadores, consecuencias únicas, recompensa de $400, Robo de $500 y consumo de una sola penalización de turno. No se duplicaron esos escenarios exitosos para aumentar porcentajes.

## Ejecución y calidad

Comandos utilizados con el repositorio Maven local disponible en este entorno:

```powershell
mvn '-Dmaven.repo.local=C:/Users/Julieta/.m2/repository' clean test
mvn '-Dmaven.repo.local=C:/Users/Julieta/.m2/repository' pmd:check
```

- `clean test`: **BUILD SUCCESS**, 755 casos, 0 fallos, 0 errores, 7 omitidos. JaCoCo cumplió los umbrales configurados.
- `pmd:check`: **BUILD SUCCESS** con las reglas actuales. El ciclo completo también ejecutó las validaciones configuradas de PMD, CPD y Checkstyle.
- La configuración vigente de PMD analiza producción (`includeTests=false`, valor predeterminado del plugin). El resultado de PMD no representa un análisis estático de todos los tests. Se revisaron los nombres, tipos wrapper, secciones, sincronización y anotaciones de las pruebas incorporadas, sin cambiar esa configuración.
- Checkstyle mantuvo advertencias existentes de documentación; no impidieron la compilación.
- Los siete casos omitidos corresponden a `EventosGlobalesPostgreSQLTest` (6) y `PostgreSQLTest` (1). Esta ejecución no acredita la integración con PostgreSQL.
- Los nuevos métodos cumplen `dadoQue...Cuando...DaComoResultado...` y utilizan las secciones Preparación, Ejecución y Verificación. No se agregaron esperas reales, probabilidades sin control, pruebas deshabilitadas ni supresiones.

## Cobertura medida con JaCoCo

La medición final procede de una ejecución completa de la suite, eliminando antes únicamente el archivo generado `jacoco.exec`, para evitar acumular resultados de ejecuciones anteriores.

| Clase productiva | Líneas antes | Líneas finales | Ramas finales |
| --- | ---: | ---: | ---: |
| `ServicioEventoImpl` | 96,46% | **100% (226/226)** | 96,88% (124/128) |
| `AplicadorConsecuenciaEventoGlobal` | 98,97% | **100% (97/97)** | 92,65% (63/68) |
| `ControladorEventoGlobal` | 69,23% | **100% (13/13)** | Sin ramas contabilizadas |
| `EstadoEventoGlobal` | 100% | **100% (73/73)** | 67,65% (23/34) |
| `ConsecuenciaEventoGlobal` | 100% | **100% (10/10)** | Sin ramas contabilizadas |
| `ServicioEventoCasilleroImpl` | 93,33% | **98,33% (59/60)** | 95% (19/20) |
| `ServicioGestionQuiebraImpl` | 82,31% | **86,92% (113/130)** | 73,75% (59/80) |
| `GestorTemporizadorPartida` | 89,58% | **100% (48/48)** | 95,45% (21/22) |

El conjunto completo del proyecto analizado tiene **93,88% de líneas (3344/3562)** y **80,51% de ramas (1157/1437)**. Tener todas las líneas cubiertas no implica cubrir todas las alternativas de una condición.

Reportes locales generados, ignorados por Git:

- [JaCoCo HTML](../.calidad-de-codigo/jacoco/index.html)
- [JaCoCo XML](../.calidad-de-codigo/jacoco/jacoco.xml)
- [PMD HTML](../.calidad-de-codigo/pmd/pmd.html)
- Resultados JUnit XML en `target/surefire-reports/`.

## Ramas pendientes y hallazgo

- `ServicioEventoImpl`: rechazo de tirada sin evento activo, guarda interna contra resolver dos veces, callback que sigue con resultado resuelto y deuda pendiente, y vista sin un jugador activo coincidente con el índice actual. La guarda privada no se invocó directamente para elevar cobertura.
- `AplicadorConsecuenciaEventoGlobal`: combinaciones defensivas de dependencias opcionales, tarjeta ausente en un casillero de evento, propiedad nula y alquiler omitido por deuda previa.
- `EstadoEventoGlobal`: principalmente estados internos nulos de campos persistentes y una combinación de exclusión que solo cambia confirmaciones. Se priorizaron las transiciones del juego mediante métodos públicos.
- `GestorTemporizadorPartida`: programación de la misma revisión sin una tarea registrada.
- `ServicioGestionQuiebraImpl`: validaciones de datos inválidos, jugador pagador inexistente y parte de `procesarMulta`, fuera del foco de eventos globales. Los escenarios de pago, venta, oferta y quiebra relevantes para eventos conservan sus pruebas existentes.
- `ServicioEventoCasilleroImpl`: continúa sin cubrirse el lanzamiento previsto de `TipoEfectoEventoNoSoportadoException`.

**Hallazgo reproducido:** con una tarjeta cuyo `tipoEfecto` es `null`, `ServicioEventoCasilleroImpl.aplicarSiCorresponde` falla en `aplicadores.get(null)` (línea 92), porque el mapa creado con `Map.copyOf` rechaza esa clave. Lanza `NullPointerException` antes de alcanzar la validación explícita de efecto no soportado.

Una prueba exploratoria que esperaba `TipoEfectoEventoNoSoportadoException` reveló este comportamiento en la primera ejecución focalizada. No se incorporó a la suite final: no se cambió su expectativa para convertir el `NullPointerException` en un contrato del juego, ni se deshabilitó la prueba. Los tipos de efecto vigentes están soportados; el caso requiere una tarjeta con configuración inválida. La corrección productiva quedó pendiente, conforme al alcance solicitado. No se detectaron fallos en los escenarios válidos ejecutados.
