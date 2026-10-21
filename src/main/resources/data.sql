<<<<<<< HEAD
INSERT IGNORE INTO Usuario(id, email, username, password, rol, activo)
VALUES(null, 'test@unlam.edu.ar', 'test123', '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08', 'ADMIN', true);


INSERT INTO Evento(id, descripcion, tipoEfecto, valor, tipoEventoGlobal, DTYPE)
VALUES (NULL,'Tramites en AFIP: Caíste a hacer un trámite y hay terrible fila. Todos tiran los dados y el que saque la mayor suma se saltea toda la fila y avanza hasta el casillero del jugador más adelantado. Si hay empate, los empatados vuelven a tirar hasta que uno se gane el pase VIP.', 'MOVER_CASILLEROS', 0, 'TRAMITES_AFIP', 'EventoGlobal'),
VALUES(null, 'Se inundó el rancho: Se largó el diluvio en La Matanza y el barrio parece una pileta municipal. Todos tiran los dados, el que saque la mayor suma de dados se lo lleva el agua puesto y retrocede 3 casilleros. Si hay empate, los que empataron con la mayor suma vuelven a tirar hasta que uno se coma el garrón. ', 'MOVER_CASILLEROS', -3, 'INUNDACION', 'EventoGlobal'),
VALUES(null, 'Piquete: Quedaste atrapado en un piquete de volqueteros. Todos tiran los dados, el que saque la menor cantidad de numero en dados, vuelve al inicio. Si hay empate, los que empataron con la menor suma vuelven a tirar hasta que uno se coma el garrón.', 'VOLVER_INICIO', 0, 'PIQUETE', 'EventoGlobal'),
VALUES(null, '¡Cuidado, andan robando por la zona! [Nombre del jugador] fue víctima de un robo y perdió $1000.', 'PERDER_PLATA', 1000, 'EL_KEVIN', 'EventoGlobal'),
VALUES(null, 'Se le quedó el Fiat 600 a Germancho! Se quedó a pata en plena Ruta 3 y [nombre del jugador] se puso la 10 para ayudarlo a empujar la máquina. Germancho le tiró 400 mangos por la changuita.', 'GANAR_PLATA', 400, 'FIAT_600', 'EventoGlobal'),
VALUES(null, 'Hubo un accidente en el tren Sarmiento y el servicio quedó parado. Justo [nombre del jugador] venía viajando y quedó atrapado en el medio del quilombo. Mala ahí, [nombre del jugador] perdiste el turno esperando que vuelva a funcionar.', 'PERDER_TURNO', 1, 'ACCIDENTE_SARMIENTO', 'EventoGlobal');




/* VALUES(null, null, null, null, null); */
=======
INSERT INTO Usuario( email, username, password, rol, activo)
VALUES( 'test@unlam.edu.ar', 'test123', '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08', 'ADMIN', true)
ON CONFLICT DO NOTHING;
>>>>>>> 27638bc7aec4c6deb189fd61a0e664056be22db1
