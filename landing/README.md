# Landing de T4KASH

Página pública de T4KASH: <https://charlieuxv.github.io/t4kash-landing/>

Construida con **React + TypeScript + Tailwind CSS** sobre Vite. El código fuente
vive en `web/` y el resultado del build queda al lado, en esta misma carpeta.

## Dónde vive y dónde se publica

Acá vive **el código** de la página, para que el sitio y la aplicación que muestra
viajen juntos en el mismo repositorio.

Lo que sirve GitHub Pages es un repositorio aparte:

| | |
|---|---|
| Repositorio publicado | <https://github.com/Charlieuxv/t4kash-landing> |
| Sitio en vivo | <https://charlieuxv.github.io/t4kash-landing/> |

El resultado del build (`index.html`, `assets/`, `media/`) **no se versiona acá**:
está en el `.gitignore` de la raíz. Si se guardara, cada compilación dejaría una
copia nueva de los archivos con hash y del video de 2,8 MB, y el repositorio
crecería sin necesidad. Para actualizar el sitio en vivo se compila y se copia al
repositorio publicado:

```bash
# desde la raíz de T4k4sh, con el otro repositorio clonado al lado
cd landing/web && npm run build && cd ../..
rsync -a --exclude node_modules --exclude .git landing/ ../t4kash-landing/
cd ../t4kash-landing && git add -A && git commit -m "..." && git push
```

## Estructura

```
landing/
  web/
    src/
      components/ Button, Logo, Pattern, Reveal, Ui
      sections/   Una por sección de la página
      layouts/    Navbar y Footer
      data/       Todo el texto y la lista de capturas
      hooks/      useReveal (scroll-reveal con IntersectionObserver)
      assets/     Capturas de la app y logotipo, en WebP
    public/media/ Video demo, imagen Open Graph, iconos
```

Al compilar aparecen además `index.html`, `assets/` y `media/` dentro de
`landing/`. No se versionan; son el material que se copia al repositorio
publicado.

## Si la URL cambia

La dirección del sitio está escrita a mano en cuatro lugares. Si el repositorio
se mueve de cuenta o se le pone un dominio propio, hay que tocar los cuatro:

- `landing/web/index.html` — `canonical`, `og:url`, `og:image`, `twitter:image` y el JSON-LD
- `landing/web/public/sitemap.xml`
- `landing/web/public/robots.txt`
- este README y el enlace del README principal

## Trabajar en local

```bash
cd landing/web
npm install
npm run dev      # http://localhost:5173
```

## Publicar

```bash
cd landing/web
npm run build    # limpia ../assets y escribe el bundle en landing/
```

El build deja `index.html`, `assets/` y `media/` en `landing/`, sin versionar.
Para que llegue al sitio en vivo hay que copiarlo al repositorio publicado, como
se explica más arriba.

## Sistema visual

Sale del *Manual de Identidad Visual de T4KASH*, no de una interpretación libre.

| Elemento | Origen |
|---|---|
| `#191919`, `#C1FF72`, `#5749FD`, `#0A74FF`, `#FFFFFF` | Página «Paleta de colores» |
| Texto negro sobre lima y sobre azul; texto blanco sobre morado y sobre tinta | Página «Contraste y accesibilidad (WCAG AA)» |
| Textura de cuadrícula | Página «Patrones y texturas» |
| Patrón modular (cuartos de círculo, cápsulas, triángulos) | Página «Patrones y texturas» |
| Codos redondeados tono sobre tono | Página «Contrastes» |
| Esquina achaflanada de los paneles | Recurso recurrente del manual |
| Logotipo e isotipo | Extraídos del PDF del manual |

**Tipografías.** El manual usa **Cosmic Octo** para títulos y **Codec Pro / Futura**
para texto. Las dos son comerciales y su licencia no cubre redistribución como
webfont, así que la página usa **Rubik 900** y **Outfit** como sustitutas medidas
contra las originales. Los stacks ya piden primero las de marca y `src/index.css`
tiene los `@font-face` listos y comentados: en cuanto existan los `.woff2` con
licencia web, se descomentan y no hay que tocar nada más.

## De dónde sale cada dato

Nada de la página está inventado. Los números y las afirmaciones salen del código
de este mismo repositorio o de las capturas reales de la aplicación.

| Dato en la página | Origen |
|---|---|
| 24 categorías | Contador visible en la pantalla de inicio de la app |
| Vigencia de 24 h y tope de C$ 1,000 en tareas rápidas | README del producto, sección «Uso del MVP» |
| Radio de búsqueda configurable | Pantalla de tareas rápidas |
| Comisión: 10 % al cliente + 5 % al estudiante | `PaymentService.java` y `application.properties` |
| 0 % de comisión en efectivo | `cash ? BigDecimal.ZERO : ...` en `PaymentService` |
| Moneda NIO | `schema-postgresql.sql` |
| «Pago protegido» | Es el nombre que usa la propia app |
| Calificación mutua de 1 a 5 | `CalificacionService` y tabla `calificaciones` |
| Android 7.0+ | `minSdk 24` en `mobile/app/build.gradle.kts` |
| Pagadito en entorno de pruebas | Integración en Sandbox, declarada como tal |
| Notificaciones push en integración | Rama separada, declarado como tal |
| Capturas y video | Aplicación funcionando, sin retoques de interfaz |

Dos capturas se editaron **solo para quitar datos personales**, sin cambiar nada
de la interfaz: en la pantalla de acceso se vació el campo de correo, y del perfil
se recortó el bloque «Cuenta», que mostraba un correo real y el nombre de una
universidad.

## Lo que la página no dice

- No promete ingresos ni cantidad de oportunidades.
- No presenta Pagadito como dinero real.
- No presenta las notificaciones push como terminadas.
- No nombra universidades ni sugiere convenios con ninguna.
- No inventa usuarios, montos procesados, testimonios ni clientes.
