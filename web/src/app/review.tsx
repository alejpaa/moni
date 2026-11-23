"use client";

import { useId, useState, useTransition } from "react";
import { Check, ChevronDown, EyeOff, Inbox, Loader2 } from "lucide-react";
import type { Account, Category, FinanceTransaction } from "@moni/shared";
import { confirmTransaction, ignoreTransaction, type ReviewResult } from "./actions";
import CategoryIcon from "./category-icon";

const money = new Intl.NumberFormat("es", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** "Por revisar": pending captured movements, collapsed behind a badge button. */
export default function Review({
  pending,
  categories,
  accounts,
}: {
  pending: FinanceTransaction[];
  categories: Category[];
  accounts: Account[];
}) {
  const [open, setOpen] = useState(false);
  const panelId = useId();
  if (pending.length === 0) return null;

  return (
    <section aria-label="Por revisar" className="rounded-card bg-card shadow-card">
      <button
        type="button"
        aria-expanded={open}
        aria-controls={panelId}
        onClick={() => setOpen((o) => !o)}
        className="press flex min-h-12 w-full items-center gap-3 rounded-card px-4 text-left"
      >
        <span className="flex size-9 items-center justify-center rounded-[10px] bg-acc-tile text-acc-ink">
          <Inbox size={18} aria-hidden />
        </span>
        <span className="flex-1 font-semibold">Por revisar</span>
        <span className="rounded-full bg-acc px-2.5 py-0.5 text-sm font-semibold text-acc-fg">
          {pending.length}
          <span className="sr-only"> pendientes</span>
        </span>
        <ChevronDown size={18} aria-hidden className={`text-mut transition-transform ${open ? "rotate-180" : ""}`} />
      </button>
      {open && (
        <ul id={panelId}>
          {pending.map((t) => (
            <ReviewItem key={t.id} t={t} categories={categories} accounts={accounts} />
          ))}
        </ul>
      )}
    </section>
  );
}

function ReviewItem({
  t,
  categories,
  accounts,
}: {
  t: FinanceTransaction;
  categories: Category[];
  accounts: Account[];
}) {
  const uid = useId();
  const [busy, start] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const cats = categories.filter((c) => c.kind === t.type);
  const income = t.type === "income";
  const title = t.merchant ?? t.note ?? t.category;

  const run = (fn: (fd: FormData) => Promise<ReviewResult>) => (fd: FormData) =>
    start(async () => {
      const r = await fn(fd);
      setError(r.ok ? null : (r.error ?? "Algo salió mal."));
    });

  return (
    <li className="border-t border-divider px-4 py-3">
      <form className="flex flex-col gap-2.5">
        <input type="hidden" name="id" value={t.id} />
        <div className="flex items-center gap-3">
          <CategoryIcon category={t.category} icon={t.categoryIcon} type={t.type} />
          <p className="min-w-0 flex-1 truncate font-semibold">{title}</p>
          <span className={`shrink-0 font-semibold ${income ? "text-inc" : "text-exp"}`}>
            {income ? "+" : "−"}
            {money.format(t.amount)}
          </span>
        </div>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <label htmlFor={`${uid}-c`} className="sr-only">
              Categoría
            </label>
            <div className="field">
              <select id={`${uid}-c`} name="category_id" defaultValue={t.categoryId ?? ""}>
                <option value="" disabled>
                  Categoría…
                </option>
                {cats.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div>
            <label htmlFor={`${uid}-a`} className="sr-only">
              Cuenta
            </label>
            <div className="field">
              <select id={`${uid}-a`} name="account_id" defaultValue={t.accountId ?? accounts[0]?.id ?? ""}>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
        {error && (
          <p role="alert" className="text-sm font-medium text-exp">
            {error}
          </p>
        )}
        <div className="flex gap-2">
          <button
            formAction={run(confirmTransaction)}
            disabled={busy}
            className="press flex min-h-11 flex-1 items-center justify-center gap-1.5 rounded-full bg-acc text-sm font-semibold text-acc-fg disabled:opacity-60"
          >
            {busy ? <Loader2 size={16} aria-hidden className="anim-spin" /> : <Check size={16} aria-hidden />}
            Confirmar
          </button>
          <button
            formAction={run(ignoreTransaction)}
            disabled={busy}
            className="press flex min-h-11 flex-1 items-center justify-center gap-1.5 rounded-full bg-bg text-sm font-semibold ring-1 ring-line disabled:opacity-60"
          >
            <EyeOff size={16} aria-hidden />
            Ignorar
          </button>
        </div>
      </form>
    </li>
  );
}
