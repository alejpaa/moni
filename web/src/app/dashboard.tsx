"use client";

import { useState } from "react";
import { useFormStatus } from "react-dom";
import { ArrowDownLeft, ArrowUpRight, Loader2, LogOut, Plus, ReceiptText, Trash2 } from "lucide-react";
import type { FinanceTransaction, Summary } from "@moni/shared";
import { deleteTransaction, signOut } from "./actions";
import AddSheet from "./add-sheet";
import CategoryIcon from "./category-icon";
import CountUp from "./count-up";

type Filter = "all" | "income" | "expense";

const FILTERS: { value: Filter; label: string }[] = [
  { value: "all", label: "Todo" },
  { value: "income", label: "Ingresos" },
  { value: "expense", label: "Gastos" },
];
const EMPTY_COPY: Record<Filter, string> = {
  all: "Aún no hay movimientos. Añade el primero y empieza a ver tus finanzas con claridad.",
  income: "Todavía no has registrado ingresos.",
  expense: "Todavía no has registrado gastos.",
};

const money = new Intl.NumberFormat("es", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const date = new Intl.DateTimeFormat("es", { dateStyle: "medium" });

export default function Dashboard({
  email,
  transactions,
  summary,
  loadError,
}: {
  email: string;
  transactions: FinanceTransaction[];
  summary: Summary;
  loadError: string | null;
}) {
  const [filter, setFilter] = useState<Filter>("all");
  const [adding, setAdding] = useState(false);
  // Ids present on first render: only items added later get the "new item" entrance.
  const [initialIds] = useState(() => new Set(transactions.map((t) => t.id)));

  const visible = filter === "all" ? transactions : transactions.filter((t) => t.type === filter);

  return (
    <main className="mx-auto flex w-full max-w-[720px] flex-col gap-4 px-4 py-6 sm:px-8 sm:py-10">
      <header className="anim-rise flex items-center justify-between gap-4">
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-[0.12em] text-mut">Moni</p>
          <h1 className="text-[22px] font-bold leading-tight">Tus finanzas</h1>
        </div>
        <form action={signOut} className="flex items-center gap-1">
          <span className="hidden max-w-[220px] truncate text-sm text-mut sm:inline">{email}</span>
          <button className="icon-btn" aria-label="Cerrar sesión" title="Cerrar sesión">
            <LogOut size={20} aria-hidden />
          </button>
        </form>
      </header>

      <section
        aria-label="Balance"
        className="anim-hero rounded-hero bg-hero p-5 text-hero-fg shadow-hero sm:p-6"
        style={{ animationDelay: "80ms" }}
      >
        <p className="text-sm opacity-70">Balance</p>
        <p className="mt-1 text-[32px] font-bold leading-tight tracking-[-0.03em]">
          <CountUp value={summary.balance} />
        </p>
        <div className="mt-4 flex gap-6 text-sm">
          <div className="flex items-center gap-1.5">
            <ArrowDownLeft size={18} aria-hidden />
            <span className="sr-only">Ingresos</span>
            <CountUp value={summary.income} className="font-semibold" />
          </div>
          <div className="flex items-center gap-1.5">
            <ArrowUpRight size={18} aria-hidden />
            <span className="sr-only">Gastos</span>
            <CountUp value={summary.expense} className="font-semibold" />
          </div>
        </div>
      </section>

      <div
        className="anim-rise flex gap-2"
        style={{ "--delay": "160ms" } as React.CSSProperties}
        role="group"
        aria-label="Filtrar movimientos"
      >
        {FILTERS.map((f) => (
          <button
            key={f.value}
            type="button"
            aria-pressed={filter === f.value}
            onClick={() => setFilter(f.value)}
            className={`press min-h-11 rounded-full px-4 text-sm font-medium shadow-chip ${
              filter === f.value ? "bg-acc text-acc-fg" : "bg-card text-fg"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      <section
        aria-label="Movimientos"
        className="anim-rise rounded-card bg-card shadow-card"
        style={{ "--delay": "240ms" } as React.CSSProperties}
      >
        {loadError && (
          <p role="alert" className="p-4 text-sm font-medium text-exp">
            {loadError}
          </p>
        )}
        {!loadError && visible.length === 0 && (
          <div className="flex flex-col items-center gap-3 px-6 py-12 text-center">
            <span className="flex size-14 items-center justify-center rounded-[16px] bg-acc-tile text-acc-ink">
              <ReceiptText size={26} aria-hidden />
            </span>
            <p className="max-w-xs text-sm text-mut">{EMPTY_COPY[filter]}</p>
          </div>
        )}
        <ul>
          {visible.map((t, i) => (
            <Row key={t.id} t={t} isNew={!initialIds.has(t.id)} index={i} />
          ))}
        </ul>
      </section>

      <button
        type="button"
        className="btn-primary anim-rise"
        style={{ "--delay": "320ms" } as React.CSSProperties}
        onClick={() => setAdding(true)}
      >
        <Plus size={18} aria-hidden />
        Añadir movimiento
      </button>

      <AddSheet open={adding} onClose={() => setAdding(false)} />
    </main>
  );
}

function Row({ t, isNew, index }: { t: FinanceTransaction; isNew: boolean; index: number }) {
  const [removing, setRemoving] = useState(false);
  const income = t.type === "income";
  const stagger = isNew ? undefined : ({ "--delay": `${Math.min(index, 12) * 70 + 300}ms` } as React.CSSProperties);

  return (
    <li className={isNew ? "anim-new" : "anim-rise"} style={stagger}>
      <div className="row-wrap" data-removing={removing}>
        <div>
          <div className="flex min-h-14 items-center gap-3 border-t border-divider px-4 py-2 first:border-t-0 [li:first-child_&]:border-t-0">
            <CategoryIcon category={t.category} type={t.type} />
            <div className="min-w-0 flex-1">
              <p className="truncate font-semibold">{t.category}</p>
              <p className="truncate text-sm text-mut">
                <span suppressHydrationWarning>{date.format(new Date(t.occurredAt))}</span>
                {t.note ? ` · ${t.note}` : ""}
              </p>
            </div>
            <span className={`shrink-0 font-semibold ${income ? "text-inc" : "text-exp"}`}>
              {income ? "+" : "−"}
              {money.format(t.amount)}
            </span>
            <form
              action={async (fd) => {
                setRemoving(true);
                await new Promise((r) => setTimeout(r, 250));
                try {
                  await deleteTransaction(fd);
                } catch {
                  setRemoving(false);
                }
              }}
            >
              <input type="hidden" name="id" value={t.id} />
              <DeleteButton label={`Eliminar movimiento de ${t.category}`} />
            </form>
          </div>
        </div>
      </div>
    </li>
  );
}


function DeleteButton({ label }: { label: string }) {
  const { pending } = useFormStatus();
  return (
    <button className="icon-btn hover:!text-exp" aria-label={label} title="Eliminar" disabled={pending}>
      {pending ? <Loader2 size={18} aria-hidden className="anim-spin" /> : <Trash2 size={18} aria-hidden />}
    </button>
  );
}
