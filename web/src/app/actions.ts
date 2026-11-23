"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";

export async function addTransaction(formData: FormData) {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) redirect("/login");

  const type = formData.get("type") === "income" ? "income" : "expense";
  const amount = Number(formData.get("amount"));
  const category = String(formData.get("category") ?? "").trim();
  const note = String(formData.get("note") ?? "").trim() || null;
  if (!(amount > 0) || !category) return;

  await supabase
    .from("transactions")
    .insert({ user_id: user.id, type, amount, category, note });
  revalidatePath("/");
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
