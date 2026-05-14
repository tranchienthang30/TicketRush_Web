<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { getCurrentMembership } from "../api/ticketRushApi";

const CHECKOUT_STORAGE_KEY = "ticketrush_checkout_payload";
const FALLBACK_SERVICE_FEE = 15000;

const router = useRouter();

const checkoutData = ref(null);
const membership = ref(null);
const membershipLoading = ref(false);
const selectedVoucherCode = ref("");
const voucherCodeInput = ref("");
const paymentMethod = ref("card");
const agreedTerms = ref(false);
const paymentSuccess = ref(false);
const contact = ref({
  fullName: "",
  email: "",
  phone: "",
});

const paymentMethods = [
  {
    id: "card",
    label: "Thẻ ngân hàng",
    description: "Visa, MasterCard, JCB và nội địa Napas.",
    badge: "Secure",
  },
  {
    id: "wallet",
    label: "Ví điện tử",
    description: "Momo, ZaloPay, VNPay QR.",
    badge: "Instant",
  },
  {
    id: "transfer",
    label: "Chuyển khoản nhanh",
    description: "Xác nhận giao dịch trong 1-2 phút.",
    badge: "24/7",
  },
];

const voucherCatalog = [
  {
    code: "WELCOME20",
    discountType: "PERCENT",
    discountValue: 20,
    maxDiscount: 200000,
    minOrderAmount: 300000,
    membershipRequired: false,
  },
  {
    code: "MEMBER10",
    discountType: "PERCENT",
    discountValue: 10,
    maxDiscount: 150000,
    minOrderAmount: 200000,
    membershipRequired: true,
  },
  {
    code: "GOLD50",
    discountType: "FIXED",
    discountValue: 50000,
    maxDiscount: null,
    minOrderAmount: 300000,
    membershipRequired: true,
  },
  {
    code: "MUSIC15",
    discountType: "PERCENT",
    discountValue: 15,
    maxDiscount: 120000,
    minOrderAmount: 250000,
    membershipRequired: false,
  },
  {
    code: "STUDENT30",
    discountType: "FIXED",
    discountValue: 30000,
    maxDiscount: null,
    minOrderAmount: 150000,
    membershipRequired: false,
  },
  {
    code: "PREMIUM100",
    discountType: "FIXED",
    discountValue: 100000,
    maxDiscount: null,
    minOrderAmount: 800000,
    membershipRequired: true,
  },
];

function formatMoney(value) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value || 0);
}

function loadCheckoutData() {
  const rawData = sessionStorage.getItem(CHECKOUT_STORAGE_KEY);
  if (!rawData) {
    checkoutData.value = null;
    return;
  }

  try {
    checkoutData.value = JSON.parse(rawData);
  } catch {
    checkoutData.value = null;
  }
}

async function loadMembership() {
  membershipLoading.value = true;

  try {
    membership.value = await getCurrentMembership();
  } catch {
    membership.value = null;
  } finally {
    membershipLoading.value = false;
  }
}

const hasCheckoutData = computed(() => {
  return Boolean(checkoutData.value?.seats?.length);
});

const selectedSeats = computed(() => checkoutData.value?.seats || []);
const seatCount = computed(() => selectedSeats.value.length);
const seatLabel = computed(() => selectedSeats.value.map((seat) => seat.id).join(", "));

const subtotal = computed(() => {
  const fixedSubtotal = checkoutData.value?.summary?.subtotal;
  if (typeof fixedSubtotal === "number") {
    return fixedSubtotal;
  }
  return selectedSeats.value.reduce((sum, seat) => sum + (seat.price || 0), 0);
});

const serviceFee = computed(() => {
  return checkoutData.value?.summary?.serviceFee ?? FALLBACK_SERVICE_FEE;
});

const membershipDiscountRate = computed(() => {
  if (!membership.value?.active) {
    return 0;
  }
  return Number(membership.value.discountPercent || 0);
});

const membershipDiscount = computed(() => {
  if (membershipDiscountRate.value <= 0) {
    return 0;
  }
  return Math.round((subtotal.value * membershipDiscountRate.value) / 100);
});

const amountAfterMembership = computed(() => {
  return Math.max(subtotal.value + serviceFee.value - membershipDiscount.value, 0);
});

const activeVoucherCode = computed(() => {
  return (selectedVoucherCode.value || voucherCodeInput.value).trim().toUpperCase();
});

const resolvedVoucher = computed(() => {
  if (!activeVoucherCode.value) {
    return null;
  }
  return voucherCatalog.find((voucher) => voucher.code === activeVoucherCode.value) || null;
});

const voucherValidation = computed(() => {
  if (!activeVoucherCode.value) {
    return {
      state: "idle",
      message: "Nhập hoặc chọn mã giảm giá để áp dụng.",
      discount: 0,
    };
  }

  if (!resolvedVoucher.value) {
    return {
      state: "invalid",
      message: "Mã giảm giá không tồn tại trong hệ thống demo.",
      discount: 0,
    };
  }

  if (amountAfterMembership.value < resolvedVoucher.value.minOrderAmount) {
    return {
      state: "invalid",
      message: `Đơn tối thiểu ${formatMoney(resolvedVoucher.value.minOrderAmount)} để dùng mã ${resolvedVoucher.value.code}.`,
      discount: 0,
    };
  }

  if (resolvedVoucher.value.membershipRequired && !membership.value?.active) {
    return {
      state: "invalid",
      message: `Mã ${resolvedVoucher.value.code} chỉ áp dụng cho tài khoản có membership.`,
      discount: 0,
    };
  }

  let discount = 0;
  if (resolvedVoucher.value.discountType === "PERCENT") {
    discount = Math.round((amountAfterMembership.value * resolvedVoucher.value.discountValue) / 100);
    if (resolvedVoucher.value.maxDiscount) {
      discount = Math.min(discount, resolvedVoucher.value.maxDiscount);
    }
  } else {
    discount = resolvedVoucher.value.discountValue;
  }

  discount = Math.min(discount, amountAfterMembership.value);

  return {
    state: "valid",
    message: `Áp dụng thành công mã ${resolvedVoucher.value.code}.`,
    discount,
  };
});

const voucherDiscount = computed(() => {
  return voucherValidation.value.state === "valid" ? voucherValidation.value.discount : 0;
});

const grandTotal = computed(() => {
  return Math.max(amountAfterMembership.value - voucherDiscount.value, 0);
});

const isPayDisabled = computed(() => {
  if (!hasCheckoutData.value) {
    return true;
  }
  if (!contact.value.fullName.trim() || !contact.value.email.trim() || !contact.value.phone.trim()) {
    return true;
  }
  return !agreedTerms.value;
});

function pickVoucher(code) {
  selectedVoucherCode.value = code;
  voucherCodeInput.value = code;
}

function clearVoucher() {
  selectedVoucherCode.value = "";
  voucherCodeInput.value = "";
}

function goBackToBooking() {
  router.push("/booking");
}

function payNow() {
  if (isPayDisabled.value) return;
  paymentSuccess.value = true;
}

onMounted(() => {
  loadCheckoutData();
  loadMembership();
});
</script>

<template>
  <div class="min-h-screen bg-brand-light dark:bg-slate-900 pb-20">
    <section class="bg-brand-navy text-white">
      <div class="max-w-7xl mx-auto px-4 md:px-8 py-12 md:py-16">
        <p class="text-sm font-black uppercase tracking-[0.35em] text-brand-orange">
          Checkout
        </p>
        <h1 class="mt-3 text-4xl md:text-5xl font-black tracking-tight">
          Tiếp tục thanh toán
        </h1>
        <p class="mt-4 max-w-2xl text-blue-100 leading-relaxed">
          Hoàn tất thông tin thanh toán, áp dụng membership hoặc voucher, và kiểm tra lại ghế đã chọn trước khi xác nhận.
        </p>

        <div class="mt-8 grid gap-3 md:grid-cols-3">
          <div class="rounded-2xl border border-white/20 bg-white/10 px-4 py-3">
            <p class="text-xs uppercase tracking-[0.18em] text-white/70 font-bold">Bước 1</p>
            <p class="mt-1 text-sm font-black">Chọn ghế</p>
          </div>
          <div class="rounded-2xl border border-brand-orange bg-brand-orange px-4 py-3 text-white shadow-lg">
            <p class="text-xs uppercase tracking-[0.18em] text-white/80 font-bold">Bước 2</p>
            <p class="mt-1 text-sm font-black">Thanh toán</p>
          </div>
          <div class="rounded-2xl border border-white/20 bg-white/10 px-4 py-3">
            <p class="text-xs uppercase tracking-[0.18em] text-white/70 font-bold">Bước 3</p>
            <p class="mt-1 text-sm font-black">Xác nhận vé</p>
          </div>
        </div>
      </div>
    </section>

    <section class="max-w-7xl mx-auto px-4 md:px-8 mt-10">
      <div
        v-if="!hasCheckoutData"
        class="rounded-3xl border border-slate-200 bg-white p-10 text-center dark:border-slate-700 dark:bg-slate-800"
      >
        <h2 class="text-2xl font-black text-brand-navy dark:text-white">
          Chưa có dữ liệu đặt vé
        </h2>
        <p class="mt-3 text-slate-500 dark:text-slate-300">
          Vui lòng quay lại trang chọn ghế để tiếp tục thanh toán.
        </p>
        <button
          type="button"
          @click="goBackToBooking"
          class="mt-6 inline-flex rounded-2xl bg-brand-orange px-6 py-3 text-sm font-black uppercase tracking-[0.18em] text-white transition hover:bg-orange-600"
        >
          Quay lại đặt vé
        </button>
      </div>

      <div v-else class="grid gap-8 lg:grid-cols-[1.3fr_0.9fr] items-start">
        <div class="space-y-6">
          <section class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <p class="text-sm font-black uppercase tracking-[0.3em] text-brand-orange">Liên hệ</p>
            <h2 class="mt-3 text-2xl font-black text-brand-navy dark:text-white">
              Thông tin nhận vé
            </h2>
            <div class="mt-6 grid gap-4 md:grid-cols-2">
              <label class="block md:col-span-2">
                <span class="text-xs font-black uppercase tracking-[0.18em] text-slate-500 dark:text-slate-300">Họ và tên</span>
                <input
                  v-model="contact.fullName"
                  type="text"
                  class="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-800 outline-none transition focus:border-brand-orange focus:ring-4 focus:ring-brand-orange/20 dark:border-slate-700 dark:bg-slate-900 dark:text-white"
                  placeholder="Nguyen Van A"
                />
              </label>

              <label class="block">
                <span class="text-xs font-black uppercase tracking-[0.18em] text-slate-500 dark:text-slate-300">Email</span>
                <input
                  v-model="contact.email"
                  type="email"
                  class="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-800 outline-none transition focus:border-brand-orange focus:ring-4 focus:ring-brand-orange/20 dark:border-slate-700 dark:bg-slate-900 dark:text-white"
                  placeholder="you@email.com"
                />
              </label>

              <label class="block">
                <span class="text-xs font-black uppercase tracking-[0.18em] text-slate-500 dark:text-slate-300">Số điện thoại</span>
                <input
                  v-model="contact.phone"
                  type="tel"
                  class="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-800 outline-none transition focus:border-brand-orange focus:ring-4 focus:ring-brand-orange/20 dark:border-slate-700 dark:bg-slate-900 dark:text-white"
                  placeholder="09xx xxx xxx"
                />
              </label>
            </div>
          </section>

          <section class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <p class="text-sm font-black uppercase tracking-[0.3em] text-brand-orange">Payment</p>
            <h2 class="mt-3 text-2xl font-black text-brand-navy dark:text-white">
              Phương thức thanh toán
            </h2>
            <div class="mt-6 grid gap-3">
              <label
                v-for="method in paymentMethods"
                :key="method.id"
                class="flex cursor-pointer items-start justify-between gap-4 rounded-2xl border p-4 transition"
                :class="paymentMethod === method.id
                  ? 'border-brand-orange bg-brand-orange/5'
                  : 'border-slate-200 hover:border-slate-300 dark:border-slate-700 dark:hover:border-slate-500'"
              >
                <div class="flex items-start gap-3">
                  <input
                    v-model="paymentMethod"
                    :value="method.id"
                    type="radio"
                    class="mt-1 h-4 w-4 accent-brand-orange"
                  />
                  <div>
                    <p class="font-black text-brand-navy dark:text-white">{{ method.label }}</p>
                    <p class="text-sm text-slate-500 dark:text-slate-300">{{ method.description }}</p>
                  </div>
                </div>
                <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-black uppercase tracking-[0.15em] text-slate-500 dark:bg-slate-900 dark:text-slate-300">
                  {{ method.badge }}
                </span>
              </label>
            </div>
          </section>

          <section class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <p class="text-sm font-black uppercase tracking-[0.3em] text-brand-orange">Voucher</p>
            <h2 class="mt-3 text-2xl font-black text-brand-navy dark:text-white">
              Mã giảm giá
            </h2>
            <div class="mt-5 flex flex-col gap-3 md:flex-row">
              <input
                v-model="voucherCodeInput"
                type="text"
                placeholder="Nhập mã ví dụ: WELCOME20"
                class="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-800 outline-none transition focus:border-brand-orange focus:ring-4 focus:ring-brand-orange/20 dark:border-slate-700 dark:bg-slate-900 dark:text-white"
              />
              <button
                type="button"
                @click="selectedVoucherCode = voucherCodeInput.trim().toUpperCase()"
                class="rounded-2xl bg-brand-navy px-6 py-3 text-sm font-black uppercase tracking-[0.18em] text-white transition hover:bg-sky-800"
              >
                Áp dụng
              </button>
              <button
                type="button"
                @click="clearVoucher"
                class="rounded-2xl border border-slate-300 px-6 py-3 text-sm font-black uppercase tracking-[0.18em] text-slate-600 transition hover:border-slate-400 dark:border-slate-600 dark:text-slate-200"
              >
                Xóa
              </button>
            </div>

            <p
              class="mt-4 text-sm font-semibold"
              :class="voucherValidation.state === 'valid'
                ? 'text-green-700 dark:text-green-400'
                : voucherValidation.state === 'invalid'
                  ? 'text-red-600 dark:text-red-400'
                  : 'text-slate-500 dark:text-slate-300'"
            >
              {{ voucherValidation.message }}
            </p>

            <div class="mt-6 grid gap-3 md:grid-cols-2">
              <button
                v-for="voucher in voucherCatalog"
                :key="voucher.code"
                type="button"
                @click="pickVoucher(voucher.code)"
                class="rounded-2xl border p-4 text-left transition"
                :class="activeVoucherCode === voucher.code
                  ? 'border-brand-orange bg-brand-orange/5'
                  : 'border-slate-200 hover:border-slate-300 dark:border-slate-700 dark:hover:border-slate-500'"
              >
                <p class="font-black text-brand-navy dark:text-white">{{ voucher.code }}</p>
                <p class="mt-1 text-sm text-slate-500 dark:text-slate-300">
                  <span v-if="voucher.discountType === 'PERCENT'">
                    Giảm {{ voucher.discountValue }}%, tối đa {{ formatMoney(voucher.maxDiscount || 0) }}
                  </span>
                  <span v-else>
                    Giảm trực tiếp {{ formatMoney(voucher.discountValue) }}
                  </span>
                </p>
                <p class="mt-2 text-xs font-bold uppercase tracking-[0.16em] text-slate-400">
                  Đơn từ {{ formatMoney(voucher.minOrderAmount) }}
                  <span v-if="voucher.membershipRequired">• Member only</span>
                </p>
              </button>
            </div>
          </section>

          <section class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <label class="flex items-start gap-3">
              <input v-model="agreedTerms" type="checkbox" class="mt-1 h-4 w-4 accent-brand-orange" />
              <span class="text-sm text-slate-600 dark:text-slate-300">
                Tôi xác nhận thông tin đặt vé là chính xác và đồng ý với điều khoản thanh toán của TicketRush.
              </span>
            </label>

            <button
              type="button"
              :disabled="isPayDisabled"
              @click="payNow"
              class="mt-6 w-full rounded-2xl px-6 py-4 text-sm font-black uppercase tracking-[0.2em] text-white transition"
              :class="isPayDisabled
                ? 'bg-slate-300 cursor-not-allowed'
                : 'bg-brand-orange hover:bg-orange-600'"
            >
              Xác nhận thanh toán
            </button>

            <div
              v-if="paymentSuccess"
              class="mt-4 rounded-2xl border border-green-200 bg-green-50 px-5 py-4 text-sm font-bold text-green-700"
            >
              Thanh toán giả lập thành công. Bước tiếp theo có thể là tạo `orders` và `order_items` từ backend API.
            </div>
          </section>
        </div>

        <aside class="lg:sticky lg:top-24 space-y-6">
          <section class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <p class="text-sm font-black uppercase tracking-[0.3em] text-brand-orange">Summary</p>
            <h2 class="mt-3 text-2xl font-black text-brand-navy dark:text-white">
              Đơn hàng của bạn
            </h2>

            <div class="mt-6 flex items-start gap-4">
              <img
                :src="checkoutData.movie?.poster"
                alt="Poster"
                class="h-24 w-16 rounded-xl object-cover"
              />
              <div>
                <p class="font-black text-brand-navy dark:text-white">{{ checkoutData.movie?.title }}</p>
                <p class="mt-1 text-sm text-slate-500 dark:text-slate-300">
                  {{ checkoutData.showtime?.weekday }}, ngày {{ checkoutData.showtime?.day }} {{ checkoutData.showtime?.month }}
                </p>
                <p class="text-sm text-slate-500 dark:text-slate-300">{{ checkoutData.showtime?.timeRange }}</p>
                <p class="text-sm text-slate-500 dark:text-slate-300">{{ checkoutData.showtime?.location }}</p>
              </div>
            </div>

            <div class="mt-6 rounded-2xl bg-slate-50 px-4 py-4 dark:bg-slate-900">
              <p class="text-xs font-black uppercase tracking-[0.18em] text-slate-500 dark:text-slate-300">
                Ghế đã chọn ({{ seatCount }})
              </p>
              <p class="mt-2 text-sm font-bold text-brand-navy dark:text-white">
                {{ seatLabel }}
              </p>
            </div>

            <div class="mt-6 space-y-3 text-sm">
              <div class="flex items-center justify-between text-slate-600 dark:text-slate-300">
                <span>Tạm tính vé</span>
                <span class="font-bold">{{ formatMoney(subtotal) }}</span>
              </div>
              <div class="flex items-center justify-between text-slate-600 dark:text-slate-300">
                <span>Phí nền tảng</span>
                <span class="font-bold">{{ formatMoney(serviceFee) }}</span>
              </div>
              <div class="flex items-center justify-between text-slate-600 dark:text-slate-300">
                <span>Membership discount</span>
                <span class="font-bold text-green-600">- {{ formatMoney(membershipDiscount) }}</span>
              </div>
              <div class="flex items-center justify-between text-slate-600 dark:text-slate-300">
                <span>Voucher discount</span>
                <span class="font-bold text-green-600">- {{ formatMoney(voucherDiscount) }}</span>
              </div>
            </div>

            <div class="mt-5 border-t border-slate-200 pt-5 dark:border-slate-700">
              <div class="flex items-center justify-between">
                <span class="text-sm font-black uppercase tracking-[0.18em] text-slate-500 dark:text-slate-300">
                  Tổng thanh toán
                </span>
                <span class="text-3xl font-black text-brand-orange">{{ formatMoney(grandTotal) }}</span>
              </div>
            </div>
          </section>

          <section class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <p class="text-xs font-black uppercase tracking-[0.2em] text-slate-500 dark:text-slate-300">
              Membership status
            </p>
            <p class="mt-2 text-xl font-black text-brand-navy dark:text-white">
              <span v-if="membershipLoading">Đang kiểm tra...</span>
              <span v-else-if="membership?.active">
                {{ membership.planName }} ({{ membership.discountPercent }}%)
              </span>
              <span v-else>Chưa có gói đang hoạt động</span>
            </p>
            <p class="mt-3 text-sm text-slate-500 dark:text-slate-300">
              Dữ liệu membership được lấy từ API `/me/membership` để mô phỏng đúng luồng dữ liệu backend hiện tại.
            </p>
          </section>
        </aside>
      </div>
    </section>
  </div>
</template>
