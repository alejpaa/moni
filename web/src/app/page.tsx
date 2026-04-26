export default function Home() {
  return (
    <div className="min-h-screen bg-slate-50 p-8 text-slate-900">
      <main className="mx-auto flex w-full max-w-3xl flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-8">
        <p className="text-sm font-medium uppercase tracking-wide text-emerald-700">
          Moni
        </p>
        <h1 className="text-3xl font-semibold tracking-tight">Finance dashboard</h1>
        <p className="text-slate-600">
          Base web app ready. Next step is connecting Supabase and creating
          transactions, savings goals, and charts.
        </p>
      </main>
    </div>
  );
}
