## Qué cambia y por qué

<!-- Una o dos frases. Si arregla un hallazgo del plan, cítalo (por ejemplo, N2 o B6). -->

## Cómo se probó

<!-- Comandos que corriste y resultado. Ejemplo: sh mvnw test → 131 pruebas, 0 fallos. -->

## Antes de fusionar

- [ ] Las pruebas pasan en CI.
- [ ] Si cambia la base de datos, hay una migración nueva en `backend/src/main/resources/db/migration` (nunca editar una ya aplicada).
- [ ] Si agrega o cambia una variable de entorno, está en `application.properties`, `backend/.env.example` y `render.yaml`.
- [ ] Si cambia un endpoint o un DTO, Android e iOS siguen funcionando (o el PR los actualiza).
- [ ] No incluye secretos, `.env`, llaves ni capturas con datos reales.

## Pasos manuales después del deploy

<!-- Variables que cargar en Render, migraciones o pruebas en producción. "Ninguno" si no aplica. -->
