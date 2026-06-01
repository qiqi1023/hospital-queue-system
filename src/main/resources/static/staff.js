const API = {
	departments: "/api/departments",
	currentQueues: "/api/departments/current-queues",
	currentServices: "/api/queues/current-services",
	queues: "/api/queues",
	ticket: (queueNumber) => `/api/queues/${encodeURIComponent(queueNumber)}`,
	nextCall: "/api/queues/next-call",
	updateStatus: (queueNumber) => `/api/queues/${encodeURIComponent(queueNumber)}/status`
};

const DEFAULT_COUNTERS = ["Counter 1", "Counter 2", "Counter 3", "Counter 4", "Counter 5"];

const VALID_STATUS_ACTIONS = {
	WAITING: [
		{ status: "CANCELLED", label: "Cancel Ticket", danger: true }
	],
	CALLED: [
		{ status: "SERVING", label: "Start Serving" },
		{ status: "COMPLETED", label: "Mark Completed" },
		{ status: "MISSED", label: "Mark Missed", danger: true },
		{ status: "CANCELLED", label: "Cancel Ticket", danger: true }
	],
	SERVING: [
		{ status: "COMPLETED", label: "Mark Completed" }
	],
	COMPLETED: [],
	MISSED: [],
	CANCELLED: []
};

const DEPARTMENT_LOGOS = {
	GEN: "/assets/departments/general-consult-logo.png?v=20260530-3",
	PHA: "/assets/departments/pharmacy-logo.png?v=20260530-3",
	DEN: "/assets/departments/dental-logo.png?v=20260530-3",
	LAB: "/assets/departments/blood-test-logo.png?v=20260530-3",
	SPC: "/assets/departments/specialist-logo.png?v=20260530-3"
};

const elements = {
	clock: document.querySelector("#clock"),
	tabButtons: document.querySelectorAll("[data-staff-tab]"),
	tabPanels: document.querySelectorAll("[data-staff-panel]"),
	staffCallForm: document.querySelector("#staff-call-form"),
	staffCallResult: document.querySelector("#staff-call-result"),
	staffCounterSelect: document.querySelector("#staff-counter-select"),
	statusLookupForm: document.querySelector("#status-lookup-form"),
	statusUpdateResult: document.querySelector("#status-update-result"),
	statusActionPanel: document.querySelector("#status-action-panel"),
	queueListForm: document.querySelector("#queue-list-form"),
	staffTicketList: document.querySelector("#staff-ticket-list"),
	currentQueueList: document.querySelector("#current-queue-list"),
	counterServiceBoard: document.querySelector("#counter-service-board"),
	toast: document.querySelector("#toast")
};

document.addEventListener("DOMContentLoaded", () => {
	document.body.classList.add("is-ready");
	bindStaffTabs();
	bindStaffCallForm();
	bindStatusLookupForm();
	bindStatusWorkflowActions();
	bindQueueListForm();
	bindGlobalJumpToStatus();
	startClock();
	loadDepartments();
	refreshStaffDashboard();
	setInterval(refreshStaffDashboard, 10000);
});

function bindStaffTabs() {
	elements.tabButtons.forEach((button) => {
		button.addEventListener("click", () => showStaffTab(button.dataset.staffTab));
	});
}

function showStaffTab(tabName) {
	elements.tabButtons.forEach((button) => {
		button.classList.toggle("active", button.dataset.staffTab === tabName);
	});
	elements.tabPanels.forEach((panel) => {
		panel.classList.toggle("active", panel.dataset.staffPanel === tabName);
	});
	if (tabName === "queue-list") {
		loadStaffTicketList();
	}
}

function bindStaffCallForm() {
	elements.staffCallForm.addEventListener("submit", async (event) => {
		event.preventDefault();
		const payload = Object.fromEntries(new FormData(elements.staffCallForm).entries());

		try {
			const ticket = await requestJson(API.nextCall, {
				method: "PUT",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(payload)
			});
			renderStaffCallResult(ticket);
			showToast(`Now calling ${ticket.queueNumber} at ${ticket.counterName}.`, "success");
			await refreshStaffDashboard();
			loadStaffTicketList();
		}
		catch (error) {
			elements.staffCallResult.hidden = false;
			elements.staffCallResult.innerHTML = `<p>${escapeHtml(error.message)}</p>`;
			showToast(error.message, "error");
		}
	});
}

function bindStatusLookupForm() {
	elements.statusLookupForm.addEventListener("submit", async (event) => {
		event.preventDefault();
		const formData = new FormData(elements.statusLookupForm);
		const queueNumber = String(formData.get("queueNumber") || "").trim().toUpperCase();
		await loadStatusTicket(queueNumber);
	});
}

function bindStatusWorkflowActions() {
	elements.statusActionPanel.addEventListener("click", async (event) => {
		const button = event.target.closest("[data-workflow-status-action]");
		if (!button) {
			return;
		}
		await updateTicketStatus(button.dataset.queueNumber, button.dataset.workflowStatusAction);
	});
}

function bindQueueListForm() {
	elements.queueListForm.addEventListener("submit", async (event) => {
		event.preventDefault();
		await loadStaffTicketList();
	});
}

function bindGlobalJumpToStatus() {
	document.addEventListener("click", async (event) => {
		const trigger = event.target.closest("[data-jump-to-status]");
		if (!trigger) {
			return;
		}

		event.preventDefault();
		console.log("Ticket row clicked! Jumping to status for:", trigger.dataset.jumpToStatus);
		const queueNumber = trigger.dataset.jumpToStatus;
		
		let targetTabName = "status";
		elements.tabPanels.forEach((panel) => {
			if (panel.contains(elements.statusLookupForm) || panel === elements.statusLookupForm) {
				targetTabName = panel.dataset.staffPanel;
			}
		});
		showStaffTab(targetTabName);
		window.scrollTo({ top: 0, behavior: "smooth" });

		const input = elements.statusLookupForm.querySelector("[name='queueNumber']");
		if (input) {
			input.value = queueNumber;
		}

		await loadStatusTicket(queueNumber);
	});
}

async function loadStatusTicket(queueNumber) {
	if (!queueNumber) {
		return;
	}
	try {
		const ticket = await requestJson(API.ticket(queueNumber));
		renderStatusTicket(ticket);
	}
	catch (error) {
		elements.statusUpdateResult.hidden = false;
		elements.statusUpdateResult.innerHTML = `<p>${escapeHtml(error.message)}</p>`;
		elements.statusActionPanel.hidden = true;
		showToast(error.message, "error");
	}
}

async function updateTicketStatus(queueNumber, status) {
	try {
		const ticket = await requestJson(API.updateStatus(queueNumber), {
			method: "PUT",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ status })
		});
		renderStatusTicket(ticket, "Ticket Updated");
		showToast(`${ticket.queueNumber} updated to ${ticket.status}.`, "success");
		await refreshStaffDashboard();
		loadStaffTicketList();
	}
	catch (error) {
		showToast(error.message, "error");
	}
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
		const departmentOptions = [
			`<option value="">Select department</option>`,
			...departments.map((department) =>
				`<option value="${department.code}">${escapeHtml(department.name)} (${department.code})</option>`
			)
		].join("");
		document.querySelectorAll("#staff-department-select, .department-select").forEach((select) => {
			select.innerHTML = departmentOptions;
		});
	}
	catch (error) {
		showToast(error.message, "error");
	}
}

async function loadStaffTicketList() {
	if (!elements.queueListForm || !elements.staffTicketList) {
		return;
	}

	const formData = new FormData(elements.queueListForm);
	const department = String(formData.get("department") || "").trim();
	const status = String(formData.get("status") || "").trim();
	if (!department) {
		elements.staffTicketList.innerHTML = `<p>Select a department to view tickets.</p>`;
		return;
	}

	try {
		const params = new URLSearchParams({ department });
		if (status) {
			params.set("status", status);
		}
		const tickets = await requestJson(`${API.queues}?${params.toString()}`);
		renderStaffTicketList(tickets);
	}
	catch (error) {
		elements.staffTicketList.innerHTML = `<p>${escapeHtml(error.message)}</p>`;
		showToast(error.message, "error");
	}
}

async function refreshStaffDashboard() {
	await Promise.all([loadCurrentQueues(), loadCurrentServices()]);
}

async function loadCurrentServices() {
	try {
		const services = await requestJson(API.currentServices);
		renderCounterServiceBoard(services);
		renderCounterOptions(services);
	}
	catch (error) {
		elements.counterServiceBoard.innerHTML = `<p>${escapeHtml(error.message)}</p>`;
	}
}

async function loadCurrentQueues() {
	try {
		const queues = await requestJson(API.currentQueues);
		elements.currentQueueList.innerHTML = queues.map((queue, index) => `
			<div class="queue-row" style="--row-index: ${index}${queue.currentQueueNumber ? '; cursor: pointer;' : ''}" ${queue.currentQueueNumber ? `title="Click to update status" data-jump-to-status="${escapeHtml(queue.currentQueueNumber)}"` : ''}>
				<img class="dept-logo" src="${departmentLogo(queue.departmentCode)}" alt="${escapeHtml(queue.departmentName)} logo" loading="lazy">
				<div>
					<h3>${escapeHtml(queue.departmentName)}</h3>
					<p>${queue.waitingCount} waiting · ${queue.usedSlots}/${queue.dailyQuota} slots used</p>
					<p>${queue.currentQueueNumber ? `${escapeHtml(queue.counterName || "Unassigned Counter")} · ${escapeHtml(queue.serviceStatus || "-")}` : "No active service"}</p>
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
	const response = await fetch(url, { cache: "no-store", ...options });
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

function renderStaffCallResult(ticket) {
	elements.staffCallResult.hidden = false;
	elements.staffCallResult.innerHTML = ticketSummary(ticket, "Now Calling");
}

function renderStatusTicket(ticket, heading = "Ticket Status") {
	elements.statusUpdateResult.hidden = false;
	elements.statusUpdateResult.innerHTML = ticketSummary(ticket, heading);
	renderStatusWorkflowActions(ticket);
}

function renderStatusWorkflowActions(ticket) {
	const actions = VALID_STATUS_ACTIONS[ticket.status] || [];
	elements.statusActionPanel.hidden = false;
	if (!actions.length) {
		elements.statusActionPanel.innerHTML = `
			<h3>No Further Action Required</h3>
			<p class="muted-copy">${escapeHtml(ticket.queueNumber)} is already ${escapeHtml(ticket.status)}. This ticket has left the active queue.</p>
		`;
		return;
	}

	elements.statusActionPanel.innerHTML = `
		<h3>Available Actions for ${escapeHtml(ticket.queueNumber)}</h3>
		<p class="muted-copy">Current status: <strong>${escapeHtml(ticket.status)}</strong></p>
		<div class="ticket-actions">
			${actions.map((action) => `
				<button class="${action.danger ? "danger-action" : "secondary-action"}" type="button"
					data-workflow-status-action="${escapeHtml(action.status)}"
					data-queue-number="${escapeHtml(ticket.queueNumber)}">${escapeHtml(action.label)}</button>
			`).join("")}
		</div>
	`;
}

function renderCounterServiceBoard(services) {
	const servicesByCounter = new Map();
	services.forEach((service) => {
		const key = normalizeCounterName(service.counterName);
		if (key) {
			servicesByCounter.set(key, service);
		}
	});

	const configuredCounters = [...DEFAULT_COUNTERS];
	services.forEach((service) => {
		const counterName = String(service.counterName || "").trim();
		if (counterName && !configuredCounters.some((name) => normalizeCounterName(name) === normalizeCounterName(counterName))) {
			configuredCounters.push(counterName);
		}
	});

	elements.counterServiceBoard.innerHTML = configuredCounters.map((counterName, index) => {
		const service = servicesByCounter.get(normalizeCounterName(counterName));
		if (!service) {
			return `
				<article class="counter-service-card available" style="--row-index: ${index}">
					<div class="counter-service-topline">
						<strong>${escapeHtml(counterName)}</strong>
						<span class="availability-pill">AVAILABLE</span>
					</div>
					<div class="counter-ticket-number available-counter">Ready</div>
					<p>No active patient assigned.</p>
				</article>
			`;
		}
		return `
			<article class="counter-service-card" style="--row-index: ${index}; cursor: pointer;" title="Click to update status" data-jump-to-status="${escapeHtml(service.queueNumber)}">
				<div class="counter-service-topline">
					<strong>${escapeHtml(service.counterName || counterName)}</strong>
					<span class="status-pill">${escapeHtml(service.status)}</span>
				</div>
				<div class="counter-ticket-number">${escapeHtml(service.queueNumber)}</div>
				<p>${escapeHtml(service.departmentName)}</p>
				<p>${escapeHtml(service.patientName)}</p>
				<small>Called at ${formatTime(service.calledAt)}</small>
			</article>
		`;
	}).join("");
}

function renderCounterOptions(services) {
	const busyCounters = new Set(services.map((service) => normalizeCounterName(service.counterName)).filter(Boolean));
	const currentValue = elements.staffCounterSelect.value;
	const options = [
		`<option value="">Select available counter</option>`,
		...DEFAULT_COUNTERS.map((counterName) => {
			const busy = busyCounters.has(normalizeCounterName(counterName));
			return `<option value="${escapeHtml(counterName)}" ${busy ? "disabled" : ""}>${escapeHtml(counterName)}${busy ? " · Busy" : " · Available"}</option>`;
		})
	];
	elements.staffCounterSelect.innerHTML = options.join("");
	if (currentValue && !busyCounters.has(normalizeCounterName(currentValue))) {
		elements.staffCounterSelect.value = currentValue;
	}
}

function renderStaffTicketList(tickets) {
	if (tickets.length === 0) {
		elements.staffTicketList.innerHTML = `<p>No tickets found.</p>`;
		return;
	}

	elements.staffTicketList.innerHTML = tickets.map((ticket, index) => `
		<div class="queue-row" style="--row-index: ${index}; cursor: pointer;" title="Click to update status" data-jump-to-status="${escapeHtml(ticket.queueNumber)}">
			<img class="dept-logo" src="${departmentLogo(ticket.departmentCode)}" alt="${escapeHtml(ticket.departmentName)} logo" loading="lazy">
			<div>
				<h3>${escapeHtml(ticket.queueNumber)} - ${escapeHtml(ticket.patientName)}</h3>
				<p>${escapeHtml(ticket.departmentName)} · ${escapeHtml(ticket.priorityCategory)} · ${ticket.peopleAhead} ahead</p>
				<p>${ticket.counterName ? escapeHtml(ticket.counterName) : "No counter assigned"}</p>
			</div>
			<div class="current-number">${escapeHtml(ticket.status)}</div>
		</div>
	`).join("");
}

function ticketSummary(ticket, heading) {
	return `
		<div class="ticket-header">
			<img class="ticket-logo" src="${departmentLogo(ticket.departmentCode)}" alt="${escapeHtml(ticket.departmentName)} logo">
			<div>
				<p>${escapeHtml(heading)}</p>
				<div class="ticket-number">${escapeHtml(ticket.queueNumber)}</div>
				<span class="status-pill">${escapeHtml(ticket.status)}</span>
			</div>
		</div>
		<div class="detail-grid">
			${detailItem("Patient", ticket.patientName)}
			${detailItem("Department", ticket.departmentName)}
			${detailItem("Priority", ticket.priorityCategory)}
			${detailItem("People Ahead", String(ticket.peopleAhead))}
			${detailItem("Estimated Wait", formatEstimatedWait(ticket))}
			${detailItem("Counter", ticket.counterName || "-")}
			${detailItem("Called At", formatTime(ticket.calledAt))}
			${detailItem("Completed At", formatTime(ticket.completedAt))}
		</div>
	`;
}

function normalizeCounterName(value) {
	return String(value || "").trim().toLowerCase();
}

function departmentLogo(code) {
	return DEPARTMENT_LOGOS[code] || "/assets/hospital-logo.png?v=20260530-3";
}

function detailItem(label, value) {
	return `
		<div class="detail-item">
			<span>${escapeHtml(label)}</span>
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
