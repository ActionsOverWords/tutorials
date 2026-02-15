const usernameEl = document.getElementById("username");
const passwordEl = document.getElementById("password");
const tokenEl = document.getElementById("token");
const sessionResultEl = document.getElementById("session-result");
const loginResultEl = document.getElementById("login-result");
const apiResultEl = document.getElementById("api-result");

function parseBody(text) {
  try {
    return JSON.parse(text);
  } catch (_) {
    return text;
  }
}

function render(target, payload) {
  target.textContent = JSON.stringify(payload, null, 2);
}

function moveToKeycloakLogin() {
  window.location.assign("/oauth2/authorization/keycloak");
}

async function loginWithPassword() {
  const username = usernameEl.value.trim();
  const password = passwordEl.value;

  if (!username || !password) {
    render(loginResultEl, {
      status: 400,
      ok: false,
      body: "username/password를 입력하세요."
    });
    return;
  }

  const response = await fetch("/api/login/password", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password })
  });

  const text = await response.text();
  const body = parseBody(text);

  render(loginResultEl, {
    status: response.status,
    ok: response.ok,
    body
  });

  if (response.ok && body.accessToken) {
    tokenEl.value = body.accessToken;
  }
}

async function logoutSession() {
  window.location.assign("/logout");
}

async function updateSessionStatus() {
  const response = await fetch("/api/secure");
  const text = await response.text();
  const body = parseBody(text);

  render(sessionResultEl, {
    status: response.status,
    ok: response.ok,
    authentication: response.ok ? "authenticated" : "unauthenticated",
    body
  });
}

async function callApi(path, token) {
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(path, { headers });
  const text = await response.text();

  render(apiResultEl, {
    status: response.status,
    ok: response.ok,
    body: parseBody(text)
  });
}

document.getElementById("btn-login").addEventListener("click", () => {
  loginWithPassword();
});

document.getElementById("btn-oidc-login").addEventListener("click", () => {
  moveToKeycloakLogin();
});

document.getElementById("btn-logout").addEventListener("click", () => {
  logoutSession();
});

document.getElementById("btn-session-status").addEventListener("click", () => {
  updateSessionStatus();
});

document.getElementById("btn-public").addEventListener("click", () => {
  callApi("/api/public", "");
});

document.getElementById("btn-secure").addEventListener("click", () => {
  callApi("/api/secure", tokenEl.value.trim());
});

updateSessionStatus();
