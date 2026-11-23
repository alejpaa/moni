"use server";

import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";

export type AuthState = {
  error?: string;
  fieldErrors?: { email?: string; password?: string };
  /** Echoed back so inputs keep their value after a failed submit. */
  email?: string;
};

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function translateError(message: string, code?: string): string {
  const m = message.toLowerCase();
  if (code === "invalid_credentials" || m.includes("invalid login credentials"))
    return "Email o contraseña incorrectos";
  if (code === "user_already_exists" || m.includes("already registered") || m.includes("already exists"))
    return "Ya existe una cuenta con este email";
  if (code === "email_not_confirmed" || m.includes("email not confirmed"))
    return "Confirma tu email antes de entrar";
  if (code === "weak_password" || m.includes("password should be"))
    return "La contraseña es demasiado débil. Usa al menos 6 caracteres";
  if (code === "over_request_rate_limit" || code === "over_email_send_rate_limit" || m.includes("rate limit"))
    return "Demasiados intentos. Espera un momento e inténtalo de nuevo";
  if (code === "email_address_invalid" || m.includes("invalid email")) return "El email no es válido";
  if (m.includes("fetch failed") || m.includes("network"))
    return "No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo";
  return "Algo salió mal. Inténtalo de nuevo";
}

async function authenticate(mode: "signin" | "signup", formData: FormData): Promise<AuthState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");

  const fieldErrors: NonNullable<AuthState["fieldErrors"]> = {};
  if (!email) fieldErrors.email = "Escribe tu email";
  else if (!EMAIL_RE.test(email)) fieldErrors.email = "Introduce un email válido";
  if (!password) fieldErrors.password = "Escribe tu contraseña";
  else if (password.length < 6) fieldErrors.password = "La contraseña debe tener al menos 6 caracteres";
  if (fieldErrors.email || fieldErrors.password) return { fieldErrors, email };

  const supabase = await createClient();
  try {
    const { error } =
      mode === "signin"
        ? await supabase.auth.signInWithPassword({ email, password })
        : await supabase.auth.signUp({ email, password });
    if (error) return { error: translateError(error.message, error.code), email };
  } catch (e) {
    return { error: translateError(e instanceof Error ? e.message : ""), email };
  }
  redirect("/");
}

export async function signIn(_prev: AuthState, formData: FormData) {
  return authenticate("signin", formData);
}

export async function signUp(_prev: AuthState, formData: FormData) {
  return authenticate("signup", formData);
}
