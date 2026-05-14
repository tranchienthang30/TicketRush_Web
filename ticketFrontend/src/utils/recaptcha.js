const SITE_KEY = import.meta.env.VITE_RECAPTCHA_SITE_KEY;
let scriptPromise;

export async function getRecaptchaToken(action) {
  if (!SITE_KEY) {
    return null;
  }

  await loadRecaptchaScript();

  return new Promise((resolve, reject) => {
    window.grecaptcha.ready(() => {
      window.grecaptcha
        .execute(SITE_KEY, { action })
        .then(resolve)
        .catch(reject);
    });
  });
}

function loadRecaptchaScript() {
  if (window.grecaptcha) {
    return Promise.resolve();
  }

  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement("script");
      script.src = `https://www.google.com/recaptcha/api.js?render=${SITE_KEY}`;
      script.async = true;
      script.defer = true;
      script.onload = resolve;
      script.onerror = reject;
      document.head.appendChild(script);
    });
  }

  return scriptPromise;
}
