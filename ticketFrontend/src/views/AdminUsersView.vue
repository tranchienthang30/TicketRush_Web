<script setup>
import { computed, onMounted, ref } from "vue";
import { deleteAdminUser, getAdminUsers, updateAdminUserRole } from "@/api/admin.api";

const users = ref([]);
const loading = ref(true);
const error = ref("");
const message = ref("");
const search = ref("");
const roleFilter = ref("ALL");
const savingUserId = ref("");
const deletingUserId = ref("");
const roleDrafts = ref({});
const editableRoles = ["CUSTOMER", "PROVIDER", "ADMIN"];

const roleOptions = computed(() => {
  const roles = new Set(users.value.map((user) => user.role).filter(Boolean));
  return ["ALL", ...Array.from(roles).sort()];
});

const filteredUsers = computed(() => {
  const query = search.value.trim().toLowerCase();
  return users.value.filter((user) => {
    const matchesRole = roleFilter.value === "ALL" || user.role === roleFilter.value;
    const matchesSearch = !query ||
      [user.email, user.fullName, user.phone, user.providerRequestStatus]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(query));
    return matchesRole && matchesSearch;
  });
});

onMounted(loadUsers);

async function loadUsers() {
  loading.value = true;
  error.value = "";
  try {
    const response = await getAdminUsers();
    users.value = response.data || [];
    roleDrafts.value = Object.fromEntries(users.value.map((user) => [user.id, user.role]));
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to load users.";
  } finally {
    loading.value = false;
  }
}

async function saveRole(user) {
  const nextRole = roleDrafts.value[user.id];
  if (!nextRole || nextRole === user.role) return;

  savingUserId.value = user.id;
  error.value = "";
  message.value = "";
  try {
    const response = await updateAdminUserRole(user.id, nextRole);
    const index = users.value.findIndex((item) => item.id === user.id);
    if (index >= 0) {
      users.value[index] = response.data;
      roleDrafts.value[user.id] = response.data.role;
    }
    message.value = "User role has been updated.";
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to update user role.";
    roleDrafts.value[user.id] = user.role;
  } finally {
    savingUserId.value = "";
  }
}

async function deleteUser(user) {
  const confirmed = window.confirm(
    `Delete ${user.email}? This also deletes tickets, orders, and provider events linked to this account.`,
  );
  if (!confirmed) return;

  deletingUserId.value = user.id;
  error.value = "";
  message.value = "";
  try {
    await deleteAdminUser(user.id);
    users.value = users.value.filter((item) => item.id !== user.id);
    delete roleDrafts.value[user.id];
    message.value = "User account and linked data have been deleted.";
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to delete user.";
  } finally {
    deletingUserId.value = "";
  }
}

function formatDate(value) {
  return value
    ? new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value))
    : "TBA";
}
</script>

<template>
  <section class="min-h-screen bg-brand-light px-4 py-10 dark:bg-slate-900">
    <div class="mx-auto max-w-6xl">
      <p class="text-sm font-black uppercase tracking-[0.25em] text-brand-orange">Admin</p>
      <div class="mt-2 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 class="text-3xl font-black text-brand-navy dark:text-white">User Management</h1>
          <p class="mt-2 max-w-2xl text-sm leading-7 text-slate-500 dark:text-slate-300">
            Review registered accounts, roles, status, provider request state, and authentication source.
          </p>
        </div>
        <button class="rounded-xl bg-brand-navy px-4 py-3 text-sm font-black text-white" @click="loadUsers">
          Refresh
        </button>
      </div>

      <div class="mt-8 grid gap-3 md:grid-cols-[1fr_220px]">
        <input
          v-model.trim="search"
          placeholder="Search by name, email, phone..."
          class="rounded-xl border border-slate-200 bg-white px-4 py-3 font-bold text-slate-900 shadow-sm dark:border-slate-700 dark:bg-slate-800 dark:text-white"
        />
        <select
          v-model="roleFilter"
          class="rounded-xl border border-slate-200 bg-white px-4 py-3 font-bold text-slate-900 shadow-sm dark:border-slate-700 dark:bg-slate-800 dark:text-white"
        >
          <option v-for="role in roleOptions" :key="role" :value="role">{{ role }}</option>
        </select>
      </div>

      <p v-if="loading" class="mt-8 rounded-2xl bg-white p-6 font-bold text-slate-500 shadow-sm dark:bg-slate-800 dark:text-slate-300">
        Loading users...
      </p>
      <p v-else-if="error" class="mt-8 rounded-2xl border border-red-100 bg-red-50 p-6 font-bold text-red-700">
        {{ error }}
      </p>
      <p v-if="message" class="mt-4 rounded-2xl border border-emerald-100 bg-emerald-50 p-4 font-bold text-emerald-700">
        {{ message }}
      </p>

      <div v-if="!loading && !error" class="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-slate-100 dark:divide-slate-700">
            <thead class="bg-slate-50 text-left text-xs font-black uppercase tracking-[0.14em] text-slate-500 dark:bg-slate-900">
              <tr>
                <th class="px-4 py-3">Account</th>
                <th class="px-4 py-3">Role</th>
                <th class="px-4 py-3">Status</th>
                <th class="px-4 py-3">Provider request</th>
                <th class="px-4 py-3">Auth</th>
                <th class="px-4 py-3">Created</th>
                <th class="px-4 py-3">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 dark:divide-slate-700">
              <tr v-for="user in filteredUsers" :key="user.id" class="text-sm">
                <td class="px-4 py-4">
                  <p class="font-black text-slate-900 dark:text-white">{{ user.fullName }}</p>
                  <p class="text-slate-500 dark:text-slate-300">{{ user.email }}</p>
                  <p v-if="user.phone" class="text-xs font-bold text-slate-400">{{ user.phone }}</p>
                </td>
                <td class="px-4 py-4">
                  <select
                    v-model="roleDrafts[user.id]"
                    class="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-black text-slate-800 dark:border-slate-700 dark:bg-slate-900 dark:text-white"
                  >
                    <option v-for="role in editableRoles" :key="role" :value="role">{{ role }}</option>
                  </select>
                </td>
                <td class="px-4 py-4">
                  <span class="rounded-lg bg-slate-100 px-2 py-1 text-xs font-black text-slate-700 dark:bg-slate-900 dark:text-slate-200">{{ user.status }}</span>
                </td>
                <td class="px-4 py-4 font-bold text-slate-600 dark:text-slate-300">
                  {{ user.providerRequestStatus || "NONE" }}
                </td>
                <td class="px-4 py-4 font-bold text-slate-600 dark:text-slate-300">
                  {{ user.provider }} / {{ user.emailVerified ? "Verified" : "Unverified" }}
                </td>
                <td class="px-4 py-4 text-slate-500 dark:text-slate-300">
                  {{ formatDate(user.createdAt) }}
                </td>
                <td class="px-4 py-4">
                  <div class="flex flex-wrap gap-2">
                    <button
                      type="button"
                      :disabled="savingUserId === user.id || roleDrafts[user.id] === user.role"
                      class="rounded-lg bg-brand-navy px-3 py-2 text-xs font-black text-white disabled:cursor-not-allowed disabled:bg-slate-300"
                      @click="saveRole(user)"
                    >
                      {{ savingUserId === user.id ? "Saving..." : "Save role" }}
                    </button>
                    <button
                      type="button"
                      :disabled="deletingUserId === user.id"
                      class="rounded-lg bg-red-600 px-3 py-2 text-xs font-black text-white disabled:cursor-not-allowed disabled:bg-slate-300"
                      @click="deleteUser(user)"
                    >
                      {{ deletingUserId === user.id ? "Deleting..." : "Delete" }}
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="filteredUsers.length === 0">
                <td colspan="7" class="px-4 py-8 text-center font-bold text-slate-500">No users matched.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </section>
</template>
