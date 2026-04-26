export type TransactionType = "income" | "expense";

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
  period: "monthly" | "yearly";
}
