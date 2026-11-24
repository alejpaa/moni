import { ArrowDownLeft, Briefcase, Coffee, Eye, PiggyBank, Plane, ShoppingCart } from "lucide-react";
import CountUp from "../count-up";

const ROWS = [
  { name: "Sueldo", date: "Hoy", amount: "+ S/ 3,200.00", income: true, Icon: Briefcase },
  { name: "Supermercado", date: "Ayer", amount: "− S/ 186.40", income: false, Icon: ShoppingCart },
  { name: "Café con Lucía", date: "Lun", amount: "− S/ 14.50", income: false, Icon: Coffee },
] as const;

/** Decorative app mockup in HTML/CSS with sample data only. */
export function PhonePreview() {
  return (
    <div
      aria-hidden
      className="relative mx-auto w-full max-w-[300px] rounded-[36px] bg-fg p-2.5 shadow-[0_30px_60px_-24px_rgba(31,30,34,0.55)]"
    >
      <div className="rounded-[28px] bg-bg px-3.5 pb-4 pt-5">
        <div className="mb-3 flex items-center justify-between px-1">
          <div>
            <div className="text-[10px] font-semibold tracking-[0.12em] text-mut">TUS FINANZAS</div>
            <div className="text-[17px] font-bold tracking-[-0.02em]">Hola, Ana</div>
          </div>
          <span className="flex size-8 items-center justify-center rounded-full bg-card text-mut shadow-chip">
            <Eye size={16} />
          </span>
        </div>

        <div className="anim-hero rounded-hero bg-hero p-4 text-hero-fg shadow-hero" style={{ animationDelay: "200ms" }}>
          <div className="text-[11px] opacity-70">Disponible</div>
          <div className="mt-0.5 text-[30px] font-bold leading-tight tracking-[-0.03em]">
            S/ <CountUp value={4820.5} />
          </div>
          <div className="mt-3 flex gap-4 text-[12px] font-semibold">
            <span className="inline-flex items-center gap-1 text-[#7dd8c0]">
              <ArrowDownLeft size={14} /> S/ 3,200
            </span>
            <span className="inline-flex items-center gap-1 text-[#f28baa]">
              <ArrowDownLeft size={14} className="rotate-180" /> S/ 1,240
            </span>
          </div>
        </div>

        <div className="mt-3 rounded-card bg-card p-1.5 shadow-card">
          {ROWS.map(({ name, date, amount, income, Icon }, i) => (
            <div
              key={name}
              className={`anim-rise flex items-center gap-2.5 px-2 py-2 ${i > 0 ? "border-t border-divider" : ""}`}
              style={{ "--delay": `${500 + i * 70}ms` } as React.CSSProperties}
            >
              <span className="flex size-9 shrink-0 items-center justify-center rounded-[10px] bg-acc-tile text-acc-ink">
                <Icon size={18} />
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-[13px] font-semibold">{name}</span>
                <span className="block text-[11px] text-mut">{date}</span>
              </span>
              <span className={`text-[13px] font-semibold ${income ? "text-inc" : "text-exp"}`}>{amount}</span>
            </div>
          ))}
        </div>

        <div className="anim-rise mt-3 rounded-card bg-card p-3 shadow-card" style={{ "--delay": "760ms" } as React.CSSProperties}>
          <div className="flex items-center gap-2.5">
            <span className="flex size-9 items-center justify-center rounded-[10px] bg-acc-tile text-acc-ink">
              <Plane size={18} />
            </span>
            <span className="flex-1">
              <span className="block text-[13px] font-semibold">Viaje a Cusco</span>
              <span className="block text-[11px] text-mut">S/ 1,800 de S/ 3,000</span>
            </span>
            <span className="text-[12px] font-semibold text-acc-ink">60%</span>
          </div>
          <div className="mt-2.5 h-2 overflow-hidden rounded-full bg-acc-tile">
            <div className="goal-bar h-full w-3/5 rounded-full bg-acc" />
          </div>
        </div>
      </div>
    </div>
  );
}

/** Small goal/ahorro card used in the "Metas" feature block. */
export function GoalsPreview() {
  const goals = [
    { name: "Viaje a Cusco", pct: 60, saved: "S/ 1,800", target: "S/ 3,000" },
    { name: "Laptop nueva", pct: 35, saved: "S/ 1,050", target: "S/ 3,000" },
    { name: "Fondo de emergencia", pct: 82, saved: "S/ 4,100", target: "S/ 5,000" },
  ];
  return (
    <div aria-hidden className="space-y-3 rounded-card bg-card p-4 shadow-card">
      {goals.map((g) => (
        <div key={g.name}>
          <div className="mb-1.5 flex items-center justify-between text-[13px]">
            <span className="inline-flex items-center gap-2 font-semibold">
              <span className="flex size-7 items-center justify-center rounded-[9px] bg-acc-tile text-acc-ink">
                <PiggyBank size={15} />
              </span>
              {g.name}
            </span>
            <span className="text-mut">
              {g.saved} / {g.target}
            </span>
          </div>
          <div className="h-2 overflow-hidden rounded-full bg-acc-tile">
            <div className="h-full rounded-full bg-acc" style={{ width: `${g.pct}%` }} />
          </div>
        </div>
      ))}
    </div>
  );
}
