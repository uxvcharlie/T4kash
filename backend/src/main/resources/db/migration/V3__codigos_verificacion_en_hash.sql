-- Los codigos de registro y de segundo factor se guardan como
-- v1$<sal>$<hmac> (101 caracteres) en lugar del codigo en texto plano.
-- Los pendientes en texto plano siguen validos hasta que venzan (minutos).
ALTER TABLE verificaciones_usuario
    ALTER COLUMN codigo_verificacion TYPE varchar(128);
