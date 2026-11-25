# Base de datos

El esquema se versiona con **Flyway**. Las migraciones están en
[`backend/src/main/resources/db/migration`](../backend/src/main/resources/db/migration)
y se aplican solas al arrancar el API.

| Archivo | Qué es |
|---|---|
| `V1__linea_base.sql` | El esquema que corría en producción antes de adoptar Flyway (54 tablas y datos semilla). |
| `V2__comision_por_lado.sql` | Reparto de la comisión entre cliente (10 %) y estudiante (5 %). |

## Reglas

- **Nunca** editar una migración ya aplicada: Flyway compara su checksum y el
  API no arranca. Cada cambio va en un archivo nuevo `V<n>__descripcion.sql`.
- No hay que correr ningún `.sql` a mano en Supabase. En una base que ya tenía
  las tablas pero no el historial de Flyway, la primera vez Flyway marca la
  línea base como versión 1 sin ejecutarla y aplica solo lo que sigue.
- Antes del primer arranque con Flyway en producción, conviene comparar el
  esquema real con `V1__linea_base.sql`
  (`pg_dump --schema-only`): si producción tiene cambios hechos a mano que no
  están en V1, hay que pasarlos a una migración nueva.
- En Supabase, usar el *session pooler* (puerto 5432) o la conexión directa.
  El *transaction pooler* (6543) no sirve para migrar.

`sqlserver-original.sql` es el modelo histórico en SQL Server; no se usa.
