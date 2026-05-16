<template>
  <section class="min-h-screen bg-brand-light px-4 py-10 dark:bg-slate-900">
    <div class="mx-auto max-w-6xl">
      <div class="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <p class="text-sm font-black uppercase tracking-[0.25em] text-brand-orange">Provider</p>
        <h1 class="mt-3 text-4xl font-black tracking-tight text-brand-navy dark:text-white">
          Welcome to TicketRush Provider Workspace
        </h1>
        <p class="mt-4 max-w-3xl text-sm leading-7 text-slate-600 dark:text-slate-300">
          This workspace is for creating and managing your own events. Provider accounts are separated from the customer booking flow, so public event browsing and seat booking are not shown here.
        </p>
      </div>

      <div class="mt-8 grid gap-6 lg:grid-cols-[1fr_1fr]">
        <div class="rounded-2xl border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800">
          <h2 class="text-2xl font-black text-brand-navy dark:text-white">Provider rules</h2>
          <ul class="mt-5 space-y-4 text-sm leading-7 text-slate-600 dark:text-slate-300">
            <li class="rounded-xl bg-slate-50 px-4 py-3 dark:bg-slate-900">
              Event information must be accurate, including title, description, location, schedule, ticket sale window, and banner image.
            </li>
            <li class="rounded-xl bg-slate-50 px-4 py-3 dark:bg-slate-900">
              Seat maps, seat labels, prices, and ticket sections must match the real venue setup.
            </li>
            <li class="rounded-xl bg-slate-50 px-4 py-3 dark:bg-slate-900">
              Payment and payout information must belong to the approved provider account.
            </li>
            <li class="rounded-xl bg-slate-50 px-4 py-3 dark:bg-slate-900">
              TicketRush can later add admin review before publishing public events.
            </li>
          </ul>
        </div>

        <div class="rounded-2xl border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800">
          <h2 class="text-2xl font-black text-brand-navy dark:text-white">Workflow</h2>
          <div class="mt-5 space-y-4">
            <div class="rounded-xl border border-slate-100 px-4 py-4 dark:border-slate-700">
              <p class="font-black text-slate-900 dark:text-white">1. Creating</p>
              <p class="mt-1 text-sm leading-7 text-slate-500 dark:text-slate-300">
                Create event information, seating configuration, payout details, and provider confirmation.
              </p>
            </div>
            <div class="rounded-xl border border-slate-100 px-4 py-4 dark:border-slate-700">
              <p class="font-black text-slate-900 dark:text-white">2. Managements</p>
              <p class="mt-1 text-sm leading-7 text-slate-500 dark:text-slate-300">
                Review events you created and prepare for future edit, schedule, ticket, and revenue management.
              </p>
            </div>
            <div class="rounded-xl border border-slate-100 px-4 py-4 dark:border-slate-700">
              <p class="font-black text-slate-900 dark:text-white">3. Help Center</p>
              <p class="mt-1 text-sm leading-7 text-slate-500 dark:text-slate-300">
                Check support information and operational guidance when provider workflows are expanded.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
