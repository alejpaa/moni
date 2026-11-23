# Plan: captura automática y finanzas centralizadas

Decisión de fondo: [ADR-0006](adr/0006-automatic-transaction-capture.md). Estado: **propuesto** (2026-11-22).

Objetivo: que Yape, Plin, Google Pay, tarjetas de débito y crédito y pagos de tarjeta se registren solos, en cuentas separadas pero con una vista central, sin contar nada dos veces.

## Principios

- **Todo lo automático entra como "por revisar"**. El usuario confirma o corrige con un toque, y Moni aprende la categoría por comercio.
- **Nunca contar dos veces**: el mismo pago puede llegar por Wallet, por la app del banco y por correo. Se agrupa en un solo movimiento.
- **Privacidad**: solo se leen las apps de una lista permitida; nunca la bandeja de correo entera.
- **Falla segura**: si un texto no se entiende, se guarda como "no reconocido" en la bandeja, no se pierde.

## Fase 0: Muestras reales (1 sesión)

Con el teléfono por adb y con permiso del usuario, capturar notificaciones reales de Yape (recibido y enviado), Plin, Google Wallet, la app del banco y Gmail (alerta del BCP) mediante `dumpsys notification`, filtrado solo a esos paquetes.

- Resultado: nombres de paquete exactos y textos de ejemplo, anonimizados y guardados como fixtures de prueba en `android/app/src/test/resources/notifications/`.
- Confirmar si los avisos de Wallet y de Gmail incluyen el monto.

## Fase 1: Modelo de datos (base para todo)

**Hecho en la base de datos** (migración `20261123120000_merge_planning_and_capture`, ver [database.md](database.md) y [ADR-0007](adr/0007-merged-data-model.md)). Los datos son del **espacio** (`workspace_id`), no del usuario.

| Tabla / campo | Para qué |
|---|---|
| `accounts` (`name`, `type`: yape · cash · savings · debit · credit_card · wallet · investment · other, `institution`, `last4`, `currency`) | Cada movimiento pertenece a una cuenta |
| `accounts.credit_limit`, `closing_day`, `due_day` (solo tarjetas) + vista `account_balances` (`card_debt`, `available_credit`) | Deuda, fecha de cierre y de pago |
| `transactions` + `account_id`, `category_id`, `merchant`, `source` (manual · email · notification · statement · installment · ai), `status` (confirmed · pending · ignored), `external_ref`, `raw_input`, `ai_confidence` | Origen, bandeja de revisión y trazabilidad |
| `transfers` (`from_account_id`, `to_account_id`, `amount`, `occurred_at`) | Pago de tarjeta, BCP → Yape, etc. No es gasto ni ingreso |
| `merchant_rules` (`pattern`, `category_id`, `account_id`, `priority`) | Categoría automática por comercio (se aplica al insertar) |
| `installment_plans` + vista `installment_plan_status` | Compras en cuotas: solo la cuota del mes cuenta como gasto |
| `reminders` (`frequency`, `remind_days_before`) | Suscripciones y pagos fijos |
| `ingest_addresses` (`token`) e `inbound_emails` | Dirección secreta de reenvío y registro de correos (Fase 3) |
| Índice único (`workspace_id`, `source`, `external_ref`) + regla de similitud (misma cuenta, mismo monto, ±10 min, en la app) | Deduplicación |

Todo con RLS. Tipos generados en `packages/shared/src/database.types.ts`; falta poner al día los modelos Kotlin y la UI.

UI (web y Android):
- Pantalla de **Cuentas** con saldo por cuenta y total.
- **Tarjetas**: deuda actual, disponible, días hasta el pago.
- **Registrar pago de tarjeta** como transferencia.
- Selector de cuenta al añadir un movimiento.
- **Bandeja "Por revisar"** con un contador en el inicio.

## Fase 2: Lector de notificaciones (Android)

- `NotificationListenerService` con una lista permitida de paquetes de Fase 0. Se ignora todo lo demás.
- Un parser por fuente (Yape, Plin, Wallet, banco, Gmail→BCP): regex sobre el texto. Devuelve monto, moneda, sentido, comercio o persona, últimos 4 dígitos y hora.
- Se asigna la cuenta por fuente o por `last4`. Las reglas de comercio aplican la categoría.
- Cola local si no hay red, que se sincroniza a Supabase después.
- Pantalla de activación que explica qué se lee y por qué, con un botón a los ajustes de "acceso a notificaciones" y un indicador de que el lector está activo.
- Tests de cada parser con las fixtures de Fase 0.

## Fase 3: Correos reenviados (solo si la Fase 2 deja huecos)

- Filtro en Gmail: los remitentes de alertas del banco se reenvían a `<código>@<dominio de Moni>`.
- Cloudflare Email Routing → Worker → Supabase Edge Function (con secreto compartido) → mismo parser y misma bandeja.
- Requiere un dominio (unos US$10 al año). El resto entra en los planes gratuitos.

## Fase 4: Cuadre mensual con estados de cuenta

- Subir el PDF del estado de cuenta (o compartir una captura de un comprobante). Un LLM extrae las filas y Moni las empareja con lo ya registrado.
- Muestra lo que falta, lo que sobra y las diferencias de monto (por ejemplo, propinas o preautorizaciones).

## Fase 5: Finanzas abiertas

Cuando la regulación de la SBS esté vigente (los bancos grandes deben sumarse a más tardar el 2028-01-01), evaluar la conexión directa con el banco usando el mismo modelo de cuentas.

## Costos estimados

| Pieza | Costo |
|---|---|
| Notificaciones (Fase 2), incluido Gmail | Gratis, todo en el teléfono |
| Supabase (Edge Functions, base de datos) | Plan gratuito a volumen personal |
| Correos reenviados (Fase 3) | Dominio ≈ US$10/año |
| Lectura de PDFs con IA (Fase 4) | Centavos al mes |

## Riesgos

- Yape o los bancos cambian el texto de sus avisos: los tests de parser lo detectan y lo no reconocido va a la bandeja.
- Notificaciones que no llegan (ahorro de batería de Xiaomi/HyperOS): hay que pedir que Moni quede excluida de la optimización de batería y tenga inicio automático.
- Solo Android captura; la web muestra y edita.
