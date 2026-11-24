import Link from "next/link";
import {
  Bell,
  CreditCard,
  EyeOff,
  Fingerprint,
  ListPlus,
  Lock,
  Smartphone,
  Sparkles,
  Target,
  Wallet,
  type LucideIcon,
} from "lucide-react";
import { MoniLockup, MoniMascot } from "../brand";
import Reveal from "./reveal";
import { GoalsPreview, PhonePreview } from "./app-preview";

const NAV = [
  { href: "#funciones", label: "Funciones" },
  { href: "#privacidad", label: "Privacidad" },
  { href: "#descarga", label: "Descarga" },
];

function Eyebrow({ children }: { children: React.ReactNode }) {
  return <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-acc-ink">{children}</p>;
}

function IconTile({ Icon }: { Icon: LucideIcon }) {
  return (
    <span className="flex size-11 items-center justify-center rounded-[14px] bg-acc-tile text-acc-ink">
      <Icon size={22} aria-hidden />
    </span>
  );
}

function Header() {
  return (
    <header className="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-4 py-4 sm:px-8">
      <Link href="/" aria-label="moni, inicio" className="rounded-lg">
        <MoniLockup size={28} />
      </Link>
      <nav aria-label="Principal" className="hidden items-center gap-1 md:flex">
        {NAV.map((n) => (
          <a key={n.href} href={n.href} className="rounded-full px-4 py-2.5 text-sm font-medium text-mut transition-colors hover:bg-acc-tile hover:text-acc-ink">
            {n.label}
          </a>
        ))}
      </nav>
      <Link
        href="/login"
        className="press inline-flex min-h-11 items-center rounded-full bg-card px-5 text-sm font-semibold text-fg shadow-chip hover:bg-acc-tile"
      >
        Iniciar sesión
      </Link>
    </header>
  );
}

function Hero() {
  return (
    <section className="mx-auto grid w-full max-w-6xl items-center gap-12 px-4 pb-16 pt-6 sm:px-8 md:pb-24 lg:grid-cols-[1.1fr_0.9fr] lg:gap-8 lg:pt-12">
      <div className="text-center lg:text-left">
        <div className="flex justify-center lg:justify-start">
          <MoniMascot size={112} />
        </div>
        <h1 className="anim-rise mt-4 text-[38px] font-bold leading-[1.05] tracking-[-0.03em] sm:text-5xl lg:text-[64px]" style={{ "--delay": "80ms" } as React.CSSProperties}>
          Tu plata, <span className="text-acc-ink">clara</span> y en calma.
        </h1>
        <p className="anim-rise mx-auto mt-5 max-w-xl text-lg leading-relaxed text-mut lg:mx-0" style={{ "--delay": "160ms" } as React.CSSProperties}>
          Registra tus movimientos en segundos, ahorra para lo que importa y sabe siempre cuánto tienes disponible. Sin hojas de cálculo ni complicaciones.
        </p>
        <div className="anim-rise mt-8 flex flex-col items-stretch gap-3 sm:flex-row sm:justify-center lg:justify-start" style={{ "--delay": "240ms" } as React.CSSProperties}>
          <Link href="/login" className="btn-primary sm:w-auto sm:min-w-52">
            Empezar ahora
          </Link>
          <a
            href="#funciones"
            className="press inline-flex min-h-12 items-center justify-center rounded-btn px-5 text-base font-semibold text-acc-ink hover:bg-acc-tile"
          >
            Ver cómo funciona
          </a>
        </div>
      </div>
      <div className="lg:justify-self-end">
        <PhonePreview />
      </div>
    </section>
  );
}

function Features() {
  return (
    <section id="funciones" className="scroll-mt-4 bg-card/60 py-16 md:py-24">
      <div className="mx-auto w-full max-w-6xl px-4 sm:px-8">
        <Reveal className="max-w-2xl">
          <Eyebrow>Funciones</Eyebrow>
          <h2 className="mt-2 text-3xl font-bold sm:text-4xl">Todo lo que necesitas, nada que sobre.</h2>
          <p className="mt-3 text-lg text-mut">Moni te ayuda a entender a dónde va tu dinero y a decidir mejor, día a día.</p>
        </Reveal>

        <div className="mt-10 grid gap-4 md:grid-cols-6">
          <Reveal className="rounded-card bg-card p-6 shadow-card md:col-span-3">
            <IconTile Icon={ListPlus} />
            <h3 className="mt-4 text-xl font-bold tracking-[-0.02em]">Registra tus movimientos</h3>
            <p className="mt-2 text-mut">
              Anota ingresos y gastos en segundos, con categoría y nota. Tu historial siempre ordenado y fácil de repasar.
            </p>
          </Reveal>

          <Reveal delay={70} className="rounded-card bg-card p-6 shadow-card md:col-span-3">
            <IconTile Icon={Wallet} />
            <h3 className="mt-4 text-xl font-bold tracking-[-0.02em]">Saldo disponible, sin sorpresas</h3>
            <p className="mt-2 text-mut">
              Ve lo que realmente tienes para gastar: el dinero de tus cuentas menos lo que debes en tus tarjetas.
            </p>
            <div aria-hidden className="mt-4 rounded-[18px] bg-hero p-4 text-hero-fg shadow-hero">
              <div className="text-[11px] opacity-70">Disponible</div>
              <div className="text-[28px] font-bold tracking-[-0.03em]">S/ 4,820.50</div>
            </div>
          </Reveal>

          <Reveal className="rounded-card bg-card p-6 shadow-card md:col-span-4">
            <div className="grid items-center gap-6 sm:grid-cols-2">
              <div>
                <IconTile Icon={Target} />
                <h3 className="mt-4 text-xl font-bold tracking-[-0.02em]">Metas de ahorro</h3>
                <p className="mt-2 text-mut">
                  Ponle nombre y monto a lo que quieres lograr: un viaje, una laptop, tu fondo de emergencia. Mira cómo avanzas.
                </p>
              </div>
              <GoalsPreview />
            </div>
          </Reveal>

          <Reveal delay={70} className="rounded-card bg-card p-6 shadow-card md:col-span-2">
            <IconTile Icon={Bell} />
            <h3 className="mt-4 text-xl font-bold tracking-[-0.02em]">Recordatorios de tarjetas</h3>
            <p className="mt-2 text-mut">Te avisamos antes de la fecha de pago para que nunca pagues de más.</p>
            <div aria-hidden className="mt-4 flex items-center gap-3 rounded-[16px] bg-acc-tint p-3">
              <span className="flex size-9 items-center justify-center rounded-[10px] bg-card text-acc-ink">
                <CreditCard size={18} />
              </span>
              <span className="text-sm">
                <span className="block font-semibold">Tarjeta Visa</span>
                <span className="block text-[12px] text-mut">Vence en 3 días</span>
              </span>
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}

function Privacy() {
  const items = [
    { Icon: EyeOff, title: "Oculta tus montos", text: "Un toque y tus cifras quedan tapadas: úsalo en el bus, en la oficina o frente a quien sea." },
    { Icon: Fingerprint, title: "Bloqueo biométrico", text: "Abre Moni con tu huella o tu rostro. Solo tú ves tus finanzas." },
    { Icon: Lock, title: "Solo tuyo", text: "Tus datos son privados y están protegidos con tu cuenta personal." },
  ];
  return (
    <section id="privacidad" className="scroll-mt-4 mx-auto w-full max-w-6xl px-4 py-16 sm:px-8 md:py-24">
      <div className="grid items-center gap-10 lg:grid-cols-[0.9fr_1.1fr]">
        <Reveal>
          <Eyebrow>Privacidad</Eyebrow>
          <h2 className="mt-2 text-3xl font-bold sm:text-4xl">Tu dinero, tus ojos.</h2>
          <p className="mt-3 text-lg text-mut">Tus finanzas son personales. Moni te da el control para que lo sigan siendo.</p>
          <div aria-hidden className="mt-6 inline-flex items-center gap-3 rounded-hero bg-hero px-5 py-4 text-hero-fg shadow-hero">
            <EyeOff size={20} />
            <span className="text-2xl font-bold tracking-[0.08em]">S/ ••••••</span>
          </div>
        </Reveal>
        <ul className="grid gap-4">
          {items.map(({ Icon, title, text }, i) => (
            <Reveal as="li" key={title} delay={i * 70} className="flex gap-4 rounded-card bg-card p-5 shadow-card">
              <IconTile Icon={Icon} />
              <div>
                <h3 className="text-lg font-bold tracking-[-0.02em]">{title}</h3>
                <p className="mt-1 text-mut">{text}</p>
              </div>
            </Reveal>
          ))}
        </ul>
      </div>
    </section>
  );
}

function Upcoming() {
  return (
    <section className="mx-auto w-full max-w-6xl px-4 pb-16 sm:px-8 md:pb-24">
      <Reveal className="relative overflow-hidden rounded-hero bg-hero p-8 text-hero-fg shadow-hero sm:p-12">
        <div className="max-w-xl">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-acc px-3 py-1 text-xs font-semibold text-acc-fg">
            <Sparkles size={14} aria-hidden /> Próximamente
          </span>
          <h2 className="mt-4 text-3xl font-bold text-white sm:text-4xl">Captura automática</h2>
          <p className="mt-3 text-lg opacity-85">
            Imagina que tus gastos se registren solos. Estamos trabajando en que Moni detecte tus movimientos para que tú solo los confirmes.
          </p>
        </div>
      </Reveal>
    </section>
  );
}

function Download() {
  return (
    <section id="descarga" className="scroll-mt-4 bg-card/60 py-16 md:py-24">
      <Reveal className="mx-auto w-full max-w-3xl px-4 text-center sm:px-8">
        <Eyebrow>Descarga</Eyebrow>
        <h2 className="mt-2 text-3xl font-bold sm:text-4xl">Descarga la app</h2>
        <p className="mt-3 text-lg text-mut">Lleva Moni en tu bolsillo y registra tus gastos en el momento.</p>
        <div className="mt-8 flex flex-col items-center gap-3">
          <button
            type="button"
            disabled
            aria-describedby="android-note"
            className="inline-flex min-h-14 cursor-not-allowed items-center gap-3 rounded-btn bg-fg px-6 text-left text-white opacity-90"
          >
            <Smartphone size={24} aria-hidden />
            <span>
              <span className="block text-[11px] opacity-75">Disponible para</span>
              <span className="block text-base font-semibold leading-tight">Android</span>
            </span>
          </button>
          <p id="android-note" className="text-sm text-mut">
            Próximamente. Mientras tanto, entra desde la web.
          </p>
        </div>
      </Reveal>
    </section>
  );
}

function FinalCta() {
  return (
    <section className="mx-auto w-full max-w-6xl px-4 py-16 text-center sm:px-8 md:py-24">
      <Reveal className="flex flex-col items-center">
        <h2 className="text-3xl font-bold sm:text-4xl">Empieza a llevar tus finanzas en calma.</h2>
        <p className="mt-3 max-w-xl text-lg text-mut">Crea tu cuenta o inicia sesión y ten todo claro desde hoy.</p>
        <Link href="/login" className="btn-primary mt-8 sm:w-auto sm:min-w-56">
          Iniciar sesión
        </Link>
      </Reveal>
    </section>
  );
}

function Footer() {
  return (
    <footer className="border-t border-line">
      <div className="mx-auto flex w-full max-w-6xl flex-col items-center justify-between gap-4 px-4 py-8 text-sm text-mut sm:flex-row sm:px-8">
        <MoniLockup size={24} />
        <nav aria-label="Pie" className="flex flex-wrap items-center justify-center gap-1">
          {NAV.map((n) => (
            <a key={n.href} href={n.href} className="rounded-full px-3 py-2.5 hover:text-acc-ink">
              {n.label}
            </a>
          ))}
          <Link href="/login" className="rounded-full px-3 py-2.5 hover:text-acc-ink">
            Iniciar sesión
          </Link>
        </nav>
        <p>© {new Date().getFullYear()} moni</p>
      </div>
    </footer>
  );
}

export default function Landing() {
  return (
    <div className="flex-1 overflow-x-clip">
      <Header />
      <main>
        <Hero />
        <Features />
        <Privacy />
        <Upcoming />
        <Download />
        <FinalCta />
      </main>
      <Footer />
    </div>
  );
}
