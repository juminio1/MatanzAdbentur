INSERT INTO Usuario( email, username, password, rol, activo)
VALUES( 'test@unlam.edu.ar', 'test123', '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08', 'ADMIN', true)
ON CONFLICT DO NOTHING;

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


-- Inicialización de los seis eventos globales.
-- Se respeta la herencia JOINED entre Evento y EventoGlobal.

WITH datos(tipo_evento, nombre, descripcion, tipo_efecto, valor) AS (
    VALUES
        ('TRAMITES_AFIP', 'TRAMITES_AFIP',
         'Caíste a hacer un trámite y hay terrible fila. Todos tiran los dados y el que saque la mayor suma se saltea toda la fila y avanza hasta el casillero del jugador más adelantado. Si hay empate, los empatados vuelven a tirar hasta definir un ganador.',
         'MOVER_CASILLEROS', 0),

        ('INUNDACION', 'INUNDACION',
         'Se largó el diluvio y el barrio parece una pileta. Todos tiran los dados y el que saque la mayor suma se lo lleva el agua puesto y retrocede 3 casilleros. Si hay empate, los empatados vuelven a tirar.',
         'MOVER_CASILLEROS', -3),

        ('PIQUETE', 'PIQUETE',
         'Quedaste atrapado en un piquete de volqueteros. Todos tiran los dados y el que saque la menor suma vuelve al Inicio. Si hay empate, los empatados vuelven a tirar.',
         'VOLVER_INICIO', 0),

        ('ROBO', 'ROBO',
         '¡Cuidado, andan robando por la zona! [nombre del jugador] fue víctima de un robo y perdió $500.',
         'PERDER_PLATA', 500),

        ('FIAT_600', 'FIAT_600',
         'Se le quedó el Fiat 600 a Germancho. [nombre del jugador] se puso la 10 para ayudarlo a empujar la máquina y recibió unos mangos por la changuita.',
         'GANAR_PLATA', 400),

        ('ACCIDENTE_SARMIENTO', 'ACCIDENTE_SARMIENTO',
         'Hubo un accidente en el tren Sarmiento y el servicio quedó parado. Justo [nombre del jugador] venía viajando y perdió el turno esperando que vuelva a funcionar.',
         'PERDER_TURNO', 1)
),

eventos_actualizados AS (
    UPDATE Evento AS evento_base
    SET
        nombre = datos.nombre,
        descripcion = datos.descripcion
    FROM EventoGlobal AS evento_global
    JOIN datos
        ON evento_global.tipoEventoGlobal = datos.tipo_evento
    WHERE evento_base.id = evento_global.id
    RETURNING
        evento_base.id,
        evento_global.tipoEventoGlobal AS tipo_evento
),

eventos_reutilizados AS (
    UPDATE Evento AS evento_base
    SET descripcion = datos.descripcion
    FROM datos
    WHERE evento_base.nombre = datos.nombre
      AND NOT EXISTS (
          SELECT 1
          FROM EventoGlobal AS evento_global
          WHERE evento_global.tipoEventoGlobal = datos.tipo_evento
      )
      AND NOT EXISTS (
          SELECT 1
          FROM EventoGlobal AS evento_global
          WHERE evento_global.id = evento_base.id
      )
      AND NOT EXISTS (
          SELECT 1
          FROM EventoCasillero AS evento_casillero
          WHERE evento_casillero.id = evento_base.id
      )
    RETURNING evento_base.id, evento_base.nombre
),

eventos_insertados AS (
    INSERT INTO Evento (nombre, descripcion)
    SELECT datos.nombre, datos.descripcion
    FROM datos
    WHERE NOT EXISTS (
        SELECT 1
        FROM EventoGlobal AS evento_global
        WHERE evento_global.tipoEventoGlobal = datos.tipo_evento
    )
    AND NOT EXISTS (
        SELECT 1
        FROM Evento AS evento_base
        WHERE evento_base.nombre = datos.nombre
    )
    RETURNING id, nombre
),

identificadores AS (
    SELECT id, tipo_evento
    FROM eventos_actualizados

    UNION ALL

    SELECT evento_base.id, datos.tipo_evento
    FROM eventos_reutilizados AS evento_base
    JOIN datos ON datos.nombre = evento_base.nombre

    UNION ALL

    SELECT evento_base.id, datos.tipo_evento
    FROM eventos_insertados AS evento_base
    JOIN datos ON datos.nombre = evento_base.nombre
)

INSERT INTO EventoGlobal (
    id,
    tipoEfecto,
    valor,
    tipoEventoGlobal
)
SELECT
    identificadores.id,
    datos.tipo_efecto,
    datos.valor,
    datos.tipo_evento
FROM identificadores
JOIN datos
    ON identificadores.tipo_evento = datos.tipo_evento
WHERE TRUE
ON CONFLICT (id) DO UPDATE SET
    tipoEfecto = EXCLUDED.tipoEfecto,
    valor = EXCLUDED.valor,
    tipoEventoGlobal = EXCLUDED.tipoEventoGlobal;

-- Las respuestas correctas se guardan como índices de 0 a 3.
INSERT INTO pregunta_trivia (
    enunciado, opcion1, opcion2, opcion3, opcion4, respuesta_correcta, dificultad
) VALUES
    ('¿Cuál es la capital de Argentina?', 'Córdoba', 'Buenos Aires', 'Mendoza', 'Rosario', 1, 'SIMPLE'),
    ('¿Qué colores predominan en la bandera argentina?', 'Rojo y blanco', 'Verde y amarillo', 'Celeste y blanco', 'Azul y rojo', 2, 'SIMPLE'),
    ('¿Cuál es la moneda de Argentina?', 'Peso argentino', 'Real', 'Guaraní', 'Sol', 0, 'SIMPLE'),
    ('¿Con qué país limita Argentina al oeste a lo largo de los Andes?', 'Brasil', 'Paraguay', 'Uruguay', 'Chile', 3, 'SIMPLE'),
    ('¿En qué deporte ganó Argentina el Mundial de Catar 2022?', 'Rugby', 'Fútbol', 'Básquet', 'Hockey sobre césped', 1, 'SIMPLE'),
    ('¿Qué infusión tradicional se prepara con yerba y se toma con bombilla?', 'Café', 'Té negro', 'Mate', 'Submarino', 2, 'SIMPLE'),
    ('¿En qué ciudad argentina está el Obelisco de la avenida 9 de Julio?', 'Buenos Aires', 'La Plata', 'Santa Fe', 'Salta', 0, 'SIMPLE'),
    ('¿Qué océano baña la costa argentina?', 'Pacífico', 'Índico', 'Ártico', 'Atlántico', 3, 'SIMPLE'),
    ('¿En qué provincia argentina se encuentran las Cataratas del Iguazú?', 'Corrientes', 'Misiones', 'Chaco', 'Formosa', 1, 'SIMPLE'),
    ('¿Qué baile rioplatense está asociado con Carlos Gardel?', 'Chacarera', 'Zamba', 'Tango', 'Carnavalito', 2, 'SIMPLE'),
    ('¿En qué ciudad se declaró la independencia argentina en 1816?', 'Buenos Aires', 'San Miguel de Tucumán', 'Córdoba', 'Salta', 1, 'MEDIA'),
    ('¿Quién escribió El Aleph?', 'Julio Cortázar', 'Ernesto Sabato', 'Jorge Luis Borges', 'Adolfo Bioy Casares', 2, 'MEDIA'),
    ('¿En qué provincia se encuentra el Aconcagua?', 'Mendoza', 'San Juan', 'La Rioja', 'Neuquén', 0, 'MEDIA'),
    ('¿Qué río separa las ciudades de Posadas y Encarnación?', 'Uruguay', 'Paraguay', 'Pilcomayo', 'Paraná', 3, 'MEDIA'),
    ('¿En qué provincia está el glaciar Perito Moreno?', 'Tierra del Fuego', 'Santa Cruz', 'Chubut', 'Río Negro', 1, 'MEDIA'),
    ('¿Quién creó la bandera argentina?', 'José de San Martín', 'Mariano Moreno', 'Manuel Belgrano', 'Martín Miguel de Güemes', 2, 'MEDIA'),
    ('¿Qué ciudad fue fundada en 1882 como capital de la provincia de Buenos Aires?', 'La Plata', 'Bahía Blanca', 'Mar del Plata', 'San Nicolás', 0, 'MEDIA'),
    ('¿Qué provincia argentina alberga el Parque Nacional Talampaya?', 'San Luis', 'Mendoza', 'Catamarca', 'La Rioja', 3, 'MEDIA'),
    ('¿Qué músico argentino compuso Libertango?', 'Aníbal Troilo', 'Astor Piazzolla', 'Osvaldo Pugliese', 'Juan D’Arienzo', 1, 'MEDIA'),
    ('¿Quién escribió El túnel?', 'Roberto Arlt', 'Ricardo Piglia', 'Ernesto Sabato', 'Julio Cortázar', 2, 'MEDIA'),
    ('¿En qué año tuvo lugar la batalla de Caseros?', '1845', '1852', '1859', '1861', 1, 'COMPLICADA'),
    ('¿Qué científico argentino compartió el Nobel de Medicina de 1984 por el trabajo sobre anticuerpos monoclonales?', 'Bernardo Houssay', 'Luis Federico Leloir', 'César Milstein', 'René Favaloro', 2, 'COMPLICADA'),
    ('¿Qué ley de 1912 estableció el voto secreto y obligatorio para los varones argentinos?', 'Ley 8871', 'Ley 1420', 'Ley 13010', 'Ley 24012', 0, 'COMPLICADA'),
    ('¿Quién escribió Museo de la Novela de la Eterna, publicado de forma póstuma?', 'Oliverio Girondo', 'Leopoldo Marechal', 'Ricardo Güiraldes', 'Macedonio Fernández', 3, 'COMPLICADA'),
    ('¿En qué provincia argentina se encuentra el Parque Nacional El Leoncito?', 'Mendoza', 'San Juan', 'La Rioja', 'San Luis', 1, 'COMPLICADA'),
    ('¿Qué ciudad fue sede del Congreso que sancionó la Constitución argentina de 1853?', 'Paraná', 'Buenos Aires', 'Santa Fe', 'San Miguel de Tucumán', 2, 'COMPLICADA'),
    ('¿En qué año se lanzó el satélite argentino ARSAT-1?', '2014', '2010', '2012', '2016', 0, 'COMPLICADA'),
    ('¿Qué arqueólogo argentino inició excavaciones en el Pucará de Tilcara junto con Salvador Debenedetti?', 'Florentino Ameghino', 'Juan Bautista Ambrosetti', 'Eduardo Holmberg', 'Francisco Moreno', 1, 'COMPLICADA'),
    ('¿Quién dirigió la película argentina La historia oficial?', 'Adolfo Aristarain', 'Fernando Solanas', 'Luis Puenzo', 'Héctor Olivera', 2, 'COMPLICADA'),
    ('¿En qué provincia está el campo de lava de la Payunia?', 'Neuquén', 'Río Negro', 'San Juan', 'Mendoza', 3, 'COMPLICADA')
ON CONFLICT (enunciado) DO NOTHING;

