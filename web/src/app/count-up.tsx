"use client";

import { useEffect, useRef, useState } from "react";

const money = new Intl.NumberFormat("es", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** Animates from the previous value to `value` (900ms first run, 500ms after, ease-out cubic). */
export default function CountUp({ value, className }: { value: number; className?: string }) {
  const [display, setDisplay] = useState(0);
  const from = useRef(0);
  const first = useRef(true);

  useEffect(() => {
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const duration = reduced ? 0 : first.current ? 900 : 500;
    first.current = false;
    const start = from.current;
    const delta = value - start;
    let raf = 0;
    let t0: number | null = null;

    const tick = (now: number) => {
      t0 ??= now;
      const p = duration === 0 ? 1 : Math.min((now - t0) / duration, 1);
      const eased = 1 - Math.pow(1 - p, 3);
      const current = start + delta * eased;
      from.current = current;
      setDisplay(current);
      if (p < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [value]);

  return (
    <span className={className}>
      <span className="sr-only">{money.format(value)}</span>
      <span aria-hidden>{money.format(display)}</span>
    </span>
  );
}
