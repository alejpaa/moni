import { redirect } from "next/navigation";
import { summarize, toFinanceTransaction, type Account, type Category } from "@moni/shared";
import { createClient } from "@/lib/supabase/server";
import Dashboard from "./dashboard";

const COLS = "id, user_id, type, amount, occurred_at, note, category_id, account_id, status, source, merchant";

export default async function Home() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) redirect("/login");

  const [confirmed, pending, cats, accs] = await Promise.all([
    supabase
      .from("transactions")
      .select(COLS)
      .eq("status", "confirmed")
      .order("occurred_at", { ascending: false })
      .limit(100),
    supabase
      .from("transactions")
      .select(COLS)
      .eq("status", "pending")
      .order("occurred_at", { ascending: false })
      .limit(50),
    supabase.from("categories").select("id, name, kind, icon, archived").order("name"),
    supabase
      .from("accounts")
      .select("id, name, type, archived")
      .eq("archived", false)
      .order("created_at"),
  ]);

  const allCategories = cats.data ?? [];
  // Archived categories still label old rows but are not offered in pickers.
  const categories: Category[] = allCategories.filter((c) => !c.archived);
  const accounts: Account[] = accs.data ?? [];
  const byId = new Map(allCategories.map((c) => [c.id, c]));
  const convert = (rows: typeof confirmed.data) =>
    (rows ?? []).map((r) => toFinanceTransaction(r, r.category_id ? byId.get(r.category_id) : undefined));

  const transactions = convert(confirmed.data);
  const pendingItems = convert(pending.data);
  const failed = confirmed.error || cats.error || accs.error;

  return (
    <Dashboard
      email={user.email ?? ""}
      transactions={transactions}
      pending={pendingItems}
      categories={categories}
      accounts={accounts}
      summary={summarize(transactions)}
      loadError={failed ? "No se pudieron cargar tus datos. Recarga la página." : null}
    />
  );
}
