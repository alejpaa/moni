"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";

export type AddState = {
  ok: boolean;
  /** Increments on every successful add so the client can react once. */
  nonce: number;
  error?: string;
  fieldErrors?: { amount?: string; category?: string };
};

export async function addTransaction(prev: AddState, formData: FormData): Promise<AddState> {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) redirect("/login");

  const type = formData.get("type") === "income" ? "income" : "expense";
  const amount = Number(String(formData.get("amount") ?? "").replace(",", "."));
  const category = String(formData.get("category") ?? "").trim();
  const note = String(formData.get("note") ?? "").trim() || null;

  const fieldErrors: NonNullable<AddState["fieldErrors"]> = {};
  if (!(amount > 0) || !Number.isFinite(amount)) fieldErrors.amount = "Introduce un monto mayor que 0";
  if (!category) fieldErrors.category = "Escribe una categoría";
  if (fieldErrors.amount || fieldErrors.category) {
    return { ok: false, nonce: prev.nonce, fieldErrors };
  }

  const { error } = await supabase
    .from("transactions")
    .insert({ user_id: user.id, type, amount, category, note });
  if (error) {
    return { ok: false, nonce: prev.nonce, error: "No se pudo guardar el movimiento. Inténtalo de nuevo." };
  }
  revalidatePath("/");
  return { ok: true, nonce: prev.nonce + 1 };
}

export async function deleteTransaction(formData: FormData) {
  const supabase = await createClient();
  await supabase.from("transactions").delete().eq("id", String(formData.get("id")));
  revalidatePath("/");
}

export async function signOut() {
  const supabase = await createClient();
  await supabase.auth.signOut();
  redirect("/login");
}
