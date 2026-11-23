"use client";

import { useActionState, useEffect, useId, useRef } from "react";
import { CircleAlert, Loader2, X } from "lucide-react";
import { addTransaction, type AddState } from "./actions";

/** Modal sheet built on <dialog>: native focus trap, Esc to close, focus returns to the trigger. */
export default function AddSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    if (open && !d.open) d.showModal();
    if (!open && d.open) d.close();
  }, [open]);

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      onClick={(e) => {
        if (e.target === ref.current) ref.current?.close();
      }}
      aria-labelledby="add-title"
      className="sheet m-auto w-[calc(100%-2rem)] max-w-md rounded-card bg-card p-0 text-fg shadow-hero"
    >
      {open && <AddForm onDone={() => ref.current?.close()} onCancel={() => ref.current?.close()} />}
    </dialog>
  );
}

function AddForm({ onDone, onCancel }: { onDone: () => void; onCancel: () => void }) {
  const uid = useId();
  const [state, formAction, pending] = useActionState<AddState, FormData>(
    async (prev, fd) => {
      const next = await addTransaction(prev, fd);
      if (next.ok) onDone();
      return next;
    },
    { ok: false, nonce: 0 },
  );
  const amountErr = state.fieldErrors?.amount;
  const catErr = state.fieldErrors?.category;

  return (
    <form action={formAction} noValidate className="flex flex-col gap-4 p-5 sm:p-6">
      <div className="flex items-center justify-between">
        <h2 id="add-title" className="text-xl font-bold">
          Nuevo movimiento
        </h2>
        <button type="button" className="icon-btn -mr-2" onClick={onCancel} aria-label="Cerrar">
          <X size={20} aria-hidden />
        </button>
      </div>

      <fieldset>
        <legend className="mb-1.5 text-sm font-medium">Tipo</legend>
        <div className="grid grid-cols-2 gap-1 rounded-full bg-bg p-1 ring-1 ring-line">
          {[
            ["expense", "Gasto"],
            ["income", "Ingreso"],
          ].map(([v, label], i) => (
            <label key={v} className="relative">
              <input
                type="radio"
                name="type"
                value={v}
                defaultChecked={i === 0}
                className="peer sr-only"
              />
              <span className="flex min-h-11 cursor-pointer items-center justify-center rounded-full text-sm font-semibold text-mut transition-colors peer-checked:bg-card peer-checked:text-fg peer-checked:shadow-chip peer-focus-visible:outline-2 peer-focus-visible:outline-acc">
                {label}
              </span>
            </label>
          ))}
        </div>
      </fieldset>

      <div>
        <label htmlFor={`${uid}-amount`} className="mb-1.5 block text-sm font-medium">
          Monto
        </label>
        <div className="field" data-invalid={!!amountErr}>
          <input
            id={`${uid}-amount`}
            name="amount"
            type="number"
            inputMode="decimal"
            step="0.01"
            min="0.01"
            required
            autoFocus
            placeholder="0,00"
            aria-invalid={!!amountErr}
            aria-describedby={amountErr ? `${uid}-amount-err` : undefined}
          />
        </div>
        {amountErr && <Err id={`${uid}-amount-err`}>{amountErr}</Err>}
      </div>

      <div>
        <label htmlFor={`${uid}-cat`} className="mb-1.5 block text-sm font-medium">
          Categoría
        </label>
        <div className="field" data-invalid={!!catErr}>
          <input
            id={`${uid}-cat`}
            name="category"
            required
            autoComplete="off"
            placeholder="Comida, sueldo, transporte…"
            aria-invalid={!!catErr}
            aria-describedby={catErr ? `${uid}-cat-err` : undefined}
          />
        </div>
        {catErr && <Err id={`${uid}-cat-err`}>{catErr}</Err>}
      </div>

      <div>
        <label htmlFor={`${uid}-note`} className="mb-1.5 block text-sm font-medium">
          Nota <span className="font-normal text-mut">(opcional)</span>
        </label>
        <div className="field">
          <input id={`${uid}-note`} name="note" autoComplete="off" />
        </div>
      </div>

      {state.error && (
        <div
          role="alert"
          className="anim-error flex items-start gap-2 rounded-input bg-exp/10 px-3.5 py-3 text-sm font-medium text-exp"
        >
          <CircleAlert size={18} aria-hidden className="mt-0.5 shrink-0" />
          {state.error}
        </div>
      )}

      <button type="submit" className="btn-primary" disabled={pending}>
        {pending ? (
          <>
            <Loader2 size={18} aria-hidden className="anim-spin" />
            Guardando…
          </>
        ) : (
          "Guardar"
        )}
      </button>
    </form>
  );
}

function Err({ id, children }: { id: string; children: React.ReactNode }) {
  return (
    <p id={id} className="anim-error mt-1.5 flex items-center gap-1.5 text-sm font-medium text-exp">
      <CircleAlert size={16} aria-hidden className="shrink-0" />
      {children}
    </p>
  );
}
