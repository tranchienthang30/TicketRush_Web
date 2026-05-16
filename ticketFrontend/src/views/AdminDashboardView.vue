<template>
  <section class="min-h-screen bg-brand-light px-4 py-10 dark:bg-slate-900">
    <div class="mx-auto max-w-6xl">
      <p class="text-sm font-black uppercase tracking-[0.25em] text-brand-orange">Admin</p>
      <h1 class="mt-2 text-3xl font-black text-brand-navy dark:text-white">Dashboard</h1>
      <p class="mt-2 max-w-2xl text-sm leading-7 text-slate-500 dark:text-slate-300">
        Overview for users, events, orders, and platform activity. Detailed charts and API-backed metrics will be added in the admin phase.
      </p>

      <div class="mt-8 grid gap-4 md:grid-cols-3">
        <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
          <p class="text-xs font-black uppercase tracking-[0.18em] text-slate-400">Users</p>
          <p class="mt-3 text-3xl font-black text-brand-navy dark:text-white">Ready</p>
          <p class="mt-2 text-sm text-slate-500 dark:text-slate-300">Customer, provider, and admin overview.</p>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
          <p class="text-xs font-black uppercase tracking-[0.18em] text-slate-400">Events</p>
          <p class="mt-3 text-3xl font-black text-brand-navy dark:text-white">Ready</p>
          <p class="mt-2 text-sm text-slate-500 dark:text-slate-300">Published and pending event monitoring.</p>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
          <p class="text-xs font-black uppercase tracking-[0.18em] text-slate-400">Revenue</p>
          <p class="mt-3 text-3xl font-black text-brand-navy dark:text-white">Ready</p>
          <p class="mt-2 text-sm text-slate-500 dark:text-slate-300">Payment and order summary placeholder.</p>
        </div>
      </div>
    </div>
  </section>
</template>
