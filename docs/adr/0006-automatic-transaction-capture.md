# ADR-0006: Automatic transaction capture (notifications first, then forwarded emails)

## Status
Proposed

## Date
2026-11-22

## Context
The user wants Moni to record movements automatically: Yape/Plin transfers, Google Pay (Billetera de Google) tap payments, debit and credit card purchases, and credit card payments, all centralized across accounts. Research (2026-11):

- Yape and Google Wallet expose no API for individuals to read their own history. Yape's API is for merchants collecting payments; Wallet's is for issuers and passes.
- Peru open finance is still a draft. SBS published the first rule block in Oct 2026. BCP, BBVA, Interbank and Scotiabank must join by 2028-01-01, starting with savings accounts and credit cards. Aggregators such as Prometeo are enterprise-only with non-public pricing.
- Several apps already capture transactions by reading payment notifications on Android: NotiPago and Yapay for Yape in Peru; Bucktrack, Masareef and ExpenseCC elsewhere.
- Google Wallet posts a "purchase" notification after each tap, with merchant, amount and card. It is silent by default. It is posted by Google Play services or the Wallet app depending on the version.
- BCP emails an alert for every card operation, and Yape can email each yapeo the user sends.

## Decision
Capture in layers, all landing in a **review inbox** (`status = pending`) and deduplicated:

1. **Android notifications** (primary) via `NotificationListenerService`, restricted to an allowlist of packages: Yape, Plin and bank apps, Google Wallet / Play services, and Gmail. The Gmail notification covers bank alert emails at zero cost, using only on-device processing.
2. **Forwarded bank emails** (secondary, if layer 1 misses card purchases). A Gmail filter forwards *only* bank alert emails to a Moni inbound address. Cloudflare Email Routing → Worker → Supabase Edge Function receive and parse them. Moni never gets inbox-wide access.
3. **Monthly statements and screenshots** (reconciliation). The user uploads a statement PDF or shares a voucher screenshot; an LLM extracts the rows and Moni matches them against existing movements.
4. **Open finance** once SBS rules are in force (≈2028).

The data model is extended first (accounts, credit cards, transfers, source and dedupe fields, merchant rules) so every layer writes into the same structure.

## Alternatives Considered
- **Gmail API with a read scope**: reads the whole mailbox. In testing mode refresh tokens expire every 7 days. Rejected for privacy and maintenance reasons.
- **Reading SMS**: restricted by Google Play policy, and Peruvian banks favour push and email over SMS. Possible for sideloaded personal use, but deprioritized.
- **Accessibility service (screen reading)**: fragile, invasive and restricted by Google. Rejected.
- **Paid aggregator (Prometeo) now**: enterprise-oriented and not justified for a personal app before open finance is regulated.
- **Manual only**: does not meet the goal.

## Consequences
- Capture works only on Android. Web shows and edits the captured data.
- Parsers depend on the text of third-party notifications and emails. Each parser has fixture tests built from real samples and fails safe: anything unparsed goes to the inbox as "no reconocido" rather than being dropped.
- The notification permission is sensitive. Moni filters by package allowlist, stores only parsed fields plus the raw text needed for debugging, and explains it in onboarding.
- Publishing on Google Play would require a policy declaration for notification access. Personal sideloaded use does not.
- Layer 2 needs a domain (about US$10/year); everything else fits free tiers at personal volume.
