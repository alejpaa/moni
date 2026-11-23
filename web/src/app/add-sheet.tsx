"use client";

import { useActionState, useEffect, useId, useRef, useState } from "react";
import { CircleAlert, Loader2, X } from "lucide-react";
import type { Account, Category, TransactionType } from "@moni/shared";
import CategoryIcon from "./category-icon";
import { addTransaction, type AddState } from "./actions";

/** Modal sheet built on <dialog>: native focus trap, Esc to close, focus returns to the trigger. */
export default function AddSheet({
  open,
  onClose,
  categories,
  accounts,
}: {
  open: boolean;
  onClose: () => void;
  categories: Category[];
  accounts: Account[];
}) {
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
      {open && (
        <AddForm
          categories={categories}
          accounts={accounts}
          onDone={() => ref.current?.close()} onCancel={() => ref.current?.close()}
        />
      )}
    </dialog>
  );
}

function AddForm({
  categories,
  accounts,
  onDone,
  onCancel,
}: {
  categories: Category[];
  accounts: Account[];
  onDone: () => void;
  onCancel: () => void;
}) {
  const uid = useId();
  const [type, setType] = useState<TransactionType>("expense");
  const visibleCats = categories.filter((c) => c.kind === type);
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
  const accErr = state.fieldErrors?.account;

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
          ].map(([v, label]) => (
            <label key={v} className="relative">
              <input
                type="radio"
                name="type"
                value={v}
                checked={type === v}
                onChange={() => setType(v as TransactionType)}
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

      <fieldset aria-describedby={catErr ? `${uid}-cat-err` : undefined}>
        <legend className="mb-1.5 text-sm font-medium">Categoría</legend>
        {/* key resets the selection when the kind changes */}
        <div key={type} className="grid max-h-44 grid-cols-2 gap-2 overflow-y-auto p-0.5">
          {visibleCats.map((c) => (
            <label key={c.id} className="relative">
              <input type="radio" name="category_id" value={c.id} className="peer sr-only" />
              <span className="flex min-h-11 cursor-pointer items-center gap-2 rounded-input bg-card px-2 text-sm font-medium ring-1 ring-line peer-checked:ring-2 peer-checked:ring-acc peer-focus-visible:outline-2 peer-focus-visible:outline-acc">
                <CategoryIcon category={c.name} icon={c.icon} type={c.kind} />
                <span className="truncate">{c.name}</span>
              </span>
            </label>
          ))}
          {visibleCats.length === 0 && <p className="col-span-2 text-sm text-mut">No hay categorías de este tipo.</p>}
        </div>
        {catErr && <Err id={`${uid}-cat-err`}>{catErr}</Err>}
      </fieldset>

      <div>
        <label htmlFor={`${uid}-acc`} className="mb-1.5 block text-sm font-medium">
          Cuenta
        </label>
        <div className="field" data-invalid={!!accErr}>
          <select
            id={`${uid}-acc`}
            name="account_id"
            required
            defaultValue={accounts[0]?.id ?? ""}
            aria-invalid={!!accErr}
            aria-describedby={accErr ? `${uid}-acc-err` : undefined}
          >
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.name}
              </option>
            ))}
          </select>
        </div>
        {accErr && <Err id={`${uid}-acc-err`}>{accErr}</Err>}
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
