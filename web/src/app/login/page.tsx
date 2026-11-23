import { signIn, signUp } from "./actions";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const { error } = await searchParams;

  return (
    <main className="flex flex-1 items-center justify-center p-4">
      <form className="flex w-full max-w-sm flex-col gap-4 rounded-2xl border border-border bg-surface p-6 shadow-sm">
        <div>
          <p className="text-sm font-medium uppercase tracking-wide text-accent">Moni</p>
          <h1 className="text-2xl font-semibold tracking-tight">Inicia sesión</h1>
        </div>
        <label className="flex flex-col gap-1 text-sm">
          Email
          <input name="email" type="email" required autoComplete="email" className="input" />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          Contraseña
          <input
            name="password"
            type="password"
            required
            minLength={6}
            autoComplete="current-password"
            className="input"
          />
        </label>
        {error && <p className="text-sm text-expense">{error}</p>}
        <button formAction={signIn} className="btn-primary">
          Entrar
        </button>
        <button formAction={signUp} className="btn-secondary">
          Crear cuenta
        </button>
      </form>
    </main>
  );
}
