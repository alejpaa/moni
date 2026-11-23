export type TransactionType = "income" | "expense";
export type GoalPeriod = "monthly" | "yearly";

export interface FinanceTransaction {
  id: string;
  userId: string;
  type: TransactionType;
  amount: number;
  category: string;
  occurredAt: string;
  note?: string;
}

export interface SavingGoal {
  id: string;
  userId: string;
  name: string;
  targetAmount: number;
  currentAmount: number;
  period: GoalPeriod;
  deadline?: string;
}

/** Row shape of `public.transactions` as returned by Supabase (snake_case). */
export interface TransactionRow {
  id: string;
  user_id: string;
  type: TransactionType;
  // numeric columns arrive as strings or numbers depending on the client
  amount: number | string;
  category: string;
  occurred_at: string;
  note: string | null;
}

export function toFinanceTransaction(row: TransactionRow): FinanceTransaction {
  return {
    id: row.id,
    userId: row.user_id,
    type: row.type,
    amount: Number(row.amount),
    category: row.category,
    occurredAt: row.occurred_at,
    note: row.note ?? undefined,
  };
}

export interface Summary {
  income: number;
  expense: number;
  balance: number;
}

export function summarize(transactions: FinanceTransaction[]): Summary {
  let income = 0;
  let expense = 0;
  for (const t of transactions) {
    if (t.type === "income") income += t.amount;
    else expense += t.amount;
  }
  return { income, expense, balance: income - expense };
}
