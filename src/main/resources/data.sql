INSERT INTO Usuario( email, username, password, rol, activo)
VALUES( 'test@unlam.edu.ar', 'test123', '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08', 'ADMIN', true)
ON CONFLICT DO NOTHING;

<<<<<<< HEAD
INSERT INTO Evento(descripcion, tipoEfecto, valor, tipoEventoGlobal, DTYPE)
VALUES ('Tramites en AFIP: Caíste a hacer un trámite y hay terrible fila. Todos tiran los dados y el que saque la mayor suma se saltea toda la fila y avanza hasta el casillero del jugador más adelantado. Si hay empate, los empatados vuelven a tirar hasta que uno se gane el pase VIP.', 'MOVER_CASILLEROS', 0, 'TRAMITES_AFIP', 'EventoGlobal'),
('Se inundó el rancho: Se largó el diluvio en La Matanza y el barrio parece una pileta municipal. Todos tiran los dados, el que saque la mayor suma de dados se lo lleva el agua puesto y retrocede 3 casilleros. Si hay empate, los que empataron con la mayor suma vuelven a tirar hasta que uno se coma el garrón. ', 'MOVER_CASILLEROS', -3, 'INUNDACION', 'EventoGlobal'),
('Piquete: Quedaste atrapado en un piquete de volqueteros. Todos tiran los dados, el que saque la menor cantidad de numero en dados, vuelve al inicio. Si hay empate, los que empataron con la menor suma vuelven a tirar hasta que uno se coma el garrón.', 'VOLVER_INICIO', 0, 'PIQUETE', 'EventoGlobal'),
('¡Cuidado, andan robando por la zona! [Nombre del jugador] fue víctima de un robo y perdió $1000.', 'PERDER_PLATA', 1000, 'EL_KEVIN', 'EventoGlobal'),
('Se le quedó el Fiat 600 a Germancho! Se quedó a pata en plena Ruta 3 y [nombre del jugador] se puso la 10 para ayudarlo a empujar la máquina. Germancho le tiró 400 mangos por la changuita.', 'GANAR_PLATA', 400, 'FIAT_600', 'EventoGlobal'),
('Hubo un accidente en el tren Sarmiento y el servicio quedó parado. Justo [nombre del jugador] venía viajando y quedó atrapado en el medio del quilombo. Mala ahí, [nombre del jugador] perdiste el turno esperando que vuelva a funcionar.', 'PERDER_TURNO', 1, 'ACCIDENTE_SARMIENTO', 'EventoGlobal');


/* VALUES(null, null, null, null, null); */
=======
DELETE FROM EventoCasillero
WHERE id IN (
    SELECT id
    FROM Evento
    WHERE nombre IN (
            'Cuota Alimentaria',
            'Debes la cuota alimentaria',
            'Choque en el tren Sarmiento',
            'Changa de fin de mes',
            'Demanda laboral',
            'Demanda laboral (opcional)',
            'Te afanaron'
        )
        OR (
            nombre IS NULL
            AND descripcion IN (
                'Cobrás por una changa en el barrio.',
                'Pagás el colectivo para volver a casa.',
                'Ganás un sorteo en el club de barrio.',
                'Te hacen una multa por estacionar mal.',
                'Vendés algo usado en la feria de San Justo.',
                'Comprás algo para compartir en una juntada.',
                'Te pagan un trabajo extra en la universidad.',
                'Tenés un gasto inesperado en el camino.'
            )
        )
);

DELETE FROM Evento
WHERE nombre IN (
    'Cuota Alimentaria',
    'Debes la cuota alimentaria',
    'Choque en el tren Sarmiento',
    'Changa de fin de mes',
    'Demanda laboral',
    'Demanda laboral (opcional)',
    'Te afanaron'
)
OR (
    nombre IS NULL
    AND descripcion IN (
        'Cobrás por una changa en el barrio.',
        'Pagás el colectivo para volver a casa.',
        'Ganás un sorteo en el club de barrio.',
        'Te hacen una multa por estacionar mal.',
        'Vendés algo usado en la feria de San Justo.',
        'Comprás algo para compartir en una juntada.',
        'Te pagan un trabajo extra en la universidad.',
        'Tenés un gasto inesperado en el camino.'
    )
);

WITH datos(nombre, descripcion, tipo_efecto, monto, cantidad_casilleros, posicion_destino) AS (
    VALUES
        ('Anda con cuidado', 'Te extorsionaron unos barra brava del barrio y les diste $200 para que no te hagan nada.', 'RESTA', 200, NULL, NULL),
        ('Sin entradas para ver a Boquita', 'Llegaste tarde a comprar la popular y te quedaste afuera. Te pasas la tarde escuchando el partido por radio. Pierdes el turno.', 'PIERDE_TURNO', NULL, NULL, NULL),
        ('Choripán traicionero', 'El chori de la previa te cayó mal y necesitas reposo. Pierdes el turno.', 'PIERDE_TURNO', NULL, NULL, NULL),
        ('Fila en la fotocopiadora', 'Fuiste a comprar los apuntes a la UNLAM y la fila daba la vuelta al campus. Pierdes el turno.', 'PIERDE_TURNO', NULL, NULL, NULL),
        ('Servicio reducido', 'Yendo a Once te quedaste varado en la estación de Ramos Mejía por problemas técnicos en las vías. Pierdes un turno.', 'PIERDE_TURNO', NULL, NULL, NULL),
        ('Changa de finde', 'Le hiciste de flete a un vecino con el auto para mover un mueble y te salvó la tarde. Ganaste $500.', 'SUMA', 500, NULL, NULL),
        ('Encontraste guita', 'Ibas caminando por av. Luro y te encontraste $500 en la vereda. Ganaste $500.', 'SUMA', 500, NULL, NULL),
        ('Devolución de AFIP', 'Ocurrió un milagro de fin de año y la AFIP te devolvió las percepciones que te debía. Cobraste $1500.', 'SUMA', 1500, NULL, NULL),
        ('Copia de llaves', 'Te olvidaste las llaves adentro de tu casa y tuviste que ir corriendo a la cerrajería del barrio para hacer una copia de emergencia. Pagás $200.', 'RESTA', 200, NULL, NULL),
        ('Asfalto nuevo', 'La municipalidad por fin terminó de tapar los pozos de tu cuadra. Ahora salís rapidísimo de tu casa sin tener que esquivar cráteres. Avanzas 2 casilleros.', 'AVANZA_CASILLEROS', NULL, 2, NULL),
        ('Asiento libre en el bondi', 'Subiste al colectivo en plena hora pico y mágicamente quedó un asiento vacío al fondo. Viajás como un rey. Avanzas 2 casilleros.', 'AVANZA_CASILLEROS', NULL, 2, NULL),
        ('Cine en el Shopping', 'Ligaste entradas 2x1 para el Cinemark y te vas directo a ver el estreno de la ultima de Spiderman. Avanzas hasta el casillero del Shopping de San Justo.', 'AVANZA_A_POSICION', NULL, NULL, 7),
        ('Antojo de chipá', 'Te pasaron la data de que cerca de la estación están vendiendo los mejores chipás calentitos y fuiste a buscar. Avanzas hasta Estación de Lafe.', 'AVANZA_A_POSICION', NULL, NULL, 18),
        ('Salvando las milanesas', 'Estabas a punto de sentarte en la compu y tu vieja te avisa que se quedó sin pan rallado ni aceite. No te queda otra que salir. Avanzas hasta el Superchino Luna.', 'AVANZA_A_POSICION', NULL, NULL, 22)
), eventos_guardados AS (
    INSERT INTO Evento (nombre, descripcion)
    SELECT datos.nombre, datos.descripcion
    FROM datos
    ON CONFLICT (nombre) DO UPDATE SET descripcion = EXCLUDED.descripcion
    RETURNING id, nombre
)
INSERT INTO EventoCasillero (id, tipoEfecto, monto, cantidadCasilleros, posicionDestino)
SELECT eventos_guardados.id, datos.tipo_efecto, datos.monto, datos.cantidad_casilleros, datos.posicion_destino
FROM eventos_guardados
JOIN datos ON datos.nombre = eventos_guardados.nombre
ON CONFLICT (id) DO UPDATE SET
    tipoEfecto = EXCLUDED.tipoEfecto,
    monto = EXCLUDED.monto,
    cantidadCasilleros = EXCLUDED.cantidadCasilleros,
    posicionDestino = EXCLUDED.posicionDestino;

WITH datos(posicion, nombre, descripcion) AS (
    VALUES
        (3, 'EVENTOS', 'Casillero de eventos'),
        (10, 'EVENTOS', 'Casillero de eventos'),
        (14, 'EVENTOS', 'Casillero de eventos'),
        (20, 'EVENTOS', 'Casillero de eventos'),
        (28, 'EVENTOS', 'Casillero de eventos')
), casilleros_nuevos AS (
    INSERT INTO Casillero (posicion, nombre, descripcion)
    SELECT datos.posicion, datos.nombre, datos.descripcion
    FROM datos
    WHERE NOT EXISTS (
        SELECT 1
        FROM CasilleroEvento casillero_evento
        JOIN Casillero casillero ON casillero.id = casillero_evento.id
        WHERE casillero.posicion = datos.posicion
    )
    RETURNING id
)
INSERT INTO CasilleroEvento (id)
SELECT id
FROM casilleros_nuevos
ON CONFLICT (id) DO NOTHING;
>>>>>>> ceeda4f80ccebd107ddc307b5cf9492cd6fa354a
