/* eslint-disable @next/next/no-img-element -- static SVG brand assets, no optimization needed */

/** Isotipo: monkey face on a graphite rounded square (docs/brand/moni-isotipo.svg). */
export function MoniMark({ size = 28, className = "" }: { size?: number; className?: string }) {
  return <img src="/brand/moni-isotipo.svg" width={size} height={size} alt="" aria-hidden className={className} />;
}

/** Isotipo + "moni" wordmark. Replaces the old "MONI" eyebrow. */
export function MoniLockup({ size = 22 }: { size?: number }) {
  return (
    <span className="inline-flex items-center gap-2">
      <MoniMark size={size} className="rounded-[6px]" />
      <span className="text-[15px] font-bold tracking-[-0.02em] text-fg">moni</span>
    </span>
  );
}

/** Mascot M3: the monkey peeking over a gold coin. Enters with a springy peek. */
export function MoniMascot({ size = 112, className = "" }: { size?: number; className?: string }) {
  return (
    <img
      src="/brand/moni-mascota.svg"
      width={size}
      height={Math.round((size * 220) / 200)}
      alt=""
      aria-hidden
      className={`anim-peek ${className}`}
    />
  );
}
