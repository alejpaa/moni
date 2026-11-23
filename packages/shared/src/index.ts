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

/** Icon keys (Lucide names, kebab-case). Clients map each key to a native icon. */
export type CategoryIconKey =
  | "briefcase"
  | "utensils"
  | "shopping-cart"
  | "coffee"
  | "bus"
  | "fuel"
  | "house"
  | "zap"
  | "heart-pulse"
  | "clapperboard"
  | "gift"
  | "plane"
  | "graduation-cap"
  | "piggy-bank"
  | "arrow-down-left"
  | "arrow-up-right";

// Order matters: first match wins. Keywords are matched as substrings of the normalized category.
const CATEGORY_ICON_RULES: ReadonlyArray<readonly [CategoryIconKey, readonly string[]]> = [
  ["briefcase", ["sueldo", "salario", "nomina", "pago"]],
  ["utensils", ["comida", "almuerzo", "cena", "restaurante"]],
  ["shopping-cart", ["supermercado", "super", "mercado", "compras"]],
  ["coffee", ["cafe"]],
  ["fuel", ["gasolina"]],
  ["bus", ["transporte", "taxi", "bus", "uber"]],
  ["house", ["casa", "alquiler", "renta", "hogar"]],
  ["zap", ["luz", "agua", "internet", "servicios"]],
  ["heart-pulse", ["salud", "farmacia", "medico"]],
  ["clapperboard", ["ocio", "cine", "juegos"]],
  ["gift", ["regalo"]],
  ["plane", ["viaje", "vuelo"]],
  ["graduation-cap", ["educacion", "curso", "libros"]],
  ["piggy-bank", ["ahorro", "inversion"]],
];

/** Lowercase and strip accents/diacritics. */
export function normalizeCategory(category: string): string {
  return category
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .trim();
}

/**
 * Maps a free-text category to an icon key by keyword; falls back to a default
 * per transaction type (income: arrow-down-left, expense: arrow-up-right).
 */
export function categoryIconKey(category: string, type: TransactionType): CategoryIconKey {
  const text = normalizeCategory(category);
  for (const [key, words] of CATEGORY_ICON_RULES) {
    if (words.some((w) => text.includes(w))) return key;
  }
  return type === "income" ? "arrow-down-left" : "arrow-up-right";
}
