# Design — Moni

Fuente de verdad del diseño visual de Moni (web y Android). Toda decisión de diseño se registra aquí antes de implementarse. Agentes: leer este archivo antes de tocar UI.

## Dirección

Rebranding (iniciado 2026-11-22): aspecto **moderno y minimalista**, con **animaciones cuidadas** y buen acabado. Reemplaza el estilo inicial (esmeralda sobre slate).

## Estado de decisiones

| Decisión | Estado |
|---|---|
| Paleta de colores | ✅ **J4 Lavanda & Grafito — modo claro** (2026-11-22). Modo oscuro: pendiente de afinar |
| Tipografía | ✅ **Space Grotesk** (2026-11-22) |
| Forma (radios, sombras, densidad) | ✅ **S1 Suave** (2026-11-22) |
| Animaciones | ✅ **M2 Fluido** (2026-11-22) |
| Componentes clave (balance, lista, añadir) | Propuesta base (ver abajo), se valida al implementar |

## Paleta elegida: J4 Lavanda & Grafito (modo claro)

Grafito cálido neutro con lavanda como único color de marca. Minimalista. Implementar primero **solo el modo claro**; el modo oscuro de J4 es un punto de partida, no está aprobado.

| Token | Claro (aprobado) | Uso |
|---|---|---|
| bg | `#F7F6F8` | Fondo de pantalla |
| fg | `#1F1E22` | Texto principal |
| mut | `#74727A` | Texto secundario, fechas, etiquetas |
| card | `#FFFFFF` | Tarjetas, lista, formularios |
| line | `#E7E5EA` | Bordes y divisores |
| hero | `#2B2A2F` | Fondo de la tarjeta de balance |
| heroFg | `#D4CCF6` | Texto sobre la tarjeta de balance |
| acc | `#9C8CD9` | Botón primario, chip activo, foco |
| accFg | `#FFFFFF` | Texto sobre acento |
| inc | `#2F8F7A` | Montos de ingreso |
| exp | `#C2416B` | Montos de gasto |

Modo oscuro (borrador, sin aprobar): bg `#161518`, fg `#EDECF0`, mut `#9E9CA6`, card `#1F1E23`, line `#2E2C33`, hero `#26232F` / `#C8BEF5`, acc `#B4A7F5` / `#161518`, inc `#7DD8C0`, exp `#F28BAA`.

Mientras no se apruebe el oscuro, las apps usan siempre el modo claro.

## Tipografía elegida: Space Grotesk

Única familia para todo (títulos, cuerpo y números). Pesos: 400 cuerpo, 500 etiquetas, 600 énfasis y montos, 700 títulos y balance. Números con `font-variant-numeric: tabular-nums` (Compose: `fontFeatureSettings = "tnum"`). Títulos con tracking negativo (-0.02em); balance -0.03em.

- Web: `next/font/google` → `Space_Grotesk`.
- Android: TTF en `res/font` (o Downloadable Fonts) → `FontFamily` en el tema de Compose.

### Opciones consideradas

Todas en Google Fonts (web vía `next/font/google`, Android vía Downloadable Fonts o TTF en `res/font`).

| Opción | Familia | Carácter |
|---|---|---|
| T1 | Manrope | Geométrica y amable, números muy legibles. Moderna y discreta. |
| T2 | Bricolage Grotesque | Grotesca con carácter, formas ligeramente irregulares. Personalidad propia. |
| T3 | Instrument Serif (títulos y balance) + Instrument Sans (resto) | Editorial, de autor. |
| T4 | Space Grotesk | Técnica y geométrica, guiños retro. Producto digital. |

## Forma elegida: S1 Suave

Aireado y premium: radios grandes, sombras difusas teñidas, **sin bordes** en tarjetas.

| Elemento | Radio | Sombra / borde |
|---|---|---|
| Tarjeta de balance (hero) | 22px | `0 10px 24px -10px rgba(43,42,47,.45)` |
| Tarjeta / lista | 20px | `0 6px 20px -8px rgba(31,30,34,.12)`; divisores internos `#F0EEF2` 1px |
| Botón primario | 18px | `0 8px 18px -8px rgba(156,140,217,.8)` (sombra lavanda) |
| Chips / filtros | pill (999px) | `0 1px 2px rgba(0,0,0,.06)` |
| Icono de categoría | 10px | fondo `#EFEBFB` (lavanda muy clara) |
| Inputs | 14px | fondo `card`, foco con anillo `acc` 2px |

Espaciado: márgenes de pantalla 16px (móvil) / 32px (web), separación entre bloques 12–16px, filas de lista ~56px de alto. Compose: `RoundedCornerShape(22.dp)` etc. y `Modifier.shadow(elevation, shape, ambientColor/spotColor)` con los colores anteriores.

### Opciones consideradas

| Opción | Nombre | Descripción |
|---|---|---|
| S1 | Suave | Radios grandes (18–22px), sombras difusas teñidas, sin bordes. Aireado y premium. |
| S2 | Nítido | Radios pequeños (6–10px), bordes finos, sin sombras, compacto. Tipo herramienta. |
| S3 | Flotante | Cada movimiento es una pastilla suelta (radio 18px), botón redondo flotante (+). Ligero. |
| S4 | Contorno | Bordes grafito de 1.5px con sombra sólida lavanda desplazada. Gráfico y juguetón. |

## Animaciones elegidas: M2 Fluido

Vivo pero controlado: curvas tipo muelle con rebote suave, entradas escalonadas, números que cuentan.

| Momento | Animación | Web (CSS) | Android (Compose) |
|---|---|---|---|
| Curva base | Muelle con leve rebote | `cubic-bezier(.34,1.56,.64,1)` | `spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)` |
| Entrada de pantalla | Bloques suben 18px + escala .96→1 + fundido, 520ms | `@keyframes` con `animation-delay` | `AnimatedVisibility(enter = fadeIn() + slideInVertically { it/6 } + scaleIn(0.96f))` |
| Escalonado de lista | 70ms entre filas | `animation-delay: calc(70ms * i)` | `LaunchedEffect` + delay por índice o `animateItem()` |
| Tarjeta de balance | Escala .92→1 al entrar | keyframes | `scaleIn(initialScale = 0.92f)` |
| Balance (número) | Cuenta desde el valor anterior, ~900ms al cargar / 500ms al cambiar, ease-out cúbico | `requestAnimationFrame` | `animateFloatAsState(tween(500, easing = EaseOutCubic))` |
| Nuevo movimiento | Entra desde la izquierda (-30px) con escala .9→1 y fondo lavanda tenue `#F4F1FD` que se desvanece | keyframes | `Modifier.animateItem()` + `animateColorAsState` |
| Pulsar botón | Escala 0.94 (90ms) y vuelve con rebote (260ms) | `:active` + `transition` | `Modifier.graphicsLayer { scale }` con `animateFloatAsState(spring)` según `interactionSource` |
| Eliminar | Fundido + colapso de altura, 250ms | `transition` en grid-rows | `animateItem()` |

Reglas: nada dura más de ~600ms salvo el conteo inicial del balance; no animar en cada scroll; respetar `prefers-reduced-motion` y la escala de animaciones del sistema.

### Opciones consideradas

| Opción | Nombre | Descripción |
|---|---|---|
| M1 | Sutil | Fundido + subida de 6px, ~250ms ease-out. Números sin animar. Botón escala 0.98 al pulsar. |
| M2 | Fluido | Curva tipo muelle con rebote suave (~500ms), lista escalonada (70ms entre filas), balance cuenta hacia arriba, nuevo movimiento entra deslizando con escala. |
| M3 | Expresivo | Balance se revela de arriba abajo con brillo, lista entra lateral con desenfoque, monto del balance late al cambiar (~700ms). |

Todas respetan `prefers-reduced-motion` (web) y la escala de animaciones del sistema (Android).

## Componentes clave (propuesta base)

Derivados de las maquetas aprobadas (paleta J4 claro + Space Grotesk + S1 + M2):

- **Cabecera**: etiqueta `MONI` (10px, 600, tracking .12em, `mut`) + título "Tus finanzas" (22px, 700).
- **Tarjeta de balance (hero)**: fondo `hero`, texto `heroFg`; "Balance" pequeño al 70%; monto 30–32px 700 tabular; fila ↑ ingresos / ↓ gastos.
- **Filtros**: chips pill Todo / Ingresos / Gastos; activo en `acc` con texto blanco.
- **Lista de movimientos**: tarjeta blanca radio 20px; filas con icono de categoría (cuadro 10px, fondo `#EFEBFB`), nombre 600, fecha `mut`, monto alineado a la derecha en `inc` / `exp` con signo + / −.
- **Añadir**: botón primario ancho completo "Añadir movimiento" (radio 18px, sombra lavanda) que abre una hoja/formulario.

## Historial de rondas de paleta

## Paleta: ronda 1 (descartada)

Feedback del usuario: demasiado parecidas a apps existentes (lima tipo Revolut/Cash, azul neobanco, naranja tipo ING). Se pidió más personalidad. Se conservan como referencia de lo que **no** buscamos.

Cada opción define modo claro y oscuro. Tokens: `bg` fondo, `fg` texto, `mut` texto secundario, `card` superficie, `line` bordes, `hero` tarjeta de balance, `acc` acento (botones, chips activos), `inc` ingreso, `exp` gasto.

### A — Grafito & Lima
Neutros cálidos casi monocromos con acento lima eléctrico. Fintech moderno, alto contraste.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#FAFAF9` | `#0C0C0D` |
| fg | `#18181B` | `#F4F4F5` |
| mut | `#71717A` | `#A1A1AA` |
| card | `#FFFFFF` | `#17171A` |
| line | `#E7E5E4` | `#27272A` |
| hero / heroFg | `#18181B` / `#FAFAF9` | `#C6F432` / `#0C0C0D` |
| acc / accFg | `#C6F432` / `#18181B` | `#C6F432` / `#0C0C0D` |
| inc | `#3F8F1F` | `#A3E635` |
| exp | `#DC2626` | `#F87171` |

### B — Índigo nocturno
Índigo profundo y lavanda. Tranquilo, confiable, premium en oscuro.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#F7F7FB` | `#0B0B16` |
| fg | `#16162A` | `#ECECF6` |
| mut | `#6B6B85` | `#9A9AB8` |
| card | `#FFFFFF` | `#151527` |
| line | `#E4E4EF` | `#24243A` |
| hero / heroFg | `#4F46E5` / `#FFFFFF` | `#6366F1` / `#FFFFFF` |
| acc / accFg | `#4F46E5` / `#FFFFFF` | `#8B8BFF` / `#0B0B16` |
| inc | `#059669` | `#34D399` |
| exp | `#E11D48` | `#FB7185` |

### C — Arena & Terracota
Tonos tierra cálidos, papel y arcilla. Minimalista editorial.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#F6F1EA` | `#171311` |
| fg | `#2A2320` | `#EFE7DD` |
| mut | `#857A70` | `#A89C90` |
| card | `#FFFCF7` | `#211B18` |
| line | `#E6DDD1` | `#332A25` |
| hero / heroFg | `#2A2320` / `#F6F1EA` | `#C2552D` / `#FFF8F0` |
| acc / accFg | `#C2552D` / `#FFFFFF` | `#E07A50` / `#171311` |
| inc | `#4D7C4A` | `#8FBF7A` |
| exp | `#B4432A` | `#F08A6A` |

### D — Monocromo & Azul eléctrico
Blanco y negro puros con un único azul intenso. Estilo neobanco.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#FFFFFF` | `#000000` |
| fg | `#0A0A0A` | `#FAFAFA` |
| mut | `#737373` | `#8A8A8A` |
| card | `#F5F5F5` | `#111111` |
| line | `#EAEAEA` | `#1F1F1F` |
| hero / heroFg | `#0A0A0A` / `#FFFFFF` | `#2563EB` / `#FFFFFF` |
| acc / accFg | `#2563EB` / `#FFFFFF` | `#3B82F6` / `#FFFFFF` |
| inc | `#16A34A` | `#4ADE80` |
| exp | `#DC2626` | `#F87171` |

### E — Carbón & Mandarina
Carbón suave con naranja mandarina. Energético sin perder sobriedad.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#F8F8F7` | `#121212` |
| fg | `#1C1B1A` | `#F5F5F4` |
| mut | `#78716C` | `#A8A29E` |
| card | `#FFFFFF` | `#1C1C1B` |
| line | `#E7E5E4` | `#2A2928` |
| hero / heroFg | `#FF6B1A` / `#FFFFFF` | `#FF7A2E` / `#121212` |
| acc / accFg | `#FF6B1A` / `#FFFFFF` | `#FF7A2E` / `#121212` |
| inc | `#15803D` | `#4ADE80` |
| exp | `#B91C1C` | `#F87171` |

## Paleta: ronda 2 (con personalidad)

Combinaciones poco habituales en apps de finanzas. Mismos tokens que la ronda 1.

### F — Musgo & Mantequilla
Verde musgo profundo con amarillo mantequilla sobre crema. Orgánico, cálido, libreta de campo.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#F5F1E6` | `#121710` |
| fg | `#1F2A1C` | `#EDE8D8` |
| mut | `#6E7363` | `#9DA28F` |
| card | `#FFFDF6` | `#1B2218` |
| line | `#E3DDCB` | `#2A3326` |
| hero / heroFg | `#2F3E2C` / `#F5E7B8` | `#E9C552` / `#1F2A1C` |
| acc / accFg | `#E9C552` / `#1F2A1C` | `#E9C552` / `#121710` |
| inc | `#4F7A3A` | `#A6C98A` |
| exp | `#B5543A` | `#E58A6C` |

### G — Ciruela & Melocotón
Ciruela oscura con melocotón suave. Sofisticado y afectuoso, poco visto en finanzas.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#FBF5F2` | `#150A13` |
| fg | `#2D1427` | `#F6E7EE` |
| mut | `#856B7C` | `#B095A6` |
| card | `#FFFFFF` | `#21121D` |
| line | `#EFE1DD` | `#34202F` |
| hero / heroFg | `#4A1F3D` / `#FFD9C4` | `#FF9E7A` / `#2D1427` |
| acc / accFg | `#FF9E7A` / `#2D1427` | `#FFB394` / `#150A13` |
| inc | `#3E8A6A` | `#7FD1AC` |
| exp | `#C23A5A` | `#F27A9A` |

### H — Petróleo & Cobre
Azul petróleo casi negro con cobre bruñido. Serio y elegante, brillo metálico.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#F2F4F3` | `#081314` |
| fg | `#0E2426` | `#E4ECEB` |
| mut | `#5F7473` | `#8DA3A1` |
| card | `#FFFFFF` | `#0F1E20` |
| line | `#DCE3E2` | `#1C2F31` |
| hero / heroFg | `#0F3B3E` / `#F3D6B8` | `#C8743A` / `#081314` |
| acc / accFg | `#C8743A` / `#FFFFFF` | `#E0915A` / `#081314` |
| inc | `#2E7D6B` | `#6CCBB2` |
| exp | `#B8452E` | `#EE8A6E` |

### I — Atardecer
Violeta crepuscular con albaricoque sobre rosado tenue. Emocional y suave.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#FBF4F1` | `#100F22` |
| fg | `#24213F` | `#ECE9F7` |
| mut | `#7A7392` | `#A29DC0` |
| card | `#FFFFFF` | `#1A1934` |
| line | `#EEE3E3` | `#2A2848` |
| hero / heroFg | `#2B2D5C` / `#FFD3AE` | `#F6A96B` / `#1A1934` |
| acc / accFg | `#F6A96B` / `#24213F` | `#FFB987` / `#100F22` |
| inc | `#3C8C7A` | `#79D3BE` |
| exp | `#D04A5E` | `#FF8A9B` |

### J — Oliva & Lavanda
Oliva grisáceo con lavanda empolvada. Inusual, calmada, de autor.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#F4F3EE` | `#121310` |
| fg | `#23251C` | `#ECECE4` |
| mut | `#73766A` | `#A0A294` |
| card | `#FFFFFF` | `#1B1C17` |
| line | `#E2E1D8` | `#2B2D25` |
| hero / heroFg | `#4B4F36` / `#E2DAF5` | `#B8AAF0` / `#1B1C17` |
| acc / accFg | `#9C8CD9` / `#FFFFFF` | `#B8AAF0` / `#121310` |
| inc | `#557A3B` | `#A9CC86` |
| exp | `#B4485A` | `#EE8A9A` |

## Paleta: ronda 3 (variantes lavanda)

Feedback ronda 2: gusta la **lavanda** de J, pero quitar el **verde oliva**; el **modo oscuro de J no convence** (negro verdoso + bloque pastel brillante).

Cambios aplicados en todas las variantes:
- Sin oliva. Ingreso pasa a verde azulado (`#2F8F7A` / `#7DD8C0`) y gasto a rosa frambuesa, que armonizan con lavanda.
- Modo oscuro: fondos con leve tinte violeta (no negro puro ni verdoso); la tarjeta de balance es una superficie profunda con texto lavanda en vez de un bloque pastel.

### J1 — Lavanda & Tinta
Lavanda con tinta violeta-negra. Contraste alto; oscuro índigo profundo.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#F7F6FB` | `#121020` |
| fg | `#1C1A2B` | `#ECE9F8` |
| mut | `#716D86` | `#9C97B8` |
| card | `#FFFFFF` | `#1B1830` |
| line | `#E6E3F0` | `#2A2645` |
| hero / heroFg | `#1E1B2E` / `#CFC6F7` | `#2A2448` / `#CFC6F7` |
| acc / accFg | `#8F7EE0` / `#FFFFFF` | `#B4A7F5` / `#121020` |
| inc | `#2F8F7A` | `#7DD8C0` |
| exp | `#C2416B` | `#F28BAA` |

### J2 — Lavanda & Berenjena
Lavanda con berenjena. Cálido y con carácter; oscuro ciruela aterciopelado.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#F8F5F9` | `#150F1B` |
| fg | `#24182E` | `#F0E9F5` |
| mut | `#7A6B85` | `#A596B2` |
| card | `#FFFFFF` | `#1F1727` |
| line | `#EBE3EF` | `#30253B` |
| hero / heroFg | `#3B2A4F` / `#DCD2FA` | `#33244A` / `#DCD2FA` |
| acc / accFg | `#9C8CD9` / `#FFFFFF` | `#BBAEF7` / `#150F1B` |
| inc | `#2F8F7A` | `#7DD8C0` |
| exp | `#C2416B` | `#F28BAA` |

### J3 — Lavanda & Durazno
Tarjeta lavanda protagonista, detalles durazno, botones negros. Alegre.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#FAF7F5` | `#14121A` |
| fg | `#1E1A2A` | `#EEEAF5` |
| mut | `#7A7488` | `#A09AAF` |
| card | `#FFFFFF` | `#1D1A25` |
| line | `#EDE6E3` | `#2C2836` |
| hero / heroFg | `#BDB1F2` / `#1E1A2A` | `#2B2540` / `#FFD7C2` |
| acc / accFg | `#1E1A2A` / `#FFD7C2` | `#BDB1F2` / `#14121A` |
| inc | `#2F8F7A` | `#7DD8C0` |
| exp | `#D0506A` | `#F59AA8` |

### J4 — Lavanda & Grafito
Grafito cálido y lavanda como único color. El más minimalista.

| Token | Claro | Oscuro |
|---|---|---|
| bg | `#F7F6F8` | `#161518` |
| fg | `#1F1E22` | `#EDECF0` |
| mut | `#74727A` | `#9E9CA6` |
| card | `#FFFFFF` | `#1F1E23` |
| line | `#E7E5EA` | `#2E2C33` |
| hero / heroFg | `#2B2A2F` / `#D4CCF6` | `#26232F` / `#C8BEF5` |
| acc / accFg | `#9C8CD9` / `#FFFFFF` | `#B4A7F5` / `#161518` |
| inc | `#2F8F7A` | `#7DD8C0` |
| exp | `#C2416B` | `#F28BAA` |
