# Correcciones puntuales en eg2

Se conservaron los cambios anteriores de destinos de Inundación y AFIP. No se realizaron commits, push, merges ni cambios en la referencia de `eg` (`10b7420d214070593628dcaf73552ea8f5fe6177`). La rama de trabajo continúa siendo `eg2`.

## Comportamiento

### Ofertas para pagar deudas

La causa del error original era la validación ordinaria en el servicio de ofertas durante la pausa global, sumada a la falta de una respuesta JSON para esa excepción en `crearOferta`.

- Durante un evento global sólo se permite ofrecer y transferir propiedades si el vendedor tiene una deuda pendiente. Se verifica nuevamente esa condición al responder la oferta.
- Se conservan las comprobaciones de propietario, participantes activos, partida, precio, vencimiento y saldo del comprador.
- La aceptación reutiliza la transferencia existente e intenta pagar la deuda mediante `intentarResolverDeuda`, dentro de la misma transacción. Si el saldo todavía no alcanza, la deuda permanece pendiente.
- Se mantiene la protección contra aceptar dos veces una oferta. Rechazarla no transfiere la propiedad ni modifica saldos.
- Los errores de creación y respuesta se devuelven como JSON. El frontend también presenta un mensaje legible si recibe HTML.
- El popup existente del comprador puede abrirse durante la pausa para las ofertas identificadas como destinadas a pagar una deuda. Se reutilizan las consultas y notificaciones existentes.

### Compra opcional tras Inundación o AFIP

- La caída en una propiedad libre guarda una oportunidad de compra en el resultado persistido del evento.
- La oportunidad se presenta después del cierre global y de la tarjeta de casillero, si la hubo. Usa los estilos y el mecanismo de compra existentes, con una decisión independiente del turno ordinario.
- La confirmación comprueba sesión, participante, ronda del evento, oportunidad pendiente, posición actual, disponibilidad y dinero. Reutiliza `ejecutarCompra` y el precio normal.
- Tanto comprar como rechazar consumen la oportunidad. Repetir la solicitud no vuelve a cobrar.
- El endpoint de esta decisión no avanza turnos ni concede oportunidades de turno adicionales. Las compras ordinarias siguen bloqueadas durante la pausa global.

### Robo de $500

- `data.sql` configura el Robo global en $500 y actualiza su descripción. El upsert existente también contempla catálogos ya inicializados.
- La entidad sigue utilizando su valor configurado; no se agregó otro mecanismo de cobro.
- Se verificó y actualizó el catálogo de desarrollo: Robo, identificador 189, pasó de 1000 a 500 y su descripción indica $500.
- Los importes de eventos de casillero y los demás eventos globales se conservaron.

Se restauró `RONDAS_ENTRE_EVENTOS` de 2 a 5 por indicación expresa de la usuaria. El valor temporal 2 ya estaba presente al iniciar este pedido.

## Archivos de esta tanda

Rutas relativas a la raíz del proyecto; esta lista distingue la tanda actual del trabajo anterior que sigue sin commit.

| Archivo | Cambio |
| --- | --- |
| `src/main/java/com/tallerwebi/dominio/entidades/ConsecuenciaEventoGlobal.java` | Posición persistida de la compra pendiente. |
| `src/main/java/com/tallerwebi/dominio/entidades/EstadoEventoGlobal.java` | Consulta y registro/consumo de esa oportunidad. |
| `src/main/java/com/tallerwebi/dominio/servicios/AplicadorConsecuenciaEventoGlobal.java` | Registro de oportunidad al terminar en una propiedad libre. |
| `src/main/java/com/tallerwebi/dominio/servicios/ServicioEventoImpl.java` | Datos de compra pendiente en la vista; frecuencia restaurada a cinco rondas. |
| `src/main/java/com/tallerwebi/dominio/servicios/ServicioOfertaPropiedadImpl.java` | Excepción limitada a ventas para deuda, aceptación y pago de deuda. |
| `src/main/java/com/tallerwebi/dominio/servicios/ServicioPropiedadImpl.java` | Transferencia autorizada durante deuda y respuesta a la compra de evento. |
| `src/main/java/com/tallerwebi/dominio/servicios/interfaces/ServicioParticipacion.java` | Validaciones compartidas de venta para deuda, compra de evento y participantes de transferencia. |
| `src/main/java/com/tallerwebi/dominio/servicios/interfaces/ServicioPropiedad.java` | Operación `responderCompraDeEvento`. |
| `src/main/java/com/tallerwebi/presentacion/ControladorPropiedadesPartida.java` | Errores JSON, identificación de ofertas para deuda y endpoint de decisión de compra. |
| `src/main/resources/data.sql` | Monto y descripción del Robo global. |
| `src/main/webapp/WEB-INF/views/thymeleaf/partida.html` | Diálogo opcional de compra con las clases visuales existentes. |
| `src/main/webapp/resources/core/js/partida.js` | Ofertas autorizadas durante la pausa, errores legibles y compra opcional posterior. |
| `src/test/java/com/tallerwebi/dominio/servicios/ServicioEventoImplTest.java` | Datos y expectativas del importe solicitado de Robo. |
| `src/test/java/com/tallerwebi/integracion/EventosGlobalesIntegracionTest.java` | Doce casos nuevos de ofertas, deuda, compra y Robo; adaptación del saldo esperado tras pagar el Robo de $500. |
| `src/test/java/com/tallerwebi/postgresql/EventosGlobalesPostgreSQLTest.java` | Catálogo de $500 y prueba adicional de actualización desde $1000. |
| `src/test/java/com/tallerwebi/presentacion/ControladorPropiedadesPartidaOfertasTest.java` | Dos pruebas HTTP que verifican errores JSON durante la pausa. |
| `src/test/js/partida-ofertas-compra-evento.test.cjs` | Seis pruebas del frontend con DOM simulado. |

Este documento registra la entrega. Los archivos anteriores de quiebra y las pruebas anteriores de tarjetas permanecen en el árbol de trabajo.

## Validación real

Se utilizó `-Dmaven.repo.local=C:/Users/Julieta/.m2/repository` por la configuración del entorno. Antes de la ejecución completa se eliminó únicamente el archivo generado de cobertura para obtener un informe nuevo.

| Comprobación | Resultado |
| --- | --- |
| `mvn clean test` | **BUILD SUCCESS**: 701 casos, 0 fallos, 0 errores, 7 omitidos. Se cumplieron los controles configurados de cobertura, Checkstyle, PMD y CPD. |
| `mvn pmd:check` | **BUILD SUCCESS**. |
| `node --test src/test/js/partida-ofertas-compra-evento.test.cjs src/test/js/partida-tarjeta-global.test.cjs` | 12 pruebas aprobadas. |
| Sintaxis del módulo `partida.js` | Correcta. |
| `git diff --check` | Correcto. |

Los casos nuevos comprueban aceptación y rechazo de ofertas para deuda, pago suficiente e insuficiente, comprador sin fondos, bloqueo de ofertas/compras ordinarias durante el evento, compras opcionales por ambos movimientos, rechazo sin descuento, propiedad ocupada posteriormente y Robo de $500 con saldo suficiente e insuficiente. Se conserva la verificación contra repetir cobros y transferencias.

La prueba adicional JDBC/PostgreSQL se intentó explícitamente, pero falló en la preparación por conexión rechazada a `localhost:5432`. Al comprobar el entorno, el contenedor PostgreSQL figuraba detenido. No se informa esa prueba como aprobada; las siete pruebas PostgreSQL quedan omitidas por configuración en la suite completa. La actualización puntual de Robo en desarrollo sí se ejecutó y verificó antes de esa interrupción.

Las pruebas de frontend usan DOM simulado; no se ejecutó una prueba visual de navegador.
