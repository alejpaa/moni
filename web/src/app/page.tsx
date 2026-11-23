import { redirect } from "next/navigation";
import { summarize, toFinanceTransaction, type TransactionRow } from "@moni/shared";
import { createClient } from "@/lib/supabase/server";
import Dashboard from "./dashboard";

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
    <Dashboard
      email={user.email ?? ""}
      transactions={transactions}
      summary={summary}
      loadError={error ? "No se pudieron cargar los movimientos." : null}
    />
  );
}
