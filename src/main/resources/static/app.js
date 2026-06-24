const CONTEXT_PATH = document.querySelector('meta[name="context-path"]')?.content || "";

const API = {
	departments: `${CONTEXT_PATH}/api/departments`,
	currentQueues: `${CONTEXT_PATH}/api/queues/current`,
	queues: `${CONTEXT_PATH}/api/queueTickets`,
	phoneCodes: `${CONTEXT_PATH}/api/phoneCodes`,
	icStates: `${CONTEXT_PATH}/api/icStates`,
	cancelQueue: (queueNumber) => `${CONTEXT_PATH}/api/queueTickets/${encodeURIComponent(queueNumber)}/status`
};

const CURRENT_QUEUE_REFRESH_INTERVAL_MS = 30000;

const VALIDATION_MESSAGES = {
	requiredIdentityType: "Identity type is required",
	invalidMyKad: "Enter a valid 12-digit MyKad number",
	invalidIdentityNumber: "Invalid identity number",
	invalidForeignIdentity: "Enter 5–20 passport or identity letters and numbers",
	requiredCountryCode: "Country code is required",
	invalidPhoneNumber: "Enter a valid phone number",
	invalidMalaysianPhone: "Enter a valid Malaysian mobile number",
	requiredDepartment: "Please select a department"
};

const DEPARTMENT_LOGOS = {
	GEN: `${CONTEXT_PATH}/assets/departments/general-consult-logo.png?v=20260530-3`,
	PHA: `${CONTEXT_PATH}/assets/departments/pharmacy-logo.png?v=20260530-3`,
	DEN: `${CONTEXT_PATH}/assets/departments/dental-logo.png?v=20260530-3`,
	LAB: `${CONTEXT_PATH}/assets/departments/blood-test-logo.png?v=20260530-3`,
	SPC: `${CONTEXT_PATH}/assets/departments/specialist-logo.png?v=20260530-3`
};

const elements = {
	clock: document.querySelector("#clock"),
	departmentSelect: document.querySelector("#department-select"),
	departmentPreview: document.querySelector("#department-preview"),
	quotaSummary: document.querySelector("#quota-summary"),
	queueForm: document.querySelector("#queue-form"),
	takePanel: document.querySelector("#take-panel"),
	identityType: document.querySelector("#identity-type"),
	identityLabel: document.querySelector("#identity-number-label"),
	identityNumber: document.querySelector("#identity-number"),
	identityHint: document.querySelector("#identity-number-hint"),
	identityMask: document.querySelector("#identity-mask"),
	phoneCountryCode: document.querySelector("#phone-country-code"),
	phoneCountryFixed: document.querySelector("#phone-country-fixed"),
	malaysiaPrefix: document.querySelector("#malaysia-prefix"),
	foreignPrefix: document.querySelector("#foreign-prefix"),
	phoneNumber: document.querySelector("#phone-number"),
	phoneFlag: document.querySelector("#phone-flag"),
	phoneHint: document.querySelector("#phone-number-hint"),
	formError: document.querySelector("#form-error"),
	checkForm: document.querySelector("#check-form"),
	ticketResult: document.querySelector("#ticket-result"),
	checkResult: document.querySelector("#check-result"),
	currentQueueList: document.querySelector("#current-queue-list"),
	toast: document.querySelector("#toast")
};

let phoneCodeData = [];
let icStateCodes = new Set();

document.addEventListener("DOMContentLoaded", () => {
	document.body.classList.add("is-ready");
	bindTabs();
	bindSmartInputs();
	bindForms();
	bindTicketActions();
	startClock();
	startCurrentQueueAutoRefresh();
	loadDepartments();
	loadPhoneCodes();
	loadIcStates();
	loadCurrentQueues();
});

function bindTabs() {
	document.querySelectorAll(".tab-button").forEach((button) => {
		button.addEventListener("click", () => {
			const targetId = button.dataset.tabTarget;
			document.querySelectorAll(".tab-button").forEach((tab) => {
				tab.classList.toggle("active", tab === button);
				tab.setAttribute("aria-selected", String(tab === button));
			});
			document.querySelectorAll(".tab-panel").forEach((panel) => {
				panel.classList.toggle("active", panel.id === targetId);
			});
			if (targetId === "current-panel") {
				loadCurrentQueues();
			}
		});
	});
}

function bindForms() {
	elements.queueForm.addEventListener("submit", async (event) => {
		event.preventDefault();
		clearFormErrors();
		const payload = queueFormPayload();
		if (!validateQueueForm(payload)) {
			return;
		}
		const submitButton = elements.queueForm.querySelector('[type="submit"]');
		submitButton.disabled = true;

		try {
			const ticket = await requestJson(API.queues, {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(payload)
			});
			renderTicketResult(ticket);
			elements.queueForm.reset();
			configureIdentityInput();
			configurePhoneCountry();
			renderDepartmentPreview(null);
			showToast(`Queue number ${ticket.queueNumber} created.`, "success");
			loadCurrentQueues();
		}
		catch (error) {
			displayApiError(error);
			showToast(error.message, "error");
		}
		finally {
			submitButton.disabled = false;
		}
	});

	elements.checkForm.addEventListener("submit", async (event) => {
		event.preventDefault();
		const formData = new FormData(elements.checkForm);
		const queueNumber = String(formData.get("queueNumber")).trim().toUpperCase();
		if (!queueNumber) {
			showToast("Please enter a queue number.", "error");
			return;
		}

		try {
			const ticket = await requestJson(`${API.queues}/${encodeURIComponent(queueNumber)}`);
			renderTicketDetail(ticket);
		}
		catch (error) {
			elements.checkResult.hidden = false;
			elements.checkResult.innerHTML = `<p>${escapeHtml(error.message)}</p>`;
			showToast(error.message, "error");
		}
	});

}

function bindTicketActions() {
	document.addEventListener("click", async (event) => {
		const backButton = event.target.closest("[data-back-to-form]");
		if (backButton) {
			showQueueForm();
			return;
		}
		const button = event.target.closest("[data-cancel-queue]");
		if (!button) {
			return;
		}

		try {
			const ticket = await requestJson(API.cancelQueue(button.dataset.cancelQueue), {
				method: "PATCH",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ status: "CANCELLED" })
			});
			if (button.closest("#ticket-result")) {
				renderTicketResult(ticket);
			}
			else {
				renderTicketDetail(ticket);
			}
			showToast(`${ticket.queueNumber} cancelled.`, "success");
			loadCurrentQueues();
		}
		catch (error) {
			showToast(error.message, "error");
		}
	});
}

function startClock() {
	updateClock();
	setInterval(updateClock, 1000);
}

function startCurrentQueueAutoRefresh() {
	setInterval(() => {
		if (document.querySelector("#current-panel")?.classList.contains("active")) {
			loadCurrentQueues();
		}
	}, CURRENT_QUEUE_REFRESH_INTERVAL_MS);
}

function updateClock() {
	elements.clock.textContent = new Intl.DateTimeFormat("en-MY", {
		hour: "2-digit",
		minute: "2-digit",
		second: "2-digit",
		hour12: false,
		timeZone: "Asia/Kuala_Lumpur"
	}).format(new Date());
}

async function loadDepartments() {
	try {
		const departments = await requestJson(API.departments);
		const departmentOptions = [
			`<option value="">Select department</option>`,
			...departments.map((department) =>
				`<option value="${department.code}">${escapeHtml(department.name)} (${department.code})</option>`
			)
		].join("");
		elements.departmentSelect.innerHTML = departmentOptions;

		elements.departmentSelect.addEventListener("change", () => {
			const selected = departments.find((department) => department.code === elements.departmentSelect.value);
			renderDepartmentPreview(selected);
		});

		elements.quotaSummary.innerHTML = departments.map((department, index) =>
			`<li class="quota-item" style="--row-index: ${index}">
				<img src="${departmentLogo(department.code)}" alt="" loading="lazy">
				<span>${escapeHtml(department.name)}: ${department.dailyQuota} slots daily</span>
			</li>`
		).join("");
	}
	catch (error) {
		showToast(error.message, "error");
	}
}

function bindSmartInputs() {
	elements.identityType.addEventListener("change", () => {
		elements.identityNumber.value = "";
		elements.phoneNumber.value = "";
		configureIdentityInput();
		configurePhoneCountry();
		validateField("identityType");
		clearFieldError("identityNumber");
		clearFieldError("phoneCountryCode");
		clearFieldError("phoneNumber");
	});

	elements.identityNumber.addEventListener("input", () => {
		if (elements.identityType.value === "MALAYSIAN") {
			const digits = elements.identityNumber.value.replace(/\D/g, "").slice(0, 12);
			elements.identityNumber.value = formatMalaysianIdentity(digits);
		}
		else {
			elements.identityNumber.value = elements.identityNumber.value
				.replace(/[^a-z0-9]/gi, "").toUpperCase().slice(0, 20);
		}
		updateIdentityMask();
		validateField("identityNumber");
	});
	elements.identityNumber.addEventListener("focus", updateIdentityMask);
	elements.identityNumber.addEventListener("blur", updateIdentityMask);

	elements.phoneNumber.addEventListener("input", () => {
		let number = elements.phoneNumber.value.replace(/\D/g, "");
		if (elements.identityType.value === "MALAYSIAN") {
			number = number.replace(/^0+/, "");
		}
		elements.phoneNumber.value = number.slice(0, 15);
		validateField("phoneNumber");
	});
	elements.phoneNumber.addEventListener("keydown", (event) => {
		if (elements.identityType.value === "MALAYSIAN" && event.key === "0" && elements.phoneNumber.selectionStart === 0) {
			event.preventDefault();
		}
	});
	elements.phoneCountryCode.addEventListener("change", () => {
		updatePhoneCountry();
		validateFields("phoneCountryCode", "phoneNumber");
	});
	elements.departmentSelect.addEventListener("change", () => validateField("departmentCode"));
	configureIdentityInput();
	configurePhoneCountry();
}

function configureIdentityInput() {
	const isMalaysian = elements.identityType.value === "MALAYSIAN";
	elements.identityLabel.textContent = isMalaysian ? "Identity Number" : "Passport Number";
	elements.identityNumber.inputMode = isMalaysian ? "numeric" : "text";
	elements.identityNumber.placeholder = isMalaysian ? "e.g. IC No." : "Enter passport number";
	elements.identityNumber.setAttribute("aria-label", isMalaysian ? "MyKad number, 12 digits" : "Passport or foreign identity number, 5 to 20 letters or numbers");
	elements.identityNumber.maxLength = isMalaysian ? 14 : 20;
	elements.identityHint.textContent = isMalaysian
		? "12-digit MyKad number"
		: "5–20 letters or numbers, for example E3905107K";
	updateIdentityMask();
}

function updateIdentityMask() {
	const active = document.activeElement === elements.identityNumber || elements.identityNumber.value.length > 0;
	elements.identityNumber.placeholder = active
		? ""
		: (elements.identityType.value === "MALAYSIAN" ? "e.g. IC No." : "Enter passport number");
	if (!active) {
		elements.identityMask.textContent = "";
		return;
	}
	if (elements.identityType.value !== "MALAYSIAN") {
		elements.identityMask.textContent = "";
		return;
	}
	const template = "XXXXXX-XX-XXXX";
	const value = elements.identityNumber.value;
	elements.identityMask.textContent = [...template]
		.map((character, index) => value[index] ? " " : character)
		.join("");
}

function updatePhoneCountry() {
	if (elements.identityType.value === "MALAYSIAN") {
		return;
	}
	const selected = elements.phoneCountryCode.selectedOptions[0];
	elements.phoneFlag.textContent = countryFlag(selected?.dataset.iso || "MY");
}

function configurePhoneCountry() {
	const isMalaysian = elements.identityType.value === "MALAYSIAN";
	elements.foreignPrefix.closest(".phone-input-group").classList.toggle("foreign-number", !isMalaysian);
	elements.malaysiaPrefix.hidden = !isMalaysian;
	elements.foreignPrefix.hidden = isMalaysian;
	elements.phoneCountryFixed.disabled = !isMalaysian;
	elements.phoneCountryCode.disabled = isMalaysian;
	elements.phoneHint.textContent = isMalaysian
		? "Enter the Malaysian number without the leading 0."
		: "Choose the country prefix, then enter the local mobile number.";
	elements.phoneNumber.placeholder = isMalaysian ? "1123456789" : "Local mobile number";

	if (isMalaysian) {
		elements.phoneNumber.value = elements.phoneNumber.value.replace(/^0+/, "");
	}
	else if (phoneCodeData.length) {
		const foreignCodes = phoneCodeData.filter((code) => code.dialCode !== "+60");
		elements.phoneCountryCode.innerHTML = foreignCodes.map(phoneCodeOption).join("");
	}
	updatePhoneCountry();
}

function phoneCodeOption(code) {
	return `<option value="${escapeHtml(code.dialCode)}" data-iso="${escapeHtml(code.isoCode)}" title="${escapeHtml(code.countryName)}">${escapeHtml(code.dialCode)} — ${escapeHtml(code.countryName)}</option>`;
}

function countryFlag(isoCode) {
	if (!/^[A-Z]{2}$/i.test(isoCode)) return "🌐";
	return [...isoCode.toUpperCase()]
		.map((letter) => String.fromCodePoint(127397 + letter.charCodeAt(0)))
		.join("");
}

function formatMalaysianIdentity(digits) {
	if (digits.length <= 6) return digits;
	if (digits.length <= 8) return `${digits.slice(0, 6)}-${digits.slice(6)}`;
	return `${digits.slice(0, 6)}-${digits.slice(6, 8)}-${digits.slice(8)}`;
}

function validateQueueForm(payload) {
	const valid = validateFields(
		"identityType",
		"identityNumber",
		"phoneCountryCode",
		"phoneNumber",
		"departmentCode",
		payload
	);
	if (!valid) elements.queueForm.querySelector(".has-error input, .has-error select")?.focus();
	return valid;
}

function queueFormPayload() {
	const payload = Object.fromEntries(new FormData(elements.queueForm).entries());
	payload.identityNumber = String(payload.identityNumber || "").replaceAll("-", "");
	payload.phoneNumber = String(payload.phoneNumber || "").replace(/\D/g, "");
	return payload;
}

function getFieldError(field, payload = queueFormPayload()) {
	const validators = {
		identityType: () => payload.identityType ? "" : VALIDATION_MESSAGES.requiredIdentityType,
		identityNumber: () => {
			if (payload.identityType === "MALAYSIAN") {
				if (!/^\d{12}$/.test(payload.identityNumber)) return VALIDATION_MESSAGES.invalidMyKad;
				if (icStateCodes.size && !icStateCodes.has(payload.identityNumber.slice(6, 8))) {
					return VALIDATION_MESSAGES.invalidIdentityNumber;
				}
			}
			if (payload.identityType === "NON_MALAYSIAN" && !/^[A-Z0-9]{5,20}$/.test(payload.identityNumber)) {
				return VALIDATION_MESSAGES.invalidForeignIdentity;
			}
			return "";
		},
		phoneCountryCode: () => payload.phoneCountryCode ? "" : VALIDATION_MESSAGES.requiredCountryCode,
		phoneNumber: () => {
			if (!/^\d{4,15}$/.test(payload.phoneNumber)) return VALIDATION_MESSAGES.invalidPhoneNumber;
			if (payload.phoneCountryCode === "+60" && !/^0?1\d{8,9}$/.test(payload.phoneNumber)) {
				return VALIDATION_MESSAGES.invalidMalaysianPhone;
			}
			return "";
		},
		departmentCode: () => payload.departmentCode ? "" : VALIDATION_MESSAGES.requiredDepartment
	};
	return validators[field]?.() || "";
}

function validateField(field, payload = queueFormPayload()) {
	const message = getFieldError(field, payload);
	if (message) return setFieldError(field, message);
	clearFieldError(field);
	return true;
}

function validateFields(...args) {
	const payload = typeof args.at(-1) === "object" ? args.pop() : queueFormPayload();
	return args.map((field) => validateField(field, payload)).every(Boolean);
}

function setFieldError(field, message) {
	const error = elements.queueForm.querySelector(`[data-error-for="${field}"]`);
	const input = field === "phoneCountryCode" ? elements.phoneCountryCode : elements.queueForm.elements[field];
	if (error) error.textContent = message;
	input?.closest(".field-group")?.classList.add("has-error");
	input?.setAttribute("aria-invalid", "true");
	return false;
}

function clearFieldError(field) {
	const error = elements.queueForm.querySelector(`[data-error-for="${field}"]`);
	const input = field === "phoneCountryCode" ? elements.phoneCountryCode : elements.queueForm.elements[field];
	if (error) error.textContent = "";
	input?.setAttribute("aria-invalid", "false");
	if (input?.closest(".field-group")?.querySelectorAll(".field-error:not(:empty)").length === 0) {
		input.closest(".field-group").classList.remove("has-error");
	}
}

function clearFormErrors() {
	["identityType", "identityNumber", "phoneCountryCode", "phoneNumber", "departmentCode"].forEach(clearFieldError);
	elements.formError.hidden = true;
	elements.formError.textContent = "";
}

function displayApiError(error) {
	if (error.errors && Object.keys(error.errors).length) {
		Object.entries(error.errors).forEach(([field, message]) => setFieldError(field, message));
		return;
	}
	const fieldByMessage = {
		"Please enter a valid identity or passport number.": "identityNumber",
		"Please enter a valid passport or foreign identity number.": "identityNumber",
		"Please select a valid country code.": "phoneCountryCode",
		"Please enter a valid mobile number.": "phoneNumber",
		"Department is required": "departmentCode"
	};
	const field = fieldByMessage[error.message];
	if (field) setFieldError(field, error.message);
	else {
		elements.formError.textContent = error.message;
		elements.formError.hidden = false;
	}
}

async function loadPhoneCodes() {
	try {
		phoneCodeData = await requestJson(API.phoneCodes);
		configurePhoneCountry();
	}
	catch (error) { showToast(error.message, "error"); }
}

async function loadIcStates() {
	try {
		const states = await requestJson(API.icStates);
		icStateCodes = new Set(states.map((state) => state.code));
		if (elements.identityNumber.value) validateField("identityNumber");
	}
	catch (error) { showToast(error.message, "error"); }
}

async function loadCurrentQueues() {
	try {
		const queues = await requestJson(API.currentQueues);
		elements.currentQueueList.innerHTML = queues.map((queue, index) => `
			<div class="queue-row" style="--row-index: ${index}">
				<img class="dept-logo" src="${departmentLogo(queue.departmentCode)}" alt="${escapeHtml(queue.departmentName)} logo" loading="lazy">
				<div>
					<h3>${escapeHtml(queue.departmentName)}</h3>
					<p>${queue.waitingCount} waiting · ${queue.usedSlots}/${queue.dailyQuota} slots used</p>
					<p>${queue.currentQueueNumber ? `${escapeHtml(queue.counterName || "Unassigned Counter")} · ${escapeHtml(queue.status || "-")}` : "No active service"}</p>
				</div>
				<div class="current-number">${queue.currentQueueNumber || "-"}</div>
			</div>
		`).join("");
	}
	catch (error) {
		elements.currentQueueList.innerHTML = `<p>${escapeHtml(error.message)}</p>`;
	}
}

async function requestJson(url, options = {}) {
	const response = await fetch(url, options);
	const isJson = response.headers.get("content-type")?.includes("application/json");
	const body = isJson ? await response.json() : null;

	if (!response.ok) {
		const error = new Error(resolveErrorMessage(body, response.statusText));
		error.errors = body?.errors || body?.validationErrors || {};
		throw error;
	}

	return body?.success === true ? body.data : body;
}

function resolveErrorMessage(body, fallback) {
	if (body?.validationErrors && Object.keys(body.validationErrors).length > 0) {
		return Object.values(body.validationErrors).join(" ");
	}
	return body?.message || fallback || "Request failed.";
}

function renderTicketResult(ticket) {
	elements.takePanel.classList.add("queue-result-active");
	elements.ticketResult.hidden = false;
	elements.ticketResult.innerHTML = `
		<div class="ticket-header">
			<img class="ticket-logo" src="${departmentLogo(ticket.departmentCode)}" alt="${escapeHtml(ticket.departmentName)} logo">
			<div>
				<p>Your Queue Number</p>
				<div class="ticket-number">${ticket.queueNumber}</div>
				<p>${escapeHtml(ticket.departmentName)} · ${formatTime(ticket.createdAt)}</p>
				<p>${ticket.peopleAhead} ahead</p>
				<span class="status-pill">${ticket.status}</span>
			</div>
		</div>
		${renderPatientTicketActions(ticket)}
	`;
}

function renderPatientTicketActions(ticket) {
	return `
		<div class="ticket-actions">
			${ticket.status === "WAITING"
				? `<button class="danger-action" type="button" data-cancel-queue="${escapeHtml(ticket.queueNumber)}">Cancel Queue</button>`
				: ""}
			<button class="secondary-action" type="button" data-back-to-form>Back</button>
		</div>
	`;
}

function showQueueForm() {
	elements.takePanel.classList.remove("queue-result-active");
	elements.ticketResult.hidden = true;
	elements.ticketResult.innerHTML = "";
	elements.queueForm.reset();
	configureIdentityInput();
	configurePhoneCountry();
	renderDepartmentPreview(null);
	clearFormErrors();
	elements.identityType.focus();
}

function renderTicketDetail(ticket) {
	elements.checkResult.hidden = false;
	elements.checkResult.innerHTML = `
		<div class="ticket-header">
			<img class="ticket-logo" src="${departmentLogo(ticket.departmentCode)}" alt="${escapeHtml(ticket.departmentName)} logo">
			<div>
				<div class="ticket-number">${ticket.queueNumber}</div>
				<span class="status-pill">${ticket.status}</span>
			</div>
		</div>
		<div class="detail-grid">
			${detailItem("Department", ticket.departmentName)}
			${detailItem("People Ahead", String(ticket.peopleAhead))}
			${detailItem("Counter", ticket.counterName || "-")}
			${detailItem("Registered", formatTime(ticket.createdAt))}
		</div>
		${renderCancelAction(ticket)}
	`;
}

function renderCancelAction(ticket) {
	if (ticket.status !== "WAITING") {
		return "";
	}
	return `
		<div class="ticket-actions">
			<button class="danger-action" type="button" data-cancel-queue="${escapeHtml(ticket.queueNumber)}">Cancel Queue</button>
		</div>
	`;
}

function renderDepartmentPreview(department) {
	if (!department) {
		elements.departmentPreview.innerHTML = `
			<div class="department-logo-frame empty">
				<span>Dept</span>
			</div>
			<div>
				<strong>Select a department</strong>
				<p>The matching clinic logo will appear here.</p>
			</div>
		`;
		return;
	}

	elements.departmentPreview.innerHTML = `
		<img class="department-logo-large" src="${departmentLogo(department.code)}" alt="${escapeHtml(department.name)} logo">
		<div>
			<strong>${escapeHtml(department.name)}</strong>
			<p>${department.dailyQuota} slots available daily · ${department.code}</p>
		</div>
	`;
}

function departmentLogo(code) {
	return DEPARTMENT_LOGOS[code] || `${CONTEXT_PATH}/assets/hospital-logo.png?v=20260530-3`;
}

function detailItem(label, value) {
	return `
		<div class="detail-item">
			<span>${label}</span>
			<strong>${escapeHtml(value)}</strong>
		</div>
	`;
}

function formatTime(value) {
	if (!value) {
		return "-";
	}
	return new Intl.DateTimeFormat("en-MY", {
		hour: "2-digit",
		minute: "2-digit",
		hour12: false
	}).format(new Date(value));
}

function formatEstimatedWait(ticket) {
	if (ticket.estimatedWaitMinutes === null || ticket.estimatedWaitMinutes === undefined) {
		return `${Number(ticket.peopleAhead || 0) * 15} min`;
	}
	return `${ticket.estimatedWaitMinutes} min`;
}

function showToast(message, type = "") {
	elements.toast.textContent = message;
	elements.toast.className = `toast ${type}`.trim();
	elements.toast.hidden = false;
	window.clearTimeout(showToast.timeoutId);
	showToast.timeoutId = window.setTimeout(() => {
		elements.toast.hidden = true;
	}, 4200);
}

function escapeHtml(value) {
	return String(value)
		.replaceAll("&", "&amp;")
		.replaceAll("<", "&lt;")
		.replaceAll(">", "&gt;")
		.replaceAll('"', "&quot;")
		.replaceAll("'", "&#039;");
}
