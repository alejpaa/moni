// Generated from the Supabase schema: `supabase gen types typescript --project-id xyswhlasctqtqxefndkr > packages/shared/src/database.types.ts`.
// Regenerate after every migration. See docs/database.md.
export type {
  Database,
  Json,
  Tables,
  TablesInsert,
  TablesUpdate,
  Enums,
  CompositeTypes,
} from "./database.types";
export { Constants } from "./database.types";
import type { Enums, Tables } from "./database.types";

export type TransactionType = Enums<"transaction_type">;
export type ReviewStatus = Enums<"review_status">;
export type GoalPeriod = "monthly" | "yearly";

export type Category = Pick<Tables<"categories">, "id" | "name" | "kind" | "icon">;
export type Account = Pick<Tables<"accounts">, "id" | "name" | "type" | "archived">;

/** Columns of `public.transactions` the web client selects. */
export type TransactionRow = Pick<
  Tables<"transactions">,
  | "id"
  | "user_id"
  | "type"
  | "amount"
  | "occurred_at"
  | "note"
  | "category_id"
  | "account_id"
  | "status"
  | "source"
  | "merchant"
>;

export interface FinanceTransaction {
  id: string;
  userId: string;
  type: TransactionType;
  amount: number;
  /** Category name ("Sin categoría" when none). */
  category: string;
  categoryId: string | null;
  /** Stored Lucide key of the category, if any. */
  categoryIcon: string | null;
  accountId: string | null;
  status: ReviewStatus;
  source: Tables<"transactions">["source"];
  merchant?: string;
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

export function toFinanceTransaction(
  row: TransactionRow,
  category?: Pick<Category, "name" | "icon">,
): FinanceTransaction {
  return {
    id: row.id,
    userId: row.user_id,
    type: row.type,
    amount: Number(row.amount),
    category: category?.name ?? "Sin categoría",
    categoryId: row.category_id,
    categoryIcon: category?.icon ?? null,
    accountId: row.account_id,
    status: row.status,
    source: row.source,
    merchant: row.merchant ?? undefined,
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
