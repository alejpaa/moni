"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";

export type AddState = {
  ok: boolean;
  /** Increments on every successful add so the client can react once. */
  nonce: number;
  error?: string;
  fieldErrors?: { amount?: string; category?: string; account?: string };
};

type PgError = { code?: string; message?: string };

/** Maps a PostgREST/Postgres error to a friendly Spanish message. */
function friendly(error: PgError, fallback: string): string {
  switch (error.code) {
    case "42501":
      return "No tienes permiso para hacer esto en este espacio.";
    case "23503":
      return "La categoría o la cuenta elegida ya no existe. Recarga la página.";
    case "23505":
      return "Este movimiento ya estaba registrado.";
    case "23514":
      return "Algún dato no es válido. Revisa el formulario.";
    case "PGRST301":
    case "PGRST303":
      return "Tu sesión expiró. Vuelve a iniciar sesión.";
    default:
      return error.message?.includes("fetch") ? "Sin conexión con el servidor. Inténtalo de nuevo." : fallback;
  }
}

async function authed() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) redirect("/login");
  return supabase;
}

export async function addTransaction(prev: AddState, formData: FormData): Promise<AddState> {
  const supabase = await authed();

  const type = formData.get("type") === "income" ? "income" : "expense";
  const amount = Number(String(formData.get("amount") ?? "").replace(",", "."));
  const categoryId = String(formData.get("category_id") ?? "");
  const accountId = String(formData.get("account_id") ?? "");
  const note = String(formData.get("note") ?? "").trim() || null;

  const fieldErrors: NonNullable<AddState["fieldErrors"]> = {};
  if (!(amount > 0) || !Number.isFinite(amount)) fieldErrors.amount = "Introduce un monto mayor que 0";
  if (!categoryId) fieldErrors.category = "Elige una categoría";
  if (!accountId) fieldErrors.account = "Elige una cuenta";
  if (Object.keys(fieldErrors).length) return { ok: false, nonce: prev.nonce, fieldErrors };

  // user_id and workspace_id default to the caller; currency defaults to PEN.
  const { error } = await supabase.from("transactions").insert({
    type,
    amount,
    category_id: categoryId,
    account_id: accountId,
    note,
    source: "manual",
    status: "confirmed",
  });
  if (error) {
    return { ok: false, nonce: prev.nonce, error: friendly(error, "No se pudo guardar el movimiento. Inténtalo de nuevo.") };
  }
  revalidatePath("/");
  return { ok: true, nonce: prev.nonce + 1 };
}

export async function deleteTransaction(formData: FormData) {
  const supabase = await authed();
  const { error } = await supabase.from("transactions").delete().eq("id", String(formData.get("id")));
  if (error) throw new Error(friendly(error, "No se pudo eliminar el movimiento."));
  revalidatePath("/");
}

export type ReviewResult = { ok: boolean; error?: string };

/** Confirms a pending transaction, optionally changing its category and account. */
export async function confirmTransaction(formData: FormData): Promise<ReviewResult> {
  const supabase = await authed();
  const categoryId = String(formData.get("category_id") ?? "");
  const accountId = String(formData.get("account_id") ?? "");
  if (!categoryId) return { ok: false, error: "Elige una categoría antes de confirmar." };
  const { error } = await supabase
    .from("transactions")
    .update({ status: "confirmed", category_id: categoryId, ...(accountId ? { account_id: accountId } : {}) })
    .eq("id", String(formData.get("id")))
    .eq("status", "pending");
  if (error) return { ok: false, error: friendly(error, "No se pudo confirmar el movimiento.") };
  revalidatePath("/");
  return { ok: true };
}

export async function ignoreTransaction(formData: FormData): Promise<ReviewResult> {
  const supabase = await authed();
  const { error } = await supabase
    .from("transactions")
    .update({ status: "ignored" })
    .eq("id", String(formData.get("id")))
    .eq("status", "pending");
  if (error) return { ok: false, error: friendly(error, "No se pudo ignorar el movimiento.") };
  revalidatePath("/");
  return { ok: true };
}

export async function signOut() {
  const supabase = await createClient();
  await supabase.auth.signOut();
  redirect("/login");
}
