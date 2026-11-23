-- Map the cloud `account_type` enum to the account kinds Moni uses
-- (yape, debit, savings, cash, credit_card, other). See ADR-0007.
--
-- Renames keep the enum OIDs, so existing defaults follow automatically
-- (accounts.type default 'bank' becomes 'savings'). There were 0 accounts when
-- this ran on the cloud, so no row changes meaning.
--   bank       -> savings  (cuenta de ahorros, the usual Peruvian bank account)
--   debit_card -> debit    (cuenta con tarjeta de débito)
--   + yape                 (Yape wallet)
--   wallet, investment, cash, credit_card, other are kept.
--
-- Kept in its own migration: a value added with ADD VALUE cannot be used in the
-- same transaction.
alter type public.account_type rename value 'bank' to 'savings';
alter type public.account_type rename value 'debit_card' to 'debit';
alter type public.account_type add value if not exists 'yape' before 'cash';
