import { redirect } from "next/navigation";
import { summarize, toFinanceTransaction, type TransactionRow } from "@moni/shared";
import { createClient } from "@/lib/supabase/server";
import { addTransaction, deleteTransaction, signOut } from "./actions";

const money = new Intl.NumberFormat("es", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const date = new Intl.DateTimeFormat("es", { dateStyle: "medium" });

export default async function Home() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) redirect("/login");

  const { data, error } = await supabase
    .from("transactions")
    .select("id, user_id, type, amount, category, occurred_at, note")
    .order("occurred_at", { ascending: false })
    .limit(100);

  const transactions = ((data ?? []) as TransactionRow[]).map(toFinanceTransaction);
  const summary = summarize(transactions);

  return (
    <main className="mx-auto flex w-full max-w-3xl flex-col gap-6 p-4 sm:p-8">
      <header className="flex items-center justify-between gap-4">
        <div>
          <p className="text-sm font-medium uppercase tracking-wide text-accent">Moni</p>
          <h1 className="text-2xl font-semibold tracking-tight">Tus finanzas</h1>
        </div>
        <form action={signOut} className="flex items-center gap-3 text-sm">
          <span className="hidden text-muted sm:inline">{user.email}</span>
          <button className="btn-secondary">Salir</button>
        </form>
      </header>

      <section className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Stat label="Balance" value={summary.balance} />
        <Stat label="Ingresos" value={summary.income} tone="income" />
        <Stat label="Gastos" value={summary.expense} tone="expense" />
      </section>

      <form
        action={addTransaction}
        className="grid grid-cols-2 gap-3 rounded-2xl border border-border bg-surface p-4 sm:grid-cols-[auto_1fr_1fr_1fr_auto]"
      >
        <select name="type" className="input" aria-label="Tipo">
          <option value="expense">Gasto</option>
          <option value="income">Ingreso</option>
        </select>
        <input name="amount" type="number" step="0.01" min="0.01" required placeholder="Monto" className="input" />
        <input name="category" required placeholder="Categoría" className="input" />
        <input name="note" placeholder="Nota (opcional)" className="input" />
        <button className="btn-primary col-span-2 sm:col-span-1">Añadir</button>
      </form>

      <section className="rounded-2xl border border-border bg-surface">
        {error && <p className="p-4 text-sm text-expense">{error.message}</p>}
        {!error && transactions.length === 0 && (
          <p className="p-6 text-center text-sm text-muted">
            Aún no hay movimientos. Añade el primero arriba.
          </p>
        )}
        <ul className="divide-y divide-border">
          {transactions.map((t) => (
            <li key={t.id} className="flex items-center justify-between gap-3 p-4">
              <div className="min-w-0">
                <p className="truncate font-medium">{t.category}</p>
                <p className="truncate text-sm text-muted">
                  {date.format(new Date(t.occurredAt))}
                  {t.note ? ` · ${t.note}` : ""}
                </p>
              </div>
              <div className="flex shrink-0 items-center gap-3">
                <span className={`font-mono tabular-nums ${t.type === "income" ? "text-income" : "text-expense"}`}>
                  {t.type === "income" ? "+" : "−"}
                  {money.format(t.amount)}
                </span>
                <form action={deleteTransaction}>
                  <input type="hidden" name="id" value={t.id} />
                  <button className="text-sm text-muted hover:text-expense" aria-label="Eliminar">
                    ✕
                  </button>
                </form>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </main>
  );
}

function Stat({ label, value, tone }: { label: string; value: number; tone?: "income" | "expense" }) {
  const color = tone === "income" ? "text-income" : tone === "expense" ? "text-expense" : "";
  return (
    <div className="rounded-2xl border border-border bg-surface p-4">
      <p className="text-sm text-muted">{label}</p>
      <p className={`mt-1 font-mono text-2xl font-semibold tabular-nums ${color}`}>{money.format(value)}</p>
    </div>
  );
}
