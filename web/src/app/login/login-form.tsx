"use client";

import { useActionState, useEffect, useId, useRef, useState } from "react";
import { CircleAlert, Eye, EyeOff, Loader2, Lock, Mail } from "lucide-react";
import { signIn, signUp, type AuthState } from "./actions";
import { MoniLockup, MoniMascot } from "../brand";

type Mode = "signin" | "signup";
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function checkEmail(v: string) {
  const t = v.trim();
  if (!t) return "Escribe tu email";
  if (!EMAIL_RE.test(t)) return "Introduce un email válido";
}
function checkPassword(v: string) {
  if (!v) return "Escribe tu contraseña";
  if (v.length < 6) return "La contraseña debe tener al menos 6 caracteres";
}

const COPY = {
  signin: { title: "Inicia sesión", submit: "Entrar", pending: "Entrando…" },
  signup: { title: "Crea tu cuenta", submit: "Crear cuenta", pending: "Creando cuenta…" },
} as const;

export default function LoginForm() {
  const [mode, setMode] = useState<Mode>("signin");
  const [pending, setPending] = useState(false);

  return (
    <div className="flex w-full max-w-[400px] flex-col items-center">
    <MoniMascot size={104} className="relative z-10 -mb-3" />
    <div className="anim-rise w-full rounded-card bg-card p-6 shadow-card sm:p-8">
      <div className="mb-6">
        <MoniLockup />
        <h1 className="mt-1 text-[22px] font-bold">{COPY[mode].title}</h1>
      </div>

      <Segmented mode={mode} onChange={setMode} disabled={pending} />

      <AuthFields key={mode} mode={mode} onPending={setPending} />
    </div>
    </div>
  );
}

function Segmented({
  mode,
  onChange,
  disabled,
}: {
  mode: Mode;
  onChange: (m: Mode) => void;
  disabled: boolean;
}) {
  const options: { value: Mode; label: string }[] = [
    { value: "signin", label: "Entrar" },
    { value: "signup", label: "Crear cuenta" },
  ];
  return (
    <div
      role="radiogroup"
      aria-label="Modo de acceso"
      className="relative mb-5 grid grid-cols-2 rounded-full bg-bg p-1 ring-1 ring-line"
    >
      <span
        aria-hidden
        className="absolute inset-y-1 left-1 w-[calc(50%-4px)] rounded-full bg-card shadow-chip"
        style={{
          transform: `translateX(${mode === "signin" ? 0 : 100}%)`,
          transition: "transform 400ms cubic-bezier(0.34, 1.56, 0.64, 1)",
        }}
      />
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          role="radio"
          aria-checked={mode === o.value}
          disabled={disabled}
          onClick={() => onChange(o.value)}
          className={`relative z-10 min-h-11 rounded-full text-sm font-semibold transition-colors ${
            mode === o.value ? "text-fg" : "text-mut"
          }`}
        >
          {o.label}
        </button>
      ))}
    </div>
  );
}

function AuthFields({ mode, onPending }: { mode: Mode; onPending: (p: boolean) => void }) {
  const [state, formAction, pending] = useActionState<AuthState, FormData>(
    mode === "signin" ? signIn : signUp,
    {},
  );
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [show, setShow] = useState(false);
  const [touched, setTouched] = useState({ email: false, password: false });
  const [submitted, setSubmitted] = useState(false);
  const emailRef = useRef<HTMLInputElement>(null);
  const passRef = useRef<HTMLInputElement>(null);
  const uid = useId();

  // Lock the segmented control while submitting.
  useEffect(() => {
    onPending(pending);
  }, [pending, onPending]);

  const copy = COPY[mode];
  // Client checks apply once the field was touched or submitted; otherwise show server field errors.
  const emailMsg =
    touched.email || submitted ? checkEmail(email) : state.fieldErrors?.email;
  const passMsg =
    touched.password || submitted ? checkPassword(password) : state.fieldErrors?.password;

  return (
    <form
      action={formAction}
      noValidate
      className="flex flex-col gap-4"
      onSubmit={(e) => {
        setSubmitted(true);
        if (pending) {
          e.preventDefault();
          return;
        }
        if (checkEmail(email)) {
          e.preventDefault();
          emailRef.current?.focus();
        } else if (checkPassword(password)) {
          e.preventDefault();
          passRef.current?.focus();
        }
      }}
    >
      <div>
        <label htmlFor={`${uid}-email`} className="mb-1.5 block text-sm font-medium">
          Email
        </label>
        <div className="field" data-invalid={!!emailMsg}>
          <Mail size={18} aria-hidden className="shrink-0 text-mut" />
          <input
            ref={emailRef}
            id={`${uid}-email`}
            name="email"
            type="email"
            inputMode="email"
            autoComplete="email"
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            placeholder="tu@email.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            onBlur={() => setTouched((t) => ({ ...t, email: true }))}
            aria-invalid={!!emailMsg}
            aria-describedby={emailMsg ? `${uid}-email-err` : undefined}
          />
        </div>
        {emailMsg && <FieldError id={`${uid}-email-err`}>{emailMsg}</FieldError>}
      </div>

      <div>
        <label htmlFor={`${uid}-password`} className="mb-1.5 block text-sm font-medium">
          Contraseña
        </label>
        <div className="field pr-1" data-invalid={!!passMsg}>
          <Lock size={18} aria-hidden className="shrink-0 text-mut" />
          <input
            ref={passRef}
            id={`${uid}-password`}
            name="password"
            type={show ? "text" : "password"}
            autoComplete={mode === "signin" ? "current-password" : "new-password"}
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onBlur={() => setTouched((t) => ({ ...t, password: true }))}
            aria-invalid={!!passMsg}
            aria-describedby={
              [mode === "signup" ? `${uid}-pass-help` : "", passMsg ? `${uid}-pass-err` : ""]
                .filter(Boolean)
                .join(" ") || undefined
            }
          />
          <button
            type="button"
            className="icon-btn"
            aria-label={show ? "Ocultar contraseña" : "Mostrar contraseña"}
            aria-pressed={show}
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => setShow((s) => !s)}
          >
            {show ? <EyeOff size={18} aria-hidden /> : <Eye size={18} aria-hidden />}
          </button>
        </div>
        {mode === "signup" && (
          <p id={`${uid}-pass-help`} className="mt-1.5 text-xs text-mut">
            Mínimo 6 caracteres.
          </p>
        )}
        {passMsg && <FieldError id={`${uid}-pass-err`}>{passMsg}</FieldError>}
      </div>

      {state.error && !pending && (
        <div
          role="alert"
          className="anim-error flex items-start gap-2 rounded-input bg-exp/10 px-3.5 py-3 text-sm font-medium text-exp"
        >
          <CircleAlert size={18} aria-hidden className="mt-0.5 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}

      <button type="submit" className="btn-primary" disabled={pending} aria-disabled={pending}>
        {pending ? (
          <>
            <Loader2 size={18} aria-hidden className="anim-spin" />
            {copy.pending}
          </>
        ) : (
          copy.submit
        )}
      </button>
    </form>
  );
}

function FieldError({ id, children }: { id: string; children: React.ReactNode }) {
  return (
    <p id={id} className="anim-error mt-1.5 flex items-center gap-1.5 text-sm font-medium text-exp">
      <CircleAlert size={16} aria-hidden className="shrink-0" />
      {children}
    </p>
  );
}
