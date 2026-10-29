import { apiRequest, clearAccessToken, getAccessToken, saveAccessToken } from "./api.js";

const page = document.body.dataset.page;
let profilePromise;
let adminSpaces = [];

function getCurrentProfile() {
  if (!profilePromise) {
    profilePromise = apiRequest("/api/v1/users/me", { authenticated: true }).catch(() => null);
  }
  return profilePromise;
}

function showMessage(element, message, kind = "info") {
  if (!element) return;
  element.textContent = message;
  element.classList.toggle("feedback-error", kind === "error");
  element.classList.toggle("feedback-success", kind === "success");
  element.hidden = !message;
}

function ensureLogoutDialog() {
  const existingDialog = document.querySelector("[data-logout-dialog]");
  if (existingDialog) return existingDialog;

  const dialog = document.createElement("dialog");
  dialog.className = "agenda-dialog logout-dialog";
  dialog.dataset.logoutDialog = "";
  dialog.setAttribute("aria-labelledby", "logout-dialog-title");
  dialog.setAttribute("aria-describedby", "logout-dialog-description");

  const heading = makeElement("div", "section-heading");
  const copy = document.createElement("div");
  const title = makeElement("h2", "", titleCase("Confirmar cierre de sesión"));
  title.id = "logout-dialog-title";
  const description = makeElement("p", "", "¿Quieres salir de tu cuenta de Reserva Campus?");
  description.id = "logout-dialog-description";
  copy.append(title, description);
  const cancel = makeElement("button", "button button-outline", "Continuar");
  cancel.type = "button";
  cancel.dataset.logoutCancel = "";
  heading.append(copy, cancel);

  const form = document.createElement("form");
  form.method = "dialog";
  const actions = makeElement("div", "form-actions logout-confirm-actions");
  const confirm = makeElement("button", "button button-danger", "Cerrar sesión");
  confirm.type = "submit";
  confirm.value = "logout";
  actions.append(confirm);
  form.append(actions);
  dialog.append(heading, form);
  document.body.append(dialog);
  return dialog;
}

async function updateNavigation() {
  const signedIn = Boolean(getAccessToken());
  const logoutButtons = document.querySelectorAll("[data-logout-button]");
  const logoutDialog = logoutButtons.length ? ensureLogoutDialog() : null;
  document.querySelectorAll("[data-login-link]").forEach((link) => { link.hidden = signedIn; });
  document.querySelectorAll("[data-profile-link]").forEach((link) => { link.hidden = !signedIn; });
  document.querySelectorAll("[data-admin-link]").forEach((link) => { link.hidden = true; });
  logoutButtons.forEach((button) => {
    button.hidden = !signedIn;
    button.addEventListener("click", () => logoutDialog?.showModal());
  });
  document.querySelector("[data-logout-cancel]")?.addEventListener("click", () => logoutDialog?.close("cancel"));
  logoutDialog?.addEventListener("close", () => {
    if (logoutDialog.returnValue !== "logout") return;
    clearAccessToken();
    window.location.replace("index.html");
  });
  if (signedIn) {
    const profile = await getCurrentProfile();
    const roles = profile?.roles || [];
    const canManage = roles.includes("ADMINISTRATOR") || roles.includes("MANAGER");
    document.querySelectorAll("[data-admin-link]").forEach((link) => {
      link.hidden = !canManage;
      if (roles.includes("ADMINISTRATOR")) link.textContent = "Administración";
      else if (canManage) link.textContent = "Responsable";
    });
  }
}

function profileRoleName(role) {
  const names = {
    ADMINISTRATOR: "Administrador",
    MANAGER: "Responsable de espacios",
    USER: "Usuario",
  };
  return names[role] || role.replaceAll("_", " ");
}

async function loadProfilePage() {
  const feedback = document.querySelector("[data-profile-feedback]");
  const content = document.querySelector("[data-profile-content]");
  const signInPrompt = document.querySelector("[data-profile-signin]");
  if (!getAccessToken()) {
    feedback.hidden = true;
    signInPrompt.hidden = false;
    return;
  }

  try {
    const profile = await getCurrentProfile();
    if (!profile) throw new Error("No pudimos cargar tu perfil. Verifica la conexión con el backend y vuelve a iniciar sesión si tu sesión expiró.");

    const name = titleCase(profile.name || "Usuario");
    const email = profile.email || "Sin correo registrado";
    const active = profile.status === "ACTIVE";
    const initials = name.trim().split(/\s+/).slice(0, 2)
      .map((part) => Array.from(part)[0]?.toLocaleUpperCase("es-CO") || "").join("");
    document.querySelector("[data-profile-avatar]").textContent = initials || "RC";
    document.querySelector("[data-profile-name]").textContent = name;
    document.querySelector("[data-profile-email]").textContent = email;
    document.querySelector("[data-profile-status]").textContent = active ? "Cuenta activa" : "Cuenta inactiva";
    document.querySelector("[data-profile-status]").classList.toggle("is-inactive", !active);
    document.querySelector("[data-profile-detail-name]").textContent = name;
    document.querySelector("[data-profile-detail-email]").textContent = email;
    document.querySelector("[data-profile-detail-status]").textContent = active ? "Activa" : "Inactiva";

    const roles = Array.isArray(profile.roles) ? profile.roles : [];
    const roleList = document.querySelector("[data-profile-roles]");
    roleList.replaceChildren(...(roles.length ? roles : ["Sin roles asignados"]).map((role) => {
      return makeElement("span", "profile-role", roles.length ? titleCase(profileRoleName(role)) : titleCase(role));
    }));
    feedback.hidden = true;
    content.hidden = false;
  } catch (error) {
    showMessage(feedback, error.message, "error");
    signInPrompt.hidden = false;
  }
}

function makeElement(tag, className, text) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text !== undefined) element.textContent = text;
  return element;
}

function titleCase(value) {
  return String(value ?? "")
    .toLocaleLowerCase("es-CO")
    .replace(/(^|[^\p{L}\p{M}])(\p{L})/gu, (_, separator, letter) =>
      `${separator}${letter.toLocaleUpperCase("es-CO")}`);
}

function formatDate(value) {
  return new Intl.DateTimeFormat("es-CO", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
    timeZone: "America/Bogota",
  }).format(new Date(value));
}

function bogotaDateString(value = new Date()) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Bogota",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(value);
  const part = (type) => parts.find((item) => item.type === type)?.value;
  return `${part("year")}-${part("month")}-${part("day")}`;
}

function availableAgendaSlots(date, entries, opensAt, closesAt) {
  if (!opensAt || !closesAt) return [];
  // The API interprets agenda dates in America/Bogota, which currently uses UTC-5 year-round.
  const halfHour = 30 * 60 * 1000;
  const dayStart = new Date(`${date}T${String(opensAt).slice(0, 8)}-05:00`).getTime();
  const dayEnd = new Date(`${date}T${String(closesAt).slice(0, 8)}-05:00`).getTime();
  if (!Number.isFinite(dayStart) || !Number.isFinite(dayEnd) || dayStart >= dayEnd) return [];
  const now = Date.now();
  let cursor = date === bogotaDateString(new Date(now))
    ? Math.max(dayStart, Math.floor(now / halfHour) * halfHour + halfHour)
    : dayStart;
  const occupied = entries
    .map((entry) => ({
      start: Math.max(dayStart, new Date(entry.startAt).getTime()),
      end: Math.min(dayEnd, new Date(entry.endAt).getTime()),
    }))
    .filter((period) => period.start < period.end)
    .sort((first, second) => first.start - second.start);
  const slots = [];

  for (const period of occupied) {
    if (period.end <= cursor) continue;
    const slotStart = Math.ceil(cursor / halfHour) * halfHour;
    const slotEnd = Math.floor(period.start / halfHour) * halfHour;
    if (slotStart < slotEnd) slots.push({ start: slotStart, end: slotEnd });
    cursor = Math.max(cursor, period.end);
    if (cursor >= dayEnd) break;
  }

  const finalSlotStart = Math.ceil(cursor / halfHour) * halfHour;
  if (finalSlotStart < dayEnd) slots.push({ start: finalSlotStart, end: dayEnd });
  return slots;
}

function spaceScheduleSummary(schedules = []) {
  const dayNames = [
    ["MONDAY", "Lun"], ["TUESDAY", "Mar"], ["WEDNESDAY", "Mié"], ["THURSDAY", "Jue"],
    ["FRIDAY", "Vie"], ["SATURDAY", "Sáb"], ["SUNDAY", "Dom"],
  ];
  const order = new Map(dayNames.map(([day], index) => [day, index]));
  const entries = schedules.map((schedule) => ({
    ...schedule,
    index: order.get(schedule.dayOfWeek),
    opensAt: String(schedule.opensAt || "").slice(0, 5),
    closesAt: String(schedule.closesAt || "").slice(0, 5),
  })).filter((schedule) => schedule.index !== undefined)
    .sort((first, second) => first.index - second.index);
  if (!entries.length) return "Sin horario configurado";

  const groups = [];
  for (const schedule of entries) {
    const previous = groups.at(-1);
    if (previous && previous.closesAt === schedule.closesAt && previous.opensAt === schedule.opensAt
      && previous.endIndex + 1 === schedule.index) {
      previous.endIndex = schedule.index;
    } else {
      groups.push({ startIndex: schedule.index, endIndex: schedule.index,
        opensAt: schedule.opensAt, closesAt: schedule.closesAt });
    }
  }
  return groups.map((group) => {
    const first = dayNames[group.startIndex][1];
    const last = dayNames[group.endIndex][1];
    const days = first === last ? first : `${first}–${last}`;
    return `${days} ${group.opensAt}–${group.closesAt}`;
  }).join(" · ");
}

function formatAgendaTime(timestamp, isDayEnd = false) {
  if (isDayEnd) return "23:59";
  return new Intl.DateTimeFormat("es-CO", {
    timeZone: "America/Bogota",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).format(new Date(timestamp));
}

function renderSpaceCard(space) {
  const article = makeElement("article", "card space-card");
  const banner = makeElement("div", "space-banner", titleCase(space.type?.name || "Espacio institucional"));
  const body = makeElement("div", "card-body");
  body.append(makeElement("h3", "", titleCase(space.name)));
  if (space.description) body.append(makeElement("p", "", space.description));
  const metadata = makeElement("div", "space-meta");
  metadata.append(makeElement("span", "", space.location));
  metadata.append(makeElement("span", "", `${space.capacity} personas`));
  metadata.append(makeElement("span", "space-hours", `Horario: ${spaceScheduleSummary(space.schedules)}`));
  body.append(metadata);
  body.append(makeElement("span", "status", "Espacio activo"));

  const link = makeElement("a", "button button-outline", "Solicitar reserva");
  link.href = `reservations.html?space=${encodeURIComponent(space.id)}`;
  body.append(link);
  if (page === "spaces") {
    const agendaButton = makeElement("button", "button button-outline", "Consultar agenda");
    agendaButton.type = "button";
    agendaButton.addEventListener("click", () => {
      const dialog = document.querySelector("[data-space-agenda-dialog]");
      const agendaForm = document.querySelector("[data-space-agenda-form]");
      agendaForm.elements.spaceId.value = space.id;
      agendaForm.elements.date.value = bogotaDateString(new Date());
      agendaForm.elements.date.min = bogotaDateString(new Date());
      document.querySelector("[data-space-agenda-title]").textContent = titleCase(`Agenda · ${space.name}`);
      dialog.showModal();
      agendaForm.requestSubmit();
    });
    body.append(agendaButton);
  }
  article.append(banner, body);
  return article;
}

async function loadHome() {
  updateHeroCalendar();
  const container = document.querySelector("[data-featured-spaces]");
  try {
    const spaces = await apiRequest("/api/v1/spaces");
    container.replaceChildren(...spaces.slice(0, 3).map(renderSpaceCard));
    if (!spaces.length) showMessage(container, "Aún no hay espacios activos en el catálogo.");
  } catch (error) {
    showMessage(container, error.message, "error");
  }
}

function updateHeroCalendar() {
  const title = document.querySelector("[data-calendar-title]");
  const days = document.querySelector("[data-calendar-days]");
  if (!title || !days) return;

  const dateParts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Bogota",
    year: "numeric",
    month: "numeric",
    day: "numeric",
  }).formatToParts(new Date());
  const values = Object.fromEntries(dateParts.map(({ type, value }) => [type, value]));
  const today = new Date(Date.UTC(Number(values.year), Number(values.month) - 1, Number(values.day)));
  const weekStart = new Date(today);
  weekStart.setUTCDate(today.getUTCDate() - ((today.getUTCDay() + 6) % 7));
  const monthYear = new Intl.DateTimeFormat("es-CO", {
    timeZone: "UTC",
    month: "long",
    year: "numeric",
  }).format(today);
  title.textContent = titleCase(`Semana actual · ${monthYear}`);

  const dayFormatter = new Intl.DateTimeFormat("es-CO", {
    timeZone: "UTC",
    weekday: "long",
    day: "numeric",
    month: "long",
  });
  days.replaceChildren(...Array.from({ length: 7 }, (_, index) => {
    const date = new Date(weekStart);
    date.setUTCDate(weekStart.getUTCDate() + index);
    const cell = document.createElement("span");
    cell.textContent = String(date.getUTCDate());
    cell.setAttribute("aria-label", dayFormatter.format(date));
    if (date.getTime() === today.getTime()) cell.classList.add("active");
    return cell;
  }));
}

async function loadSpaceCatalog() {
  const form = document.querySelector("[data-space-filters]");
  const availabilityForm = document.querySelector("[data-availability-form]");
  const result = document.querySelector("[data-space-results]");
  const feedback = document.querySelector("[data-feedback]");
  const agendaDialog = document.querySelector("[data-space-agenda-dialog]");
  const agendaForm = document.querySelector("[data-space-agenda-form]");
  const agendaFeedback = document.querySelector("[data-space-agenda-feedback]");
  const agendaEntries = document.querySelector("[data-space-agenda-entries]");
  const typeSelect = form.elements.type;
  const searchParams = new URLSearchParams(window.location.search);
  form.elements.search.value = searchParams.get("query") || "";
  form.elements.capacity.value = searchParams.get("minimumCapacity") || "";

  try {
    const types = await apiRequest("/api/v1/space-types");
    for (const type of types) typeSelect.add(new Option(titleCase(type.name), type.name));
    typeSelect.value = searchParams.get("type") || "";
  } catch (error) {
    showMessage(feedback, error.message, "error");
  }

  const renderResults = async () => {
    const params = new URLSearchParams();
    if (form.elements.search.value.trim()) params.set("query", form.elements.search.value.trim());
    if (typeSelect.value) params.set("type", typeSelect.value);
    if (form.elements.capacity.value) params.set("minimumCapacity", form.elements.capacity.value);
    const suffix = params.size ? `?${params}` : "";
    window.history.replaceState(null, "", `${window.location.pathname}${suffix}`);
    showMessage(feedback, "Cargando catálogo…");
    try {
      const spaces = await apiRequest(`/api/v1/spaces${suffix}`);
      result.replaceChildren(...spaces.map(renderSpaceCard));
      showMessage(feedback, spaces.length ? `${spaces.length} espacio(s) encontrado(s).` : "No encontramos espacios con esos filtros.");
    } catch (error) {
      result.replaceChildren();
      showMessage(feedback, error.message, "error");
    }
  };

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    renderResults();
  });
  agendaForm?.addEventListener("submit", (event) => {
    event.preventDefault();
    loadSpaceAgenda();
  });
  document.querySelector("[data-agenda-close]")?.addEventListener("click", () => agendaDialog.close());

  async function loadSpaceAgenda() {
    const spaceId = agendaForm.elements.spaceId.value;
    const date = agendaForm.elements.date.value;
    if (!spaceId || !date) return;
    const submit = agendaForm.querySelector("button[type=submit]");
    submit.disabled = true;
    showMessage(agendaFeedback, "Cargando agenda…");
    agendaEntries.replaceChildren();
    try {
      const params = new URLSearchParams({ date });
      const agenda = await apiRequest(`/api/v1/spaces/${spaceId}/agenda?${params}`);
      const slots = availableAgendaSlots(date, agenda.entries, agenda.opensAt, agenda.closesAt);
      if (!slots.length) {
        showMessage(agendaFeedback, agenda.opensAt
          ? "No hay horarios disponibles para reservar en esta fecha."
          : "Este espacio no tiene atención programada para el día seleccionado.");
        return;
      }
      const slotCount = slots.length === 1 ? "1 franja disponible" : `${slots.length} franjas disponibles`;
      showMessage(agendaFeedback,
        `Atención ${String(agenda.opensAt).slice(0, 5)}–${String(agenda.closesAt).slice(0, 5)} · ${slotCount} para reservar.`);
      slots.forEach((slot) => {
        const item = makeElement("article", "admin-list-item agenda-slot");
        const details = makeElement("div", "admin-list-details");
        details.append(makeElement("h3", "", "Disponible para reservar"));
        details.append(makeElement("p", "", `${formatAgendaTime(slot.start)} – ${formatAgendaTime(slot.end)}`));
        item.append(details);
        agendaEntries.append(item);
      });
    } catch (error) {
      showMessage(agendaFeedback, error.message, "error");
    } finally {
      submit.disabled = false;
    }
  }

  if (availabilityForm) {
    const startInput = availabilityForm.elements.startAt;
    const endInput = availabilityForm.elements.endAt;
    const availabilityResults = document.querySelector("[data-availability-results]");
    const availabilityFeedback = document.querySelector("[data-availability-feedback]");
    setLocalDateMinimum(startInput);
    setLocalDateMinimum(endInput);

    availabilityForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      const startAt = new Date(startInput.value);
      const endAt = new Date(endInput.value);
      if (!Number.isFinite(startAt.getTime()) || !Number.isFinite(endAt.getTime()) || startAt >= endAt) {
        showMessage(availabilityFeedback, "La hora de inicio debe ser anterior a la hora de finalización.", "error");
        availabilityResults.replaceChildren();
        return;
      }

      const submit = availabilityForm.querySelector("button[type=submit]");
      submit.disabled = true;
      availabilityResults.replaceChildren();
      showMessage(availabilityFeedback, "Buscando espacios disponibles…");
      const params = new URLSearchParams({ startAt: startAt.toISOString(), endAt: endAt.toISOString() });
      if (availabilityForm.elements.minimumCapacity.value) {
        params.set("minimumCapacity", availabilityForm.elements.minimumCapacity.value);
      }
      try {
        const spaces = await apiRequest(`/api/v1/spaces/availability?${params}`);
        availabilityResults.replaceChildren(...spaces.map(renderSpaceCard));
        showMessage(availabilityFeedback, spaces.length
          ? `${spaces.length} espacio(s) disponible(s) para ese horario.`
          : "No hay espacios disponibles para ese horario.");
      } catch (error) {
        showMessage(availabilityFeedback, error.message, "error");
      } finally {
        submit.disabled = false;
      }
    });
  }

  await renderResults();
}

function configureAuthForm() {
  const form = document.querySelector("[data-auth-form]");
  const nameField = form.querySelector("[data-name-field]");
  const nameInput = form.elements.name;
  const passwordInput = form.elements.password;
  const confirmField = form.querySelector("[data-confirm-password-field]");
  const confirmInput = form.elements.confirmPassword;
  const passwordRequirements = form.querySelector("[data-password-requirements]");
  const passwordRuleItems = [...form.querySelectorAll("[data-password-rule]")];
  const passwordStrength = form.querySelector("[data-password-strength]");
  const passwordStrengthLabel = form.querySelector("[data-password-strength-label]");
  const passwordStrengthMeter = form.querySelector("[data-password-strength-meter]");
  const passwordStrengthSegments = [...form.querySelectorAll("[data-strength-segment]")];
  const passwordMatchHint = form.querySelector("[data-password-match-hint]");
  const title = form.querySelector("[data-auth-title]");
  const description = form.querySelector("[data-auth-description]");
  const submit = form.querySelector("[data-auth-submit]");
  const toggle = form.querySelector("[data-auth-toggle]");
  const feedback = form.querySelector("[data-feedback]");
  let registering = false;

  function prepareAnimatedPanel(panel) {
    panel.hidden = false;
    panel.inert = true;
    panel.setAttribute("aria-hidden", "true");
    panel.dataset.wantsVisible = "false";
    panel.dataset.visibilityGeneration = "0";
  }

  function setAnimatedPanelVisibility(panel, visible) {
    const nextState = String(visible);
    if (panel.dataset.wantsVisible === nextState) return;
    panel.dataset.wantsVisible = nextState;
    const generation = String(Number(panel.dataset.visibilityGeneration) + 1);
    panel.dataset.visibilityGeneration = generation;

    if (visible) {
      panel.hidden = false;
      requestAnimationFrame(() => {
        if (panel.dataset.visibilityGeneration === generation && panel.dataset.wantsVisible === "true") {
          panel.classList.add("is-visible");
          panel.inert = false;
          panel.setAttribute("aria-hidden", "false");
        }
      });
      return;
    }

    panel.inert = true;
    panel.setAttribute("aria-hidden", "true");
    panel.classList.remove("is-visible");
  }

  prepareAnimatedPanel(passwordRequirements);
  prepareAnimatedPanel(passwordStrength);

  form.querySelectorAll("[data-password-toggle]").forEach((button) => {
    const input = form.elements[button.dataset.passwordToggle];
    button.addEventListener("click", () => {
      const visible = input.type === "password";
      input.type = visible ? "text" : "password";
      button.classList.toggle("is-visible", visible);
      button.setAttribute("aria-pressed", String(visible));
      button.setAttribute("aria-label", visible ? "Ocultar contraseña" : "Mostrar contraseña");
    });
  });

  function passwordRuleStatus(value) {
    return {
      length: value.length >= 12,
      uppercase: /\p{Lu}/u.test(value),
      lowercase: /\p{Ll}/u.test(value),
      number: /\p{N}/u.test(value),
      special: /[^\p{L}\p{N}\s]/u.test(value),
    };
  }

  function updatePasswordRequirements() {
    const status = passwordRuleStatus(passwordInput.value);
    passwordRuleItems.forEach((item) => {
      const isMet = status[item.dataset.passwordRule];
      item.classList.toggle("is-met", isMet);
      item.classList.toggle("is-unmet", !isMet);
    });
    const allRequirementsMet = Object.values(status).every(Boolean);
    const hasStartedTyping = passwordInput.value.length > 0;
    setAnimatedPanelVisibility(passwordRequirements, registering && hasStartedTyping && !allRequirementsMet);
    setAnimatedPanelVisibility(passwordStrength, registering && allRequirementsMet);

    if (allRequirementsMet) {
      const level = passwordInput.value.length >= 20 ? 3 : passwordInput.value.length >= 16 ? 2 : 1;
      const labels = ["", "Buena", "Fuerte", "Muy fuerte"];
      const levelLabel = labels[level];
      const levelName = ["", "good", "strong", "very-strong"][level];
      passwordStrength.dataset.level = levelName;
      passwordStrengthLabel.textContent = levelLabel;
      passwordStrengthMeter.setAttribute("aria-valuenow", String(level));
      passwordStrengthMeter.setAttribute("aria-valuetext", levelLabel);
      passwordStrengthSegments.forEach((segment, index) => {
        segment.classList.toggle("is-active", index < level);
      });
    } else {
      passwordStrengthSegments.forEach((segment) => segment.classList.remove("is-active"));
    }
    return allRequirementsMet;
  }

  function updatePasswordMatch() {
    const hasConfirmation = confirmInput.value.length > 0;
    const matches = hasConfirmation && passwordInput.value === confirmInput.value;
    passwordMatchHint.textContent = !hasConfirmation
      ? ""
      : matches ? "Las contraseñas coinciden." : "Las contraseñas no coinciden.";
    passwordMatchHint.classList.toggle("is-success", matches);
    passwordMatchHint.classList.toggle("is-error", hasConfirmation && !matches);
  }

  function concealPasswords() {
    form.querySelectorAll("[data-password-toggle]").forEach((button) => {
      const input = form.elements[button.dataset.passwordToggle];
      input.type = "password";
      button.classList.remove("is-visible");
      button.setAttribute("aria-pressed", "false");
      button.setAttribute("aria-label", "Mostrar contraseña");
    });
  }

  passwordInput.addEventListener("input", () => {
    updatePasswordRequirements();
    if (registering) updatePasswordMatch();
  });
  confirmInput.addEventListener("input", updatePasswordMatch);

  toggle.addEventListener("click", () => {
    registering = !registering;
    nameField.hidden = !registering;
    nameInput.required = registering;
    confirmField.hidden = !registering;
    confirmInput.required = registering;
    confirmInput.value = "";
    passwordRequirements.hidden = !registering;
    concealPasswords();
    passwordInput.autocomplete = registering ? "new-password" : "current-password";
    confirmInput.autocomplete = "new-password";
    updatePasswordRequirements();
    updatePasswordMatch();
    title.textContent = titleCase(registering ? "Crea tu cuenta" : "Te damos la bienvenida");
    description.textContent = registering ? "Regístrate para solicitar y consultar tus reservas." : "Inicia sesión para solicitar, consultar o administrar tus reservas.";
    submit.textContent = registering ? "Crear cuenta" : "Iniciar sesión";
    toggle.textContent = registering ? "Ya tengo una cuenta" : "Crear una cuenta";
    showMessage(feedback, "");
  });

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (registering) {
      const requirementsMet = updatePasswordRequirements();
      if (!requirementsMet) {
        showMessage(feedback, "La contraseña debe cumplir todos los requisitos indicados.", "error");
        passwordInput.focus();
        return;
      }
      if (passwordInput.value !== confirmInput.value) {
        updatePasswordMatch();
        showMessage(feedback, "Las contraseñas no coinciden.", "error");
        confirmInput.focus();
        return;
      }
    }
    submit.disabled = true;
    showMessage(feedback, registering ? "Creando cuenta…" : "Iniciando sesión…");
    const credentials = { email: form.elements.email.value.trim(), password: passwordInput.value };
    try {
      const result = await apiRequest(registering ? "/api/v1/auth/register" : "/api/v1/auth/login", {
        method: "POST",
        body: registering ? { ...credentials, name: nameInput.value.trim() } : credentials,
      });
      saveAccessToken(result.accessToken);
      const requestedPage = new URLSearchParams(window.location.search).get("next");
      const next = requestedPage && /^[a-z0-9_-]+\.html$/i.test(requestedPage) ? requestedPage : "reservations.html";
      window.location.href = next;
    } catch (error) {
      showMessage(feedback, error.message, "error");
      submit.disabled = false;
    }
  });
}

function setLocalDateMinimum(input) {
  const now = new Date(Date.now() + 60000);
  now.setSeconds(0, 0);
  if (now.getTime() < Date.now() + 60000) now.setMinutes(now.getMinutes() + 1);
  const localMinimum = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  input.min = localMinimum.toISOString().slice(0, 16);
}

function setDateMinimum(input) {
  input.min = localDateString(new Date());
}

function populateHalfHourTimes(select) {
  select.replaceChildren(new Option("Selecciona una hora", ""));
  for (let hour = 0; hour < 24; hour += 1) {
    for (const minute of [0, 30]) {
      const time = `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
      select.add(new Option(time, time));
    }
  }
}

function nextHalfHourDateTime() {
  const threshold = Date.now() + 60000;
  const next = new Date(threshold);
  next.setSeconds(0, 0);
  if (next.getTime() < threshold) next.setMinutes(next.getMinutes() + 1);
  const remainder = next.getMinutes() % 30;
  if (remainder) next.setMinutes(next.getMinutes() + 30 - remainder);
  return {
    date: localDateString(next),
    time: `${String(next.getHours()).padStart(2, "0")}:${String(next.getMinutes()).padStart(2, "0")}`,
  };
}

async function loadReservationPage() {
  const form = document.querySelector("[data-reservation-form]");
  const select = form.elements.space;
  const formFeedback = form.querySelector("[data-feedback]");
  const historyFeedback = document.querySelector("[data-history-feedback]");
  const historyFilterForm = document.querySelector("[data-history-filter-form]");
  const reservationList = document.querySelector("[data-reservation-list]");
  const sessionNote = document.querySelector("[data-session-note]");
  const dateInput = form.elements.date;
  const startTimeSelect = form.elements.startTime;
  const endTimeSelect = form.elements.endTime;
  const scheduleHint = form.querySelector("[data-reservation-schedule-hint]");
  let reservationSpaces = [];
  setDateMinimum(dateInput);
  [startTimeSelect, endTimeSelect].forEach((select) => populateHalfHourTimes(select));
  const updateReservationTimes = () => {
    const weekdayNames = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
    const selectedSpace = reservationSpaces.find((space) => String(space.id) === select.value);
    const dayIndex = dateInput.value ? new Date(`${dateInput.value}T12:00:00`).getDay() : -1;
    const schedule = selectedSpace?.schedules?.find((item) => item.dayOfWeek === weekdayNames[dayIndex]);
    scheduleHint.textContent = schedule
      ? `Horario de atención para este día: ${String(schedule.opensAt).slice(0, 5)}–${String(schedule.closesAt).slice(0, 5)}.`
      : selectedSpace && dateInput.value
        ? "Este espacio no tiene atención programada para el día seleccionado."
        : "Selecciona un espacio y una fecha para consultar el horario disponible.";
    const minimum = nextHalfHourDateTime();
    [startTimeSelect, endTimeSelect].forEach((timeSelect) => {
      [...timeSelect.options].forEach((option) => {
        if (!option.value) return;
        const tooEarly = dateInput.value < minimum.date
          || (dateInput.value === minimum.date && option.value < minimum.time);
        const opensAt = String(schedule?.opensAt || "").slice(0, 5);
        const closesAt = String(schedule?.closesAt || "").slice(0, 5);
        const outsideSchedule = !schedule || (timeSelect === startTimeSelect
          ? option.value < opensAt || option.value >= closesAt
          : option.value <= opensAt || option.value > closesAt);
        const wrongOrder = timeSelect === startTimeSelect
          ? endTimeSelect.value && option.value >= endTimeSelect.value
          : startTimeSelect.value && option.value <= startTimeSelect.value;
        option.disabled = tooEarly || outsideSchedule || Boolean(wrongOrder);
      });
      if (timeSelect.selectedOptions[0]?.disabled) timeSelect.value = "";
    });
  };
  updateReservationTimes();
  dateInput.addEventListener("focus", () => setDateMinimum(dateInput));
  dateInput.addEventListener("change", updateReservationTimes);
  select.addEventListener("change", updateReservationTimes);
  [startTimeSelect, endTimeSelect].forEach((timeSelect) => {
    timeSelect.addEventListener("focus", updateReservationTimes);
    timeSelect.addEventListener("change", updateReservationTimes);
  });

  try {
    const spaces = await apiRequest("/api/v1/spaces");
    reservationSpaces = spaces;
    select.replaceChildren(new Option("Selecciona un espacio", ""));
    for (const space of spaces) select.add(new Option(`${titleCase(space.name)} · ${space.capacity} personas`, space.id));
    select.value = new URLSearchParams(window.location.search).get("space") || "";
    updateReservationTimes();
  } catch (error) {
    select.replaceChildren(new Option("No se pudo cargar el catálogo", ""));
    showMessage(formFeedback, error.message, "error");
  }

  if (getAccessToken()) {
    historyFilterForm.hidden = false;
    showMessage(sessionNote, "Sesión iniciada. El servicio validará disponibilidad, capacidad y permisos.");
    await loadMyReservations();
  } else {
    showMessage(sessionNote, "Inicia sesión para crear y consultar tus reservas.");
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!getAccessToken()) {
      window.location.href = "login.html?next=reservations.html";
      return;
    }
    setDateMinimum(dateInput);
    updateReservationTimes();
    if (!startTimeSelect.value || !endTimeSelect.value) {
      const selectedSpace = reservationSpaces.find((space) => String(space.id) === select.value);
      const weekdayNames = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
      const dayIndex = dateInput.value ? new Date(`${dateInput.value}T12:00:00`).getDay() : -1;
      const hasSchedule = selectedSpace?.schedules?.some((item) => item.dayOfWeek === weekdayNames[dayIndex]);
      showMessage(formFeedback, hasSchedule
        ? "Selecciona horas dentro del horario del espacio y en intervalos de 30 minutos."
        : "El espacio no tiene atención programada para el día seleccionado.", "error");
      return;
    }
    const startAt = new Date(`${dateInput.value}T${startTimeSelect.value}`);
    const endAt = new Date(`${dateInput.value}T${endTimeSelect.value}`);
    if (!Number.isFinite(startAt.getTime()) || !Number.isFinite(endAt.getTime()) || startAt >= endAt) {
      showMessage(formFeedback, "La hora de inicio debe ser anterior a la finalización.", "error");
      return;
    }
    if (startAt <= new Date()) {
      showMessage(formFeedback, "La hora de inicio debe ser posterior a la hora actual. Selecciona otro horario.", "error");
      return;
    }
    if (![startTimeSelect.value, endTimeSelect.value].every((time) => /^\d{2}:(00|30)$/.test(time))) {
      showMessage(formFeedback, "Las horas de inicio y finalización deben estar en intervalos de 30 minutos (00 o 30).", "error");
      return;
    }

    const submit = form.querySelector("button[type=submit]");
    submit.disabled = true;
    showMessage(formFeedback, "Enviando solicitud…");
    try {
      await apiRequest("/api/v1/reservations", {
        method: "POST",
        authenticated: true,
        body: {
          spaceId: select.value,
          startAt: startAt.toISOString(),
          endAt: endAt.toISOString(),
          purpose: form.elements.purpose.value.trim(),
          attendees: Number(form.elements.attendees.value),
        },
      });
      form.reset();
      setDateMinimum(dateInput);
      updateReservationTimes();
      showMessage(formFeedback, "Reserva creada. Puedes consultar su estado en Mis reservas.", "success");
      await loadMyReservations();
    } catch (error) {
      const message = error.message.includes("Start time must be in the future")
        ? "La hora de inicio debe ser posterior a la hora actual. Selecciona otro horario."
        : error.message.includes("End time must be in the future")
          ? "La hora de finalización debe ser posterior a la hora actual."
          : error.message;
      showMessage(formFeedback, message, "error");
    } finally {
      submit.disabled = false;
    }
  });

  historyFilterForm?.addEventListener("submit", (event) => {
    event.preventDefault();
    loadMyReservations();
  });

  async function loadMyReservations() {
    if (!getAccessToken()) return;
    const fromDate = historyFilterForm.elements.from.value;
    const toDate = historyFilterForm.elements.to.value;
    if (fromDate && toDate && fromDate > toDate) {
      reservationList.replaceChildren();
      showMessage(historyFeedback, "La fecha inicial debe ser anterior o igual a la final.", "error");
      return;
    }
    const params = new URLSearchParams();
    if (historyFilterForm.elements.status.value) params.set("status", historyFilterForm.elements.status.value);
    if (fromDate) params.set("startAt", localDayBounds(fromDate).startAt);
    if (toDate) params.set("endAt", localDayBounds(toDate).endAt);
    showMessage(historyFeedback, "Cargando reservas…");
    try {
      const suffix = params.size ? `?${params}` : "";
      const reservations = await apiRequest(`/api/v1/reservations/me${suffix}`, { authenticated: true });
      reservationList.replaceChildren();
      if (!reservations.length) {
        showMessage(historyFeedback, "Todavía no tienes reservas.");
        return;
      }
      showMessage(historyFeedback, `${reservations.length} reserva(s).`);
      reservations.forEach((reservation) => reservationList.append(renderReservation(reservation)));
    } catch (error) {
      showMessage(historyFeedback, error.message, "error");
    }
  }

  function renderReservation(reservation) {
    const item = makeElement("article", "reservation-item");
    const details = makeElement("div", "reservation-details");
    details.append(makeElement("h3", "", titleCase(reservation.spaceName)));
    details.append(makeElement("p", "", `${formatDate(reservation.startAt)} – ${formatDate(reservation.endAt)}`));
    details.append(makeElement("p", "", `${reservation.purpose} · ${reservation.attendees} asistentes`));
    const isPast = new Date(reservation.endAt) <= new Date();
    const displayStatus = statusLabel(reservation.status);
    details.append(makeElement("span", `reservation-status status-${reservation.status.toLowerCase()}`, displayStatus));
    item.append(details);
    if (reservation.status === "ACTIVE" && !isPast) {
      const cancelButton = makeElement("button", "button button-outline", "Cancelar");
      cancelButton.type = "button";
      cancelButton.addEventListener("click", async () => {
        const reason = window.prompt("Indica el motivo de la cancelación:");
        if (!reason?.trim()) return;
        cancelButton.disabled = true;
        try {
          await apiRequest(`/api/v1/reservations/${reservation.id}/cancel`, {
            method: "PATCH",
            authenticated: true,
            body: { reason: reason.trim() },
          });
          showMessage(formFeedback, "La reserva fue cancelada.", "success");
          await loadMyReservations();
        } catch (error) {
          showMessage(historyFeedback, error.message, "error");
          cancelButton.disabled = false;
        }
      });
      item.append(cancelButton);
    }
    return item;
  }
}

function localDateString(date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return localDate.toISOString().slice(0, 10);
}

function localDayBounds(dateValue) {
  const start = new Date(`${dateValue}T00:00:00`);
  const end = new Date(start);
  end.setDate(end.getDate() + 1);
  return { startAt: start.toISOString(), endAt: end.toISOString() };
}

function statusLabel(status) {
  return ({ ACTIVE: "Activa", CANCELLED: "Cancelada", FINISHED: "Finalizada", MAINTENANCE: "Mantenimiento",
    INACTIVE: "Inactivo", RESERVATION: "Reserva", BLOCK: "Bloqueo" })[status] || status;
}

function fillSpaceOptions(select, spaces, placeholder) {
  if (!select) return;
  const previousValue = select.value;
  select.replaceChildren(new Option(placeholder, ""));
  spaces.forEach((space) => select.add(new Option(`${titleCase(space.name)} · ${space.location}`, space.id)));
  if (spaces.some((space) => String(space.id) === previousValue)) select.value = previousValue;
}

function setSpaceScheduleForm(form, schedules = null) {
  const defaults = {
    MONDAY: ["07:00", "22:00"], TUESDAY: ["07:00", "22:00"], WEDNESDAY: ["07:00", "22:00"],
    THURSDAY: ["07:00", "22:00"], FRIDAY: ["07:00", "22:00"], SATURDAY: ["08:00", "18:00"],
    SUNDAY: ["08:00", "18:00"],
  };
  const byDay = new Map((schedules || []).map((schedule) => [schedule.dayOfWeek, schedule]));
  form.querySelectorAll("[data-schedule-row]").forEach((row) => {
    const checkbox = row.querySelector("[data-schedule-day]");
    const opens = row.querySelector("[data-schedule-open]");
    const closes = row.querySelector("[data-schedule-close]");
    if (!opens.options.length) {
      for (let minutes = 0; minutes <= 23 * 60 + 30; minutes += 30) {
        const hour = String(Math.floor(minutes / 60)).padStart(2, "0");
        const minute = String(minutes % 60).padStart(2, "0");
        const value = `${hour}:${minute}`;
        opens.add(new Option(value, value));
        closes.add(new Option(value, value));
      }
      closes.add(new Option("23:59", "23:59"));
    }
    const schedule = byDay.get(checkbox.value);
    const [defaultOpens, defaultCloses] = defaults[checkbox.value];
    checkbox.checked = schedules === null
      ? ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"].includes(checkbox.value)
      : Boolean(schedule);
    opens.value = String(schedule?.opensAt || defaultOpens).slice(0, 5);
    closes.value = String(schedule?.closesAt || defaultCloses).slice(0, 5);
    opens.disabled = !checkbox.checked;
    closes.disabled = !checkbox.checked;
    checkbox.onchange = () => {
      opens.disabled = !checkbox.checked;
      closes.disabled = !checkbox.checked;
    };
  });
}

function getSpaceScheduleFormValues(form) {
  return [...form.querySelectorAll("[data-schedule-row]")]
    .filter((row) => row.querySelector("[data-schedule-day]").checked)
    .map((row) => ({
      dayOfWeek: row.querySelector("[data-schedule-day]").value,
      opensAt: row.querySelector("[data-schedule-open]").value,
      closesAt: row.querySelector("[data-schedule-close]").value,
    }));
}

function renderAdminSpaces(spaces = adminSpaces) {
  const container = document.querySelector("[data-admin-spaces]");
  if (!container) return;
  container.replaceChildren();
  if (!spaces.length) {
    container.append(makeElement("p", "feedback", "No hay espacios que coincidan con estos filtros."));
    return;
  }
  spaces.forEach((space) => {
    const item = makeElement("article", "admin-list-item");
    const details = makeElement("div", "admin-list-details");
    details.append(makeElement("h3", "", titleCase(space.name)));
    details.append(makeElement("p", "", `${titleCase(space.type?.name || "Sin tipo")} · ${space.location} · ${space.capacity} personas`));
    details.append(makeElement("p", "", `Horario: ${spaceScheduleSummary(space.schedules)}`));
    details.append(makeElement("span", "admin-status", statusLabel(space.status)));
    const edit = makeElement("button", "button button-outline", "Editar");
    edit.type = "button";
    edit.addEventListener("click", () => {
      const form = document.querySelector("[data-admin-space-form]");
      form.elements.spaceId.value = space.id;
      form.elements.name.value = titleCase(space.name);
      form.elements.typeId.value = space.type.id;
      form.elements.location.value = space.location;
      form.elements.capacity.value = space.capacity;
      form.elements.description.value = space.description || "";
      form.elements.status.value = space.status;
      setSpaceScheduleForm(form, space.schedules || []);
      form.querySelector("[data-space-submit]").textContent = "Guardar cambios";
      form.querySelector("[data-space-reset]").hidden = false;
      form.scrollIntoView({ behavior: "smooth", block: "start" });
    });
    item.append(details, edit);
    container.append(item);
  });
}

function renderManagerSpaces() {
  const container = document.querySelector("[data-manager-spaces]");
  if (!container) return;
  container.replaceChildren();
  if (!adminSpaces.length) {
    container.append(makeElement("p", "feedback", "No tienes espacios asignados."));
    return;
  }
  adminSpaces.forEach((space) => {
    const item = makeElement("article", "admin-list-item manager-space-item");
    const details = makeElement("div", "admin-list-details");
    details.append(makeElement("h3", "", titleCase(space.name)));
    details.append(makeElement("p", "", `${titleCase(space.type?.name || "Sin tipo")} · ${space.location} · ${space.capacity} personas`));
    details.append(makeElement("span", "admin-status", statusLabel(space.status)));
    const form = makeElement("form", "manager-space-status-form");
    const field = makeElement("div", "field");
    const id = `manager-space-status-${space.id}`;
    const label = makeElement("label", "", "Estado del espacio");
    label.htmlFor = id;
    const select = document.createElement("select");
    select.id = id;
    select.name = "status";
    [["ACTIVE", "Activo"], ["MAINTENANCE", "Mantenimiento"], ["INACTIVE", "Inactivo"]]
      .forEach(([value, text]) => select.add(new Option(text, value)));
    select.value = space.status;
    field.append(label, select);
    const feedback = makeElement("p", "feedback");
    feedback.setAttribute("role", "status");
    feedback.setAttribute("aria-live", "polite");
    feedback.hidden = true;
    const save = makeElement("button", "button button-outline", "Guardar estado");
    save.type = "submit";
    form.append(field, feedback, save);
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      save.disabled = true;
      try {
        await apiRequest(`/api/v1/spaces/${space.id}/status`, {
          method: "PATCH", authenticated: true, body: { status: select.value },
        });
        showMessage(feedback, "Estado del espacio actualizado.", "success");
        await loadAdminSpacesAndTypes(false);
        await loadAdminBlocks();
      } catch (error) {
        showMessage(feedback, error.message, "error");
        save.disabled = false;
      }
    });
    item.append(details, form);
    container.append(item);
  });
}

async function loadAdminSpacesAndTypes(isAdministrator) {
  const typeSelect = document.querySelector("[data-admin-space-form] [name=typeId]");
  const typeList = document.querySelector("[data-admin-types]");
  const filterTypeSelect = document.querySelector("[data-admin-space-filter] [name=type]");
  const spaces = await apiRequest("/api/v1/spaces/manage", { authenticated: true });
  adminSpaces = spaces;
  if (isAdministrator) {
    const types = await apiRequest("/api/v1/space-types");
    typeSelect.replaceChildren(new Option("Selecciona un tipo", ""));
    types.forEach((type) => typeSelect.add(new Option(titleCase(type.name), type.id)));
    filterTypeSelect.replaceChildren(new Option("Todos los tipos", ""));
    types.forEach((type) => filterTypeSelect.add(new Option(titleCase(type.name), type.name)));
    typeList.replaceChildren();
    types.forEach((type) => {
      const item = makeElement("article", "admin-list-item");
      const details = makeElement("div", "admin-list-details");
      details.append(makeElement("h3", "", titleCase(type.name)));
      if (type.description) details.append(makeElement("p", "", type.description));
      const edit = makeElement("button", "button button-outline", "Editar tipo");
      edit.type = "button";
      edit.addEventListener("click", () => {
        const form = document.querySelector("[data-admin-type-form]");
        form.elements.typeId.value = type.id;
        form.elements.name.value = titleCase(type.name);
        form.elements.description.value = type.description || "";
        form.querySelector("[data-type-submit]").textContent = "Guardar tipo";
        form.querySelector("[data-type-reset]").hidden = false;
        form.scrollIntoView({ behavior: "smooth", block: "center" });
      });
      item.append(details, edit);
      typeList.append(item);
    });
  }
  renderAdminSpaces();
  if (!isAdministrator) renderManagerSpaces();
  fillSpaceOptions(document.querySelector("[data-admin-block-form] [name=spaceId]"),
    adminSpaces.filter((space) => space.status === "ACTIVE"), "Selecciona un espacio activo");
  fillSpaceOptions(document.querySelector("[data-admin-agenda-form] [name=spaceId]"), adminSpaces,
    "Todos los espacios");
}

async function loadAdminUsers() {
  const container = document.querySelector("[data-admin-users]");
  const feedback = document.querySelector("[data-users-feedback]");
  const [users, currentProfile] = await Promise.all([
    apiRequest("/api/v1/users", { authenticated: true }), getCurrentProfile(),
  ]);
  container.replaceChildren();
  if (!users.length) {
    showMessage(feedback, "No hay usuarios registrados.");
    return;
  }
  showMessage(feedback, `${users.length} usuario(s).`);
  const roleOptions = [
    ["ADMINISTRATOR", "Administrador"], ["MANAGER", "Responsable"], ["USER", "Usuario"], ["VISITOR", "Visitante"],
  ];
  users.forEach((user) => {
    const form = makeElement("form", "admin-list-item admin-user-form");
    const details = makeElement("div", "admin-list-details");
    details.append(makeElement("h3", "", titleCase(user.name)));
    details.append(makeElement("p", "", `${user.email} · ${statusLabel(user.status)}`));
    const roles = makeElement("fieldset", "admin-role-options");
    roles.append(makeElement("legend", "", "Roles (puedes seleccionar varios)"));
    roleOptions.forEach(([value, label]) => {
      const wrapper = makeElement("label", "admin-role-option");
      const checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.name = "roles";
      checkbox.value = value;
      checkbox.checked = user.roles.includes(value);
      wrapper.append(checkbox, document.createTextNode(label));
      roles.append(wrapper);
    });
    details.append(roles);
    const spaceField = makeElement("div", "field admin-managed-spaces");
    const label = makeElement("label", "", "Espacios asignados a responsable");
    const select = document.createElement("select");
    select.id = `user-managed-spaces-${user.id}`;
    select.name = "spaceIds";
    select.multiple = true;
    select.size = Math.min(Math.max(adminSpaces.length, 3), 6);
    adminSpaces.forEach((space) => {
      const option = new Option(`${titleCase(space.name)} · ${space.location}`, space.id);
      option.selected = user.managedSpaceIds.includes(space.id);
      select.add(option);
    });
    label.htmlFor = select.id;
    spaceField.append(label, select);
    details.append(spaceField);
    const statusField = makeElement("div", "field admin-user-status");
    const statusLabelElement = makeElement("label", "", "Estado de la cuenta");
    const statusSelect = document.createElement("select");
    statusSelect.id = `user-status-${user.id}`;
    statusSelect.name = "status";
    statusLabelElement.htmlFor = statusSelect.id;
    [["ACTIVE", "Activa"], ["INACTIVE", "Inactiva"]]
      .forEach(([value, labelText]) => statusSelect.add(new Option(labelText, value)));
    statusSelect.value = user.status;
    const isCurrentUser = currentProfile?.id === user.id;
    statusSelect.disabled = isCurrentUser;
    statusField.append(statusLabelElement, statusSelect);
    if (isCurrentUser) {
      statusField.append(makeElement("small", "field-hint", "No puedes cambiar el estado de tu propia cuenta."));
    }
    details.append(statusField);
    const feedbackItem = makeElement("p", "feedback");
    feedbackItem.hidden = true;
    const save = makeElement("button", "button button-primary", "Guardar cambios de usuario");
    save.type = "submit";
    form.append(details, feedbackItem, save);
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      save.disabled = true;
      const selectedRoles = Array.from(form.querySelectorAll("input[name=roles]:checked"), (input) => input.value);
      const selectedSpaceIds = Array.from(select.selectedOptions, (option) => option.value);
      if (!selectedRoles.length) {
        showMessage(feedbackItem, "Cada usuario debe conservar al menos un rol.", "error");
        save.disabled = false;
        return;
      }
      try {
        await apiRequest(`/api/v1/users/${user.id}/roles`, {
          method: "PATCH", authenticated: true, body: { roles: selectedRoles },
        });
        if (selectedRoles.includes("MANAGER")) {
          await apiRequest(`/api/v1/users/${user.id}/managed-spaces`, {
            method: "PATCH", authenticated: true, body: { spaceIds: selectedSpaceIds },
          });
        }
        await apiRequest(`/api/v1/users/${user.id}/status`, {
          method: "PATCH", authenticated: true, body: { status: statusSelect.value },
        });
        showMessage(feedbackItem, "Permisos actualizados.", "success");
        await loadAdminUsers();
      } catch (error) {
        showMessage(feedbackItem, error.message, "error");
        save.disabled = false;
      }
    });
    container.append(form);
  });
}

async function loadAdminBlocks() {
  const container = document.querySelector("[data-admin-blocks]");
  const blocks = await apiRequest("/api/v1/space-blocks", { authenticated: true });
  container.replaceChildren();
  if (!blocks.length) {
    container.append(makeElement("p", "feedback", "No hay bloqueos para los espacios disponibles."));
    return;
  }
  blocks.forEach((block) => {
    const item = makeElement("article", "admin-list-item");
    const details = makeElement("div", "admin-list-details");
    details.append(makeElement("h3", "", titleCase(block.spaceName)));
    details.append(makeElement("p", "", `${formatDate(block.startAt)} – ${formatDate(block.endAt)}`));
    details.append(makeElement("p", "", block.reason));
    details.append(makeElement("span", "admin-status", statusLabel(block.status)));
    item.append(details);
    if (block.status === "ACTIVE") {
      const cancel = makeElement("button", "button button-outline", "Cancelar bloqueo");
      cancel.type = "button";
      cancel.addEventListener("click", async () => {
        cancel.disabled = true;
        try {
          await apiRequest(`/api/v1/space-blocks/${block.id}/cancel`, { method: "PATCH", authenticated: true });
          await loadAdminBlocks();
        } catch (error) {
          showMessage(document.querySelector("[data-block-feedback]"), error.message, "error");
          cancel.disabled = false;
        }
      });
      item.append(cancel);
    }
    container.append(item);
  });
}

async function loadAdminAgenda() {
  const form = document.querySelector("[data-admin-agenda-form]");
  const feedback = document.querySelector("[data-agenda-feedback]");
  const container = document.querySelector("[data-admin-agenda]");
  const date = form.elements.date.value;
  if (!date) return;
  showMessage(feedback, "Cargando agenda…");
  const { startAt, endAt } = localDayBounds(date);
  const spaceId = form.elements.spaceId.value;
  const status = form.elements.status.value;
  const reservationParams = new URLSearchParams({ startAt, endAt });
  if (spaceId) reservationParams.set("spaceId", spaceId);
  if (status) reservationParams.set("status", status);
  const blockParams = new URLSearchParams({ startAt, endAt });
  if (spaceId) blockParams.set("spaceId", spaceId);
  if (status === "ACTIVE" || status === "CANCELLED") blockParams.set("status", status);
  try {
    const [reservations, blocks] = await Promise.all([
      apiRequest(`/api/v1/reservations?${reservationParams}`, { authenticated: true }),
      status === "FINISHED" ? Promise.resolve([]) : apiRequest(`/api/v1/space-blocks?${blockParams}`, { authenticated: true }),
    ]);
    const entries = [
      ...reservations.map((entry) => ({ ...entry, kind: "Reserva", title: titleCase(`${entry.spaceName} · ${entry.requesterName}`) })),
      ...blocks.map((entry) => ({ ...entry, kind: "Bloqueo", title: titleCase(`${entry.spaceName} · ${entry.reason}`) })),
    ].sort((left, right) => new Date(left.startAt) - new Date(right.startAt));
    container.replaceChildren();
    if (!entries.length) {
      showMessage(feedback, "No hay reservas ni bloqueos para esa fecha.");
      return;
    }
    showMessage(feedback, `${entries.length} elemento(s) en la agenda.`);
    entries.forEach((entry) => {
      const item = makeElement("article", "admin-list-item");
      const details = makeElement("div", "admin-list-details");
      details.append(makeElement("h3", "", entry.title));
      details.append(makeElement("p", "", `${entry.kind} · ${formatDate(entry.startAt)} – ${formatDate(entry.endAt)}`));
      details.append(makeElement("span", "admin-status", statusLabel(entry.status)));
      item.append(details);
      container.append(item);
    });
  } catch (error) {
    container.replaceChildren();
    showMessage(feedback, error.message, "error");
  }
}

async function loadAdminReport() {
  const form = document.querySelector("[data-admin-report-form]");
  const feedback = document.querySelector("[data-report-feedback]");
  const container = document.querySelector("[data-admin-report]");
  const fromDate = form.elements.from.value;
  const toDate = form.elements.to.value;
  if (!fromDate || !toDate) return;
  if (fromDate > toDate) {
    showMessage(feedback, "La fecha inicial debe ser anterior o igual a la final.", "error");
    return;
  }
  showMessage(feedback, "Generando reporte…");
  const { startAt } = localDayBounds(fromDate);
  const { endAt } = localDayBounds(toDate);
  const params = new URLSearchParams({ startAt, endAt });
  try {
    const rows = await apiRequest(`/api/v1/reports/usage?${params}`, { authenticated: true });
    container.replaceChildren();
    if (!rows.length) {
      showMessage(feedback, "No hay espacios registrados.");
      return;
    }
    showMessage(feedback, `Reporte para ${fromDate} a ${toDate}.`);
    rows.forEach((row) => {
      const item = makeElement("article", "admin-list-item");
      const details = makeElement("div", "admin-list-details");
      details.append(makeElement("h3", "", titleCase(row.spaceName)));
      details.append(makeElement("p", "", `${row.reservationCount} reservas · ${row.occupiedHours} horas ocupadas`));
      item.append(details);
      container.append(item);
    });
  } catch (error) {
    container.replaceChildren();
    showMessage(feedback, error.message, "error");
  }
}

async function loadAdminAudit() {
  const feedback = document.querySelector("[data-audit-feedback]");
  const container = document.querySelector("[data-admin-audit]");
  try {
    const entries = await apiRequest("/api/v1/audit", { authenticated: true });
    container.replaceChildren();
    if (!entries.length) {
      showMessage(feedback, "Todavía no hay operaciones auditadas.");
      return;
    }
    showMessage(feedback, `${entries.length} operación(es) recientes.`);
    entries.forEach((entry) => {
      const item = makeElement("article", "admin-list-item");
      const details = makeElement("div", "admin-list-details");
      details.append(makeElement("h3", "", titleCase(`${entry.action.replaceAll("_", " ")} · ${entry.entity.replaceAll("_", " ")}`)));
      details.append(makeElement("p", "", `${entry.actorName} · ${formatDate(entry.createdAt)}`));
      if (entry.detail) details.append(makeElement("p", "", entry.detail));
      item.append(details);
      container.append(item);
    });
  } catch (error) {
    showMessage(feedback, error.message, "error");
  }
}

async function loadAdminPage() {
  const message = document.querySelector("[data-admin-access-message]");
  const content = document.querySelector("[data-admin-content]");
  const profile = await getCurrentProfile();
  const roles = profile?.roles || [];
  const isAdministrator = roles.includes("ADMINISTRATOR");
  const isManager = roles.includes("MANAGER");
  if (!isAdministrator && !isManager) {
    showMessage(message, "Inicia sesión con un rol de administrador o responsable para continuar.", "error");
    return;
  }
  content.hidden = false;
  if (!isAdministrator) {
    document.querySelectorAll("[data-admin-only]").forEach((section) => { section.hidden = true; });
    document.querySelector("#blocks-admin-title").textContent = titleCase("Bloqueos de mis espacios");
    document.querySelector("#blocks-admin-title").nextElementSibling.textContent =
      "Crea o cancela bloqueos únicamente en los espacios asignados a tu usuario.";
  }
  document.querySelectorAll("[data-manager-only]").forEach((section) => { section.hidden = isAdministrator; });
  showMessage(message, isAdministrator ? "Sesión de administrador." : "Sesión de responsable.", "success");
  try {
    const spaceEditor = document.querySelector("[data-admin-space-form]");
    if (spaceEditor) setSpaceScheduleForm(spaceEditor);
    await loadAdminSpacesAndTypes(isAdministrator);
    if (isAdministrator) await loadAdminUsers();

    document.querySelector("[data-admin-space-filter]")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      const form = event.currentTarget;
      const params = new URLSearchParams();
      ["query", "type", "minimumCapacity", "status"].forEach((key) => {
        const value = form.elements[key].value.trim();
        if (value) params.set(key, value);
      });
      try {
        const suffix = params.size ? `?${params}` : "";
        const filteredSpaces = await apiRequest(`/api/v1/spaces/manage${suffix}`, { authenticated: true });
        renderAdminSpaces(filteredSpaces);
        showMessage(document.querySelector("[data-admin-space-filter-feedback]"),
          `${filteredSpaces.length} espacio(s) encontrados.`, "success");
      } catch (error) {
        showMessage(document.querySelector("[data-admin-space-filter-feedback]"), error.message, "error");
      }
    });

    const today = localDateString(new Date());
    const agendaForm = document.querySelector("[data-admin-agenda-form]");
    if (agendaForm) agendaForm.elements.date.value = today;
    const reportForm = document.querySelector("[data-admin-report-form]");
    if (reportForm) {
      const monthAgo = new Date();
      monthAgo.setDate(monthAgo.getDate() - 30);
      reportForm.elements.from.value = localDateString(monthAgo);
      reportForm.elements.to.value = today;
    }

    document.querySelector("[data-admin-space-form]")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      const form = event.currentTarget;
      const feedback = document.querySelector("[data-space-feedback]");
      const submit = form.querySelector("[data-space-submit]");
      const schedules = getSpaceScheduleFormValues(form);
      if (!schedules.length) {
        showMessage(feedback, "Selecciona al menos un día y horario disponible.", "error");
        return;
      }
      if (schedules.some((schedule) => schedule.opensAt >= schedule.closesAt)) {
        showMessage(feedback, "En cada día, la hora de cierre debe ser posterior a la hora de apertura.", "error");
        return;
      }
      submit.disabled = true;
      const body = {
        name: form.elements.name.value.trim(), typeId: form.elements.typeId.value,
        location: form.elements.location.value.trim(), capacity: Number(form.elements.capacity.value),
        description: form.elements.description.value.trim(), status: form.elements.status.value, schedules,
      };
      try {
        const spaceId = form.elements.spaceId.value;
        await apiRequest(spaceId ? `/api/v1/spaces/${spaceId}` : "/api/v1/spaces", {
          method: spaceId ? "PATCH" : "POST", authenticated: true, body,
        });
        showMessage(feedback, spaceId ? "Espacio actualizado." : "Espacio creado.", "success");
        form.reset();
        form.elements.spaceId.value = "";
        form.elements.status.value = "ACTIVE";
        setSpaceScheduleForm(form);
        submit.textContent = "Crear espacio";
        form.querySelector("[data-space-reset]").hidden = true;
        await loadAdminSpacesAndTypes(true);
        await loadAdminUsers();
      } catch (error) {
        showMessage(feedback, error.message, "error");
      } finally {
        submit.disabled = false;
      }
    });
    document.querySelector("[data-space-reset]")?.addEventListener("click", () => {
      const form = document.querySelector("[data-admin-space-form]");
      form.reset();
      form.elements.spaceId.value = "";
      form.elements.status.value = "ACTIVE";
      setSpaceScheduleForm(form);
      form.querySelector("[data-space-submit]").textContent = "Crear espacio";
      form.querySelector("[data-space-reset]").hidden = true;
      showMessage(document.querySelector("[data-space-feedback]"), "");
    });
    document.querySelector("[data-admin-type-form]")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      const form = event.currentTarget;
      const typeId = form.elements.typeId.value;
      try {
        await apiRequest(typeId ? `/api/v1/space-types/${typeId}` : "/api/v1/space-types", {
          method: typeId ? "PATCH" : "POST", authenticated: true,
          body: { name: form.elements.name.value.trim(), description: form.elements.description.value.trim() },
        });
        form.reset();
        form.elements.typeId.value = "";
        form.querySelector("[data-type-submit]").textContent = "Agregar tipo";
        form.querySelector("[data-type-reset]").hidden = true;
        showMessage(document.querySelector("[data-type-feedback]"), typeId ? "Tipo actualizado." : "Tipo agregado.", "success");
        await loadAdminSpacesAndTypes(true);
      } catch (error) {
        showMessage(document.querySelector("[data-type-feedback]"), error.message, "error");
      }
    });
    document.querySelector("[data-type-reset]")?.addEventListener("click", () => {
      const form = document.querySelector("[data-admin-type-form]");
      form.reset();
      form.elements.typeId.value = "";
      form.querySelector("[data-type-submit]").textContent = "Agregar tipo";
      form.querySelector("[data-type-reset]").hidden = true;
      showMessage(document.querySelector("[data-type-feedback]"), "");
    });
    document.querySelector("[data-admin-block-form]")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      const form = event.currentTarget;
      const feedback = document.querySelector("[data-block-feedback]");
      const startAt = new Date(form.elements.startAt.value);
      const endAt = new Date(form.elements.endAt.value);
      if (!Number.isFinite(startAt.getTime()) || !Number.isFinite(endAt.getTime()) || startAt >= endAt) {
        showMessage(feedback, "La hora de inicio debe ser anterior a la hora de finalización.", "error");
        return;
      }
      const submit = form.querySelector("button[type=submit]");
      submit.disabled = true;
      try {
        await apiRequest("/api/v1/space-blocks", {
          method: "POST", authenticated: true,
          body: { spaceId: form.elements.spaceId.value, startAt: startAt.toISOString(), endAt: endAt.toISOString(),
            reason: form.elements.reason.value.trim() },
        });
        form.reset();
        showMessage(feedback, "Bloqueo creado.", "success");
        await loadAdminBlocks();
      } catch (error) {
        showMessage(feedback, error.message, "error");
      } finally {
        submit.disabled = false;
      }
    });
    document.querySelector("[data-admin-agenda-form]")?.addEventListener("submit", (event) => {
      event.preventDefault();
      loadAdminAgenda();
    });
    document.querySelector("[data-admin-report-form]")?.addEventListener("submit", (event) => {
      event.preventDefault();
      loadAdminReport();
    });

    await loadAdminBlocks();
    if (isAdministrator) {
      await Promise.all([loadAdminAgenda(), loadAdminReport(), loadAdminAudit()]);
    }
  } catch (error) {
    showMessage(message, error.message, "error");
  }
}

updateNavigation();
if (page === "home") loadHome();
if (page === "spaces") loadSpaceCatalog();
if (page === "login") configureAuthForm();
if (page === "reservations") loadReservationPage();
if (page === "admin") loadAdminPage();
if (page === "profile") loadProfilePage();
