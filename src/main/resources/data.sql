INSERT INTO Usuario( email, username, password, rol, activo)
VALUES( 'test@unlam.edu.ar', 'test123', '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08', 'ADMIN', true)
ON CONFLICT DO NOTHING;
