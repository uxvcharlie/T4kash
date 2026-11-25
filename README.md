<div align="center">
  <img src="./Logotipo%20Negro.png" alt="T4KASH" width="190">
  <h1>T4KASH</h1>
  <p><strong>Oportunidades, talento y experiencia para la comunidad universitaria.</strong></p>
  <p>
    <a href="./mobile"><img src="https://img.shields.io/badge/Android-Kotlin-3DDC84?logo=android&amp;logoColor=white" alt="Android Kotlin"></a>
    <a href="./backend"><img src="https://img.shields.io/badge/API-Spring_Boot-6DB33F?logo=springboot&amp;logoColor=white" alt="API Spring Boot"></a>
    <a href="./database"><img src="https://img.shields.io/badge/Database-PostgreSQL-4169E1?logo=postgresql&amp;logoColor=white" alt="PostgreSQL"></a>
    <a href="https://github.com/uxvcharlie/T4kash/actions/workflows/ci.yml"><img src="https://github.com/uxvcharlie/T4kash/actions/workflows/ci.yml/badge.svg?branch=main" alt="CI"></a>
    <a href="#control-de-versiones"><img src="https://img.shields.io/badge/Commits-Conventional-191919" alt="Conventional Commits"></a>
  </p>
  <p>
    <a href="https://charlieuxv.github.io/t4kash-landing/">Landing</a> ·
    <a href="https://t4k4sh.onrender.com/api/health">API</a> ·
    <a href="https://www.figma.com/design/k5PeSZUFQJgIja3Mw2BpAw/Sin-t%C3%ADtulo?node-id=0-1">Figma</a>
  </p>
</div>

T4KASH es una aplicación móvil que conecta estudiantes con personas, empresas e
instituciones que publican microtrabajos, tutorías, proyectos y oportunidades flexibles.
El MVP reúne marketplace, red universitaria, seguimiento de trabajos, comunicación y
pagos de prueba en un flujo trazable.

> **Entorno de demostración:** la API se ejecuta en Render, PostgreSQL y los archivos
> se administran en Supabase, y Android consume el backend mediante HTTPS.

## Contenido

- [Producto](#producto)
- [Funciones principales](#funciones-principales)
- [Capturas](#capturas)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Estructura](#estructura-del-repositorio)
- [Instalación](#instalación-y-ejecución)
- [Uso de la API](#uso-de-la-api)
- [Seguridad](#seguridad)
- [Pruebas](#pruebas)
- [Despliegue](#despliegue)
- [Base de datos y diagramas](#base-de-datos-y-diagramas)
- [Control de versiones](#control-de-versiones)

## Producto

Los estudiantes necesitan obtener ingresos y experiencia sin abandonar sus horarios
académicos. Al mismo tiempo, muchas personas requieren ayuda puntual, pero los canales
informales dificultan verificar perfiles, organizar postulaciones, controlar entregas y
dar seguimiento a los pagos.

T4KASH organiza ese proceso:

```mermaid
flowchart LR
    A[Publicar oportunidad] --> B[Explorar y postularse]
    B --> C[Aceptar propuesta]
    C --> D[Confirmar pago]
    D --> E[Coordinar por chat]
    E --> F[Registrar entrega]
    F --> G[Aprobar y cerrar]
```

### Estado del MVP

| Área | Estado de demostración |
|---|---|
| Marketplace, tareas rápidas y mapa | Operativo |
| Registro, correo, segundo paso y recuperación | Operativo |
| Postulaciones, trabajos, entregas y chat | Operativo |
| Wallet y Pagadito | Integrado en Sandbox |
| Archivos privados | Integrado con Supabase Storage |
| Network universitario | Operativo |
| Moderación, reportes y administración | Operativo |
| Calificaciones y reputación | Operativo |
| KYC de identidad | Integrado con Didit Hosted Sessions |
| Notificaciones push FCM | Backend listo; Android necesita `google-services.json` y un servicio de mensajería |
| Aplicación iOS (SwiftUI) | Implementada contra la misma API; pendiente de compilar y probar en Xcode |

## Funciones principales

### Marketplace

- Publicación y edición de oportunidades remotas, presenciales e híbridas.
- Categorías, habilidades, presupuesto y fechas de postulación y entrega.
- Postulaciones con propuesta económica y aceptación por el propietario.
- Límite de trabajos activos y control de estados.
- Entregas, comentarios, correcciones y aprobación.
- Archivos adjuntos privados de hasta 10 MB desde Android.

### Tareas rápidas

- Búsqueda por ubicación y radio configurable.
- Vigencia automática de 24 horas.
- Asignación inmediata para oportunidades urgentes cercanas.
- Presupuesto máximo de C$1,000 y confirmación de efectivo por ambas partes.

### Identidad y perfiles

- Registro con correo personal o institucional.
- Detección automática de universidad por dominio.
- Selección de carrera para dominios universitarios reconocidos.
- Verificación de correo, segundo paso de acceso y recuperación de contraseña.
- Nombre de usuario público único y perfil consultable por arroba.
- Verificación estudiantil administrativa y KYC alojado mediante Didit.

### Comunicación y comunidad

- Conversaciones vinculadas a los participantes de un trabajo.
- Mensajes, estados de lectura y notificaciones internas.
- Feed universitario con publicaciones, comentarios, reacciones y guardados.
- Reportes y herramientas administrativas de moderación.

### Finanzas de demostración

- Wallet calculada desde movimientos verificables.
- Checkout y consulta de pagos mediante Pagadito Sandbox.
- Fondos retenidos, liberación, reembolso y disputas auditables.
- Efectivo únicamente para tareas presenciales y tareas rápidas.
- Tarifa de servicio visible antes de confirmar el pago.

## Capturas

<div align="center">
  <table>
    <tr>
      <td align="center"><strong>Marketplace</strong></td>
      <td align="center"><strong>Tareas rápidas</strong></td>
      <td align="center"><strong>Wallet Sandbox</strong></td>
      <td align="center"><strong>Chat</strong></td>
    </tr>
    <tr>
      <td><img src="./docs/capturas/inicio.png" alt="Inicio de T4KASH" width="210"></td>
      <td><img src="./docs/capturas/tareas-rapidas.png" alt="Mapa de tareas rápidas" width="210"></td>
      <td><img src="./docs/capturas/wallet.png" alt="Wallet transaccional" width="210"></td>
      <td><img src="./docs/capturas/chat.png" alt="Conversación de trabajo" width="210"></td>
    </tr>
  </table>
</div>

## Arquitectura

```mermaid
flowchart LR
    A[Android<br/>Jetpack Compose] -->|HTTPS / REST| B[API Spring Boot<br/>Render]
    A -->|Mapas y teselas| M[OpenFreeMap<br/>MapLibre]
    B -->|JDBC + SSL| D[(PostgreSQL<br/>Supabase)]
    B -->|Archivos privados| S[Supabase Storage]
    B -->|Correo HTTPS| E[Brevo]
    B -->|Sesiones KYC| K[Didit]
    K -->|Webhook firmado| B
    B -->|WSPG Sandbox| P[Pagadito]
    P -->|Confirmación / consulta| B
```

- Android nunca se conecta directamente a PostgreSQL ni recibe claves privadas.
- Spring Boot concentra autenticación, reglas de negocio, autorización y persistencia.
- Supabase administra PostgreSQL y el bucket privado de archivos.
- Brevo envía códigos de activación, acceso y recuperación mediante HTTPS.
- Didit procesa documento, prueba de vida y coincidencia facial; T4KASH conserva estados
  y una huella irreversible del documento.
- Pagadito funciona exclusivamente en Sandbox durante la demostración.

## Tecnologías

| Capa | Tecnologías |
|---|---|
| Android | Kotlin, Jetpack Compose, Navigation Compose |
| Red móvil | Retrofit, OkHttp, Gson |
| Mapas | MapLibre Compose, OpenFreeMap |
| iOS | Swift, SwiftUI, Swift Concurrency, Observation |
| Red iOS | URLSession y Codable, sin dependencias de terceros |
| Mapas iOS | MapKit |
| Backend | Java 21, Spring Boot, Spring Data JPA |
| API | REST, Bean Validation, Swagger / OpenAPI |
| Datos | PostgreSQL, Supabase, Supabase Storage |
| Integraciones | Brevo, Didit, Pagadito Sandbox |
| Desarrollo local | Docker Compose, Maven, Gradle |
| Despliegue | Render y Supabase |

Las versiones exactas se administran en
[`backend/pom.xml`](./backend/pom.xml) y
[`mobile/gradle/libs.versions.toml`](./mobile/gradle/libs.versions.toml).

## Estructura del repositorio

```text
T4k4sh/
├── backend/
│   ├── src/main/              API organizada por módulos
│   ├── src/test/              Pruebas unitarias e integrales
│   ├── Dockerfile             Imagen de la API
│   ├── docker-compose.yml     API y PostgreSQL local
│   └── pom.xml                Dependencias Maven
├── mobile/
│   ├── app/src/main/          Aplicación Android
│   └── gradle/                Catálogo de dependencias
├── ios/
│   ├── T4KASH.xcodeproj       Proyecto Xcode
│   ├── T4KASH/                Aplicación iOS (SwiftUI)
│   ├── T4KASHTests/           Pruebas unitarias
│   └── README.md              Configuración, compilación y limitaciones
├── database/
│   ├── README.md              Cómo se versiona el esquema (Flyway)
│   └── sqlserver-original.sql Referencia histórica
├── landing/
│   ├── web/                   Página pública: React, TypeScript y Tailwind
│   └── README.md              Cómo compilarla y dónde se publica
├── docs/
│   ├── capturas/              Evidencia visual actual
│   └── diagramas/             ERD y UML
├── render.yaml                Configuración de Render
└── README.md
```

El backend sigue una estructura modular por dominio: `identity`, `marketplace`,
`communication`, `finance`, `network`, `moderation` y `admin`. Cada módulo separa
controladores, servicios, repositorios, entidades y objetos de transferencia.

## Instalación y ejecución

### Requisitos

- Git.
- JDK 21.
- Docker Desktop con Docker Compose.
- Android Studio y un emulador o teléfono Android.
- Para la aplicación iOS: macOS con Xcode 16 o superior.
- Conexión a Internet para el entorno de demostración.

### Backend y PostgreSQL local

```powershell
git clone https://github.com/uxvcharlie/T4kash.git
cd T4kash\backend
copy .env.example .env
docker compose up -d --build
```

| Servicio | Dirección local |
|---|---|
| API | `http://localhost:8080/api/` |
| Swagger (solo fuera del perfil `prod`) | `http://localhost:8080/swagger-ui/index.html` |
| Health | `http://localhost:8080/api/health` |

```powershell
docker compose logs -f api
docker compose down
```

Al arrancar, el API crea o actualiza el esquema con las migraciones Flyway de
[`backend/src/main/resources/db/migration`](./backend/src/main/resources/db/migration).

### Android contra Render

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

APK generado:

```text
mobile/app/build/outputs/apk/debug/app-debug.apk
```

### Android contra el backend local

```powershell
.\gradlew.bat :app:assembleDebug `
  -PT4KASH_API_BASE_URL=http://10.0.2.2:8080/api/
```

`10.0.2.2` representa la computadora anfitriona desde el emulador de Android.

<details>
<summary><strong>Variables de entorno</strong></summary>

Todas las variables, con valores de ejemplo y explicación, están en
[`backend/.env.example`](./backend/.env.example). Una prueba del backend falla si el
código lee una variable que no está documentada ahí. Ningún secreto debe guardarse en Git.

| Grupo | Variables principales |
|---|---|
| PostgreSQL | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| API | `SERVER_PORT`, `SPRING_PROFILES_ACTIVE`, `APP_PUBLIC_BASE_URL`, `APP_CORS_ALLOWED_ORIGINS`, `APP_HTTP_TRUSTED_PROXY_HOPS` |
| Seguridad | `APP_AUTH_CODE_SECRET`, `APP_AUTH_LOGIN_MAX_ATTEMPTS*`, `APP_RATE_LIMIT_*` |
| Supabase Storage | `SUPABASE_URL`, `SUPABASE_SECRET_KEY`, `SUPABASE_STORAGE_BUCKET` |
| Correo | `APP_MAIL_ENABLED`, `APP_MAIL_PROVIDER`, `APP_MAIL_FROM`, `BREVO_API_KEY`, `SMTP_*` (respaldo) |
| Push | `FIREBASE_CREDENTIALS_JSON` |
| Didit | `APP_DIDIT_ENABLED`, `DIDIT_API_KEY`, `DIDIT_WORKFLOW_ID`, `DIDIT_WEBHOOK_SECRET`, `DIDIT_DOCUMENT_HASH_SECRET` |
| Pagos | `APP_PAYMENTS_CLIENT_FEE_PERCENT`, `APP_PAYMENTS_STUDENT_FEE_PERCENT`, `APP_PAGADITO_ENABLED`, `APP_PAGADITO_ENVIRONMENT`, `PAGADITO_UID`, `PAGADITO_WSK` |
| Administración | `APP_AUTH_EVALUATOR_EMAILS`, `APP_AUTH_ADMIN_EMAILS` |
| Android | `T4KASH_API_BASE_URL` |

Render administra las variables del backend. Android recibe únicamente la URL pública de
la API.

</details>

### iOS

```bash
cd ios
open T4KASH.xcodeproj
```

La URL de la API se configura con el build setting `T4KASH_API_BASE_URL`, y el
identificador con `T4KASH_BUNDLE_ID`. El simulador comparte la red del Mac, así que
para un backend local se usa `http://localhost:8080/api/`, no `10.0.2.2`.

Los detalles de configuración, firma, verificación en Xcode y limitaciones conocidas
están en [`ios/README.md`](./ios/README.md). El diagnóstico que guió la migración está
en [`docs/IOS_MIGRATION_AUDIT.md`](./docs/IOS_MIGRATION_AUDIT.md).

## Uso de la API

La documentación interactiva (Swagger) contiene todos los contratos. En producción está
apagada para no publicar el mapa de rutas; se consulta levantando el backend en local
(`http://localhost:8080/swagger-ui/index.html`).

- **Health check:** https://t4k4sh.onrender.com/api/health
- **Base URL:** `https://t4k4sh.onrender.com/api/`

### Autenticación en dos pasos

1. `POST /api/auth/login` valida correo y contraseña y envía un código temporal.
2. `POST /api/auth/login/verify` confirma el código y entrega la sesión.
3. Los endpoints privados utilizan `Authorization: Bearer <token>`.

```json
{
  "correo": "estudiante@universidad.edu.ni",
  "password": "contraseña-segura"
}
```

```json
{
  "correo": "estudiante@universidad.edu.ni",
  "codigo": "123456"
}
```

### Crear una oportunidad

```http
POST /api/tasks
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "idCategoria": 1,
  "titulo": "Diseñar afiche para actividad universitaria",
  "descripcion": "Crear una propuesta lista para redes sociales.",
  "presupuesto": 500,
  "tipoOportunidad": "TAREA",
  "modalidad": "REMOTA",
  "fechaLimitePostulacion": "2026-09-02T18:00:00",
  "fechaLimite": "2026-09-05T18:00:00"
}
```

<details>
<summary><strong>Grupos de endpoints implementados</strong></summary>

| Grupo | Prefijos principales |
|---|---|
| Autenticación | `/api/auth`, `/api/identity` |
| Perfiles y verificaciones | `/api/profiles`, `/api/student-verifications`, `/api/identity-verifications` |
| Marketplace | `/api/tasks`, `/api/quick-tasks`, `/api/applications`, `/api/jobs`, `/api/deliveries` |
| Comunicación | `/api/conversations`, `/api/notifications` |
| Network | `/api/network` |
| Finanzas | `/api/wallet`, `/api/payments`, `/api/disputes` |
| Archivos | `/api/attachments`, adjuntos de tareas y entregas |
| Moderación | `/api/reports`, `/api/admin` |

Swagger muestra método, cuerpo, respuesta, validaciones y autorización de cada operación.

</details>

## Seguridad

| Control | Aplicación |
|---|---|
| Contraseñas | Hash BCrypt; nunca se almacena la contraseña original |
| Sesiones | Token aleatorio; PostgreSQL conserva solamente su hash SHA-256 |
| Segundo paso | Código temporal enviado por correo después de validar credenciales |
| Códigos de 6 dígitos | Se guardan como HMAC-SHA256 con sal y un secreto fuera de la base |
| Intentos | Bloqueo por correo + IP y tope por cuenta; límite de peticiones por IP en `/api/auth/**` |
| Ubicación | Dirección y coordenadas exactas solo para el cliente y el estudiante asignado; el resto ve la zona (~550 m) |
| Autorización | Roles, propiedad del recurso y participación en el trabajo |
| Entradas | Bean Validation, restricciones PostgreSQL y errores uniformes |
| Archivos | Bucket privado, validación de tipo/tamaño y descarga mediante la API |
| Webhooks | Firma, ventana temporal e idempotencia para Didit y pagos |
| KYC | Datos biométricos procesados por Didit; T4KASH conserva referencias mínimas |
| Base de datos | RLS bloquea el acceso con claves públicas de Supabase |
| Secretos | Variables privadas de Render; nunca se incluyen en Android ni Git |
| Producción | Perfil `prod`: sin Swagger, logs en INFO, sin CORS abierto y contenedor sin root |

La aplicación toma el usuario desde la sesión autenticada. Android no puede declarar por
su cuenta quién es propietario, estudiante, cliente o administrador de una operación.

## Pruebas

### Backend

```bash
cd backend
sh mvnw test        # Windows: .\mvnw.cmd test
```

Las pruebas cubren identidad, seguridad, marketplace, entregas, finanzas, archivos,
moderación, comunicación, Network, migraciones Flyway y el perfil `prod`. Las de
integración usan PostgreSQL real con Testcontainers (Docker o Podman); sin Docker se
omiten, pero en CI siempre corren.

Cada PR corre el workflow [CI](./.github/workflows/ci.yml) (backend, Android y landing) y,
si cambia `ios/`, el de [iOS](./.github/workflows/ios.yml) en un simulador de macOS.

### Android

```powershell
cd mobile
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

Se validan formatos, fechas, moneda, distancias, dominios institucionales, manejo del
teclado y compilación del APK.

### iOS

```bash
cd ios
xcodebuild -project T4KASH.xcodeproj -scheme T4KASH \
  -destination 'platform=iOS Simulator,name=iPhone 16' test
```

Se validan la construcción de peticiones, la autenticación Bearer, el mapeo de errores
`ProblemDetail`, la decodificación de los modelos, el formato de fechas sin zona horaria,
las validaciones de formulario, el cuerpo multipart y los ViewModel de acceso y marketplace.

## Despliegue

| Componente | Servicio |
|---|---|
| API Docker | Render Web Service |
| PostgreSQL | Supabase Database |
| Archivos | Supabase Storage |
| Correo | Brevo Email API |
| KYC | Didit Hosted Sessions |
| Pagos | Pagadito Sandbox |
| Mapas | OpenFreeMap / MapLibre |

Flujo de publicación:

1. Si cambia la base, agregar una migración nueva en `backend/src/main/resources/db/migration`.
   No se ejecuta nada a mano en Supabase: Flyway la aplica al arrancar.
2. Subir el código mediante Pull Request y esperar el CI en verde.
3. Al fusionar en `main`, Render construye el `Dockerfile` y despliega la API.
4. Verificar `/api/health` y probar registro e inicio de sesión.
5. Compilar Android apuntando a la URL de Render.
6. Ejecutar una prueba funcional desde el emulador o teléfono.

El plan gratuito de Render suspende la instancia tras unos 15 minutos sin tráfico. El
workflow [mantener-demo-despierta](./.github/workflows/mantener-demo-despierta.yml) llama
al API cada 10 minutos, y Android muestra "Despertando el servidor…" si el primer arranque
tarda.

## Base de datos y diagramas

La fuente oficial del esquema son las migraciones Flyway de
[`backend/src/main/resources/db/migration`](./backend/src/main/resources/db/migration)
(ver [`database/README.md`](./database/README.md)). El modelo contiene 54 tablas normalizadas y organizadas en identidad, marketplace, finanzas, puntos,
comunicación, Network, moderación y auditoría.

Incluye:

- Llaves primarias y foráneas.
- Restricciones de dominio, unicidad y consistencia.
- Índices compuestos para consultas frecuentes.
- Catálogos iniciales de roles, universidades, carreras, categorías y habilidades.
- Idempotencia y trazabilidad para pagos y webhooks.
- Row Level Security para impedir acceso directo desde claves públicas.

Los diagramas ERD, clases, actividades y casos de uso se encuentran en
[`docs/diagramas`](./docs/diagramas).

> El esquema contiene instrucciones `DROP TABLE` para recrear entornos de desarrollo.
> Sobre una base con información deben aplicarse cambios específicos desde SQL Editor.

## Control de versiones

El equipo trabaja con GitHub Flow, Pull Requests y Conventional Commits:

- `main` está protegida: todo cambio entra por Pull Request, sin *force push* y con
  historial lineal (**Rebase and merge**).
- Las ramas son cortas y expresan su propósito (`feat/`, `fix/`, `docs/`, `chore/`,
  `test/`, `ci/`), nunca el nombre de una persona. Se borran al fusionar.
- La [plantilla de PR](./.github/pull_request_template.md) recuerda migraciones, variables
  de entorno y compatibilidad con las apps. Dependabot propone actualizaciones cada semana.

```text
feat/marketplace-tareas-rapidas
feat/firebase-notificaciones
fix/didit-webhook
docs/readme-final
```

Ejemplos de commits:

```text
feat: agregar búsqueda de tareas rápidas por radio
fix: validar la firma del webhook de identidad
docs: mejorar arquitectura y guía de ejecución
test: cubrir aceptación y entrega de trabajos
chore: ajustar variables del despliegue
```

Flujo aplicado:

```mermaid
flowchart LR
    A[Rama descriptiva] --> B[Commits convencionales]
    B --> C[Pull Request]
    C --> D[Revisión y pruebas]
    D --> E[Merge a main]
```

## Equipo

Proyecto desarrollado por **Thinking Out Loud** para el Hackathon 2026.
