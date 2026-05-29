const API = {
	departments: "/api/departments",
	currentQueues: "/api/departments/current-queues",
	queues: "/api/queues"
};

const DEPARTMENT_LOGOS = {
	GEN: "/assets/departments/general-consult-logo.png?v=20260530-2",
	PHA: "/assets/departments/pharmacy-logo.png?v=20260530-2",
	DEN: "/assets/departments/dental-logo.png?v=20260530-2",
	LAB: "/assets/departments/blood-test-logo.png?v=20260530-2",
	SPC: "/assets/departments/specialist-logo.png?v=20260530-2"
};

const elements = {
	clock: document.querySelector("#clock"),
	departmentSelect: document.querySelector("#department-select"),
	departmentPreview: document.querySelector("#department-preview"),
	quotaSummary: document.querySelector("#quota-summary"),
	queueForm: document.querySelector("#queue-form"),
	checkForm: document.querySelector("#check-form"),
	ticketResult: document.querySelector("#ticket-result"),
	checkResult: document.querySelector("#check-result"),
	currentQueueList: document.querySelector("#current-queue-list"),
	toast: document.querySelector("#toast")
};

document.addEventListener("DOMContentLoaded", () => {
	bindTabs();
	bindForms();
	startClock();
	loadDepartments();
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
		const payload = Object.fromEntries(new FormData(elements.queueForm).entries());

		try {
			const ticket = await requestJson(API.queues, {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(payload)
			});
			renderTicketResult(ticket);
			elements.queueForm.reset();
			showToast(`Queue number ${ticket.queueNumber} created.`, "success");
			loadCurrentQueues();
		}
		catch (error) {
			showToast(error.message, "error");
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

function startClock() {
	updateClock();
	setInterval(updateClock, 1000);
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
		elements.departmentSelect.innerHTML = [
			`<option value="">Select department</option>`,
			...departments.map((department) =>
				`<option value="${department.code}">${escapeHtml(department.name)} (${department.code})</option>`
			)
		].join("");

		elements.departmentSelect.addEventListener("change", () => {
			const selected = departments.find((department) => department.code === elements.departmentSelect.value);
			renderDepartmentPreview(selected);
		});

		elements.quotaSummary.innerHTML = departments.map((department) =>
			`<li class="quota-item">
				<img src="${departmentLogo(department.code)}" alt="" loading="lazy">
				<span>${escapeHtml(department.name)}: ${department.dailyQuota} slots daily</span>
			</li>`
		).join("");
	}
	catch (error) {
		showToast(error.message, "error");
	}
}

async function loadCurrentQueues() {
	try {
		const queues = await requestJson(API.currentQueues);
		elements.currentQueueList.innerHTML = queues.map((queue) => `
			<div class="queue-row">
				<img class="dept-logo" src="${departmentLogo(queue.departmentCode)}" alt="${escapeHtml(queue.departmentName)} logo" loading="lazy">
				<div>
					<h3>${escapeHtml(queue.departmentName)}</h3>
					<p>${queue.waitingCount} waiting · ${queue.usedSlots}/${queue.dailyQuota} slots used</p>
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
		throw new Error(resolveErrorMessage(body, response.statusText));
	}

	return body;
}

function resolveErrorMessage(body, fallback) {
	if (body?.validationErrors && Object.keys(body.validationErrors).length > 0) {
		return Object.values(body.validationErrors).join(" ");
	}
	return body?.message || fallback || "Request failed.";
}

function renderTicketResult(ticket) {
	elements.ticketResult.hidden = false;
	elements.ticketResult.innerHTML = `
		<div class="ticket-header">
			<img class="ticket-logo" src="${departmentLogo(ticket.departmentCode)}" alt="${escapeHtml(ticket.departmentName)} logo">
			<div>
				<p>Your Queue Number</p>
				<div class="ticket-number">${ticket.queueNumber}</div>
				<p>${escapeHtml(ticket.departmentName)} · ${formatTime(ticket.createdAt)} · ${ticket.priorityCategory}</p>
				<span class="status-pill">${ticket.status}</span>
			</div>
		</div>
	`;
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
			${detailItem("Patient", ticket.patientName)}
			${detailItem("Department", ticket.departmentName)}
			${detailItem("Priority", ticket.priorityCategory)}
			${detailItem("People Ahead", String(ticket.peopleAhead))}
			${detailItem("Counter", ticket.counterName || "-")}
			${detailItem("Registered", formatTime(ticket.createdAt))}
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
	return DEPARTMENT_LOGOS[code] || "/assets/hospital-logo.png?v=20260530-2";
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
