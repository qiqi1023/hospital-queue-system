const CONTEXT_PATH = document.querySelector('meta[name="context-path"]')?.content || "";

const API = {
	departments: `${CONTEXT_PATH}/api/departments`,
	counters: `${CONTEXT_PATH}/api/counters`,
	currentQueues: `${CONTEXT_PATH}/api/queues/current`,
	currentServices: `${CONTEXT_PATH}/api/queues/current`,
	queues: `${CONTEXT_PATH}/api/queueTickets`,
	ticket: (queueNumber) => `${CONTEXT_PATH}/api/queueTickets/${encodeURIComponent(queueNumber)}`,
	nextCall: `${CONTEXT_PATH}/api/queueCalls`,
	callTicket: (queueNumber) => `${CONTEXT_PATH}/api/queueTickets/${encodeURIComponent(queueNumber)}/call`,
	updateStatus: (queueNumber) => `${CONTEXT_PATH}/api/queueTickets/${encodeURIComponent(queueNumber)}/status`
};

let counterData = [];
let activeServices = [];
let pendingCallQueueNumber = null;

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
	MISSED: [
		{ status: "WAITING", label: "Return to Queue" }
	],
	CANCELLED: []
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
	bindRedirectCall();
	bindQueueListForm();
	bindGlobalJumpToStatus();
	startClock();
	initializeStaffData();
	setInterval(refreshStaffDashboard, 10000);
});

async function initializeStaffData() {
	await Promise.all([loadDepartments(), loadCounters()]);
	await refreshStaffDashboard();
}

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
	document.querySelector("#staff-department-select")?.addEventListener("change", () => {
		renderCounterOptions(activeServices);
	});
	elements.staffCallForm.addEventListener("submit", async (event) => {
		event.preventDefault();
		const payload = Object.fromEntries(new FormData(elements.staffCallForm).entries());

		try {
			let ticket;
			if (pendingCallQueueNumber) {
				// Call the specific ticket that was selected from the status panel
				ticket = await requestJson(API.callTicket(pendingCallQueueNumber), {
					method: "POST",
					headers: { "Content-Type": "application/json" },
					body: JSON.stringify({ counterName: payload.counterName })
				});
				// clear pending after successful call
				pendingCallQueueNumber = null;
			} else {
				ticket = await requestJson(API.nextCall, {
					method: "POST",
					headers: { "Content-Type": "application/json" },
					body: JSON.stringify(payload)
				});
			}
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
		const workflowButton = event.target.closest("[data-workflow-status-action]");
		if (workflowButton) {
			event.preventDefault();
			await updateTicketStatus(workflowButton.dataset.queueNumber, workflowButton.dataset.workflowStatusAction);
			return;
		}

		const callButton = event.target.closest("[data-call-ticket]");
		if (callButton) {
			event.preventDefault();
			event.stopPropagation();
			const queueNumber = callButton.dataset.queueNumber;
			const counterSelect = document.querySelector("#status-action-panel select[data-counter-select]");
			const counterName = counterSelect?.value || "";
			await callTicket(queueNumber, counterName);
		}
	});
}

// Handle redirect action from status panel to Call Next panel
function bindRedirectCall() {
	elements.statusActionPanel.addEventListener("click", (event) => {
		const redirect = event.target.closest("[data-redirect-call]");
		if (!redirect) return;
		event.preventDefault();
		const dept = redirect.dataset.departmentCode;
		const qnum = redirect.dataset.queueNumber;
		const deptSelect = document.querySelector("#staff-department-select");
		if (deptSelect) {
			deptSelect.value = dept;
			renderCounterOptions(activeServices);
			// auto-select first available OPEN counter for the department
			const busyCounters = new Set(activeServices.map((service) => normalizeCounterName(service.counterName)).filter(Boolean));
			const matchingCounters = counterData.filter((counter) => counter.departmentCode === dept);
			const available = matchingCounters.find((counter) => counter.status === "OPEN" && !busyCounters.has(normalizeCounterName(counter.name)));
			const counterSelect = document.querySelector("#staff-counter-select");
			if (available && counterSelect) {
				counterSelect.value = available.name;
			}
		}
		// store pending ticket so call-next form will call this ticket instead of oldest
		pendingCallQueueNumber = qnum || null;
		showStaffTab("call-next");
		// focus the counter select on the call-next panel
		setTimeout(() => {
			const sel = document.querySelector("#staff-counter-select");
			if (sel) sel.focus();
		}, 50);
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
		if (event.target.closest("button,select,input,textarea")) {
			return;
		}
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
			method: "PATCH",
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

async function callTicket(queueNumber, counterName) {
	if (!counterName) {
		showToast("Please select a counter before calling the ticket.", "error");
		return;
	}

	try {
		const ticket = await requestJson(API.callTicket(queueNumber), {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ counterName })
		});
		renderStatusTicket(ticket, "Ticket Called");
		showToast(`Now calling ${ticket.queueNumber} at ${ticket.counterName}.`, "success");
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

async function loadCounters() {
	try {
		counterData = await requestJson(API.counters);
		renderCounterServiceBoard(activeServices);
		renderCounterOptions(activeServices);
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
		const params = new URLSearchParams({ departmentCode: department });
		if (status) {
			params.set("status", status);
		}
		const tickets = await requestJson(`${API.queues}?${params.toString()}`);
		renderStaffTicketList(tickets.content || []);
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
		const queues = await requestJson(API.currentServices);
		activeServices = queues.filter((queue) => queue.currentQueueNumber).map((queue) => ({
			counterName: queue.counterName, queueNumber: queue.currentQueueNumber,
			departmentCode: queue.departmentCode, departmentName: queue.departmentName,
			status: queue.status, calledAt: null
		}));
		renderCounterServiceBoard(activeServices);
		renderCounterOptions(activeServices);
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
	const response = await fetch(url, { cache: "no-store", ...options });
	const isJson = response.headers.get("content-type")?.includes("application/json");
	const body = isJson ? await response.json() : null;

	if (!response.ok) {
		throw new Error(resolveErrorMessage(body, response.statusText));
	}

	return body?.success === true ? body.data : body;
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
	if (!actions.length && ticket.status !== "WAITING") {
		elements.statusActionPanel.innerHTML = `
			<h3>No Further Action Required</h3>
			<p class="muted-copy">${escapeHtml(ticket.queueNumber)} is already ${escapeHtml(ticket.status)}. This ticket has left the active queue.</p>
		`;
		return;
	}

	const callTicketControls = ticket.status === "WAITING" ? renderCallTicketControls(ticket.departmentCode, ticket.queueNumber) : "";
	elements.statusActionPanel.innerHTML = `
		<h3>Available Actions for ${escapeHtml(ticket.queueNumber)}</h3>
		<p class="muted-copy">Current status: <strong>${escapeHtml(ticket.status)}</strong></p>
		<div class="ticket-actions">
			${callTicketControls}
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

	elements.counterServiceBoard.innerHTML = counterData.map((counter, index) => {
		const counterName = counter.name;
		const service = servicesByCounter.get(normalizeCounterName(counterName));
		if (!service) {
			const isOpen = counter.status === "OPEN";
			return `
				<article class="counter-service-card ${isOpen ? "available" : ""}" style="--row-index: ${index}">
					<div class="counter-service-topline">
						<strong>${escapeHtml(counterName)}</strong>
						<span class="availability-pill">${escapeHtml(counter.status)}</span>
					</div>
					<div class="counter-ticket-number available-counter">${isOpen ? "Ready" : "Unavailable"}</div>
					<p>${escapeHtml(counter.departmentCode)} · No active patient assigned.</p>
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
				<small>Called at ${formatTime(service.calledAt)}</small>
			</article>
		`;
	}).join("");
}

function renderCounterOptions(services) {
	const busyCounters = new Set(services.map((service) => normalizeCounterName(service.counterName)).filter(Boolean));
	const currentValue = elements.staffCounterSelect.value;
	const departmentCode = document.querySelector("#staff-department-select")?.value || "";
	const matchingCounters = departmentCode
		? counterData.filter((counter) => counter.departmentCode === departmentCode)
		: [];
	const options = [
		`<option value="">${departmentCode ? "Select available counter" : "Select a department first"}</option>`,
		...matchingCounters.map((counter) => {
			const busy = busyCounters.has(normalizeCounterName(counter.name));
			const unavailable = busy || counter.status !== "OPEN";
			const label = busy ? "Busy" : counter.status === "OPEN" ? "Available" : counter.status;
			return `<option value="${escapeHtml(counter.name)}" ${unavailable ? "disabled" : ""}>${escapeHtml(counter.name)} · ${escapeHtml(label)}</option>`;
		})
	];
	elements.staffCounterSelect.innerHTML = options.join("");
	if (currentValue && matchingCounters.some((counter) => counter.name === currentValue)
			&& !busyCounters.has(normalizeCounterName(currentValue))) {
		elements.staffCounterSelect.value = currentValue;
	}
}

function renderCallTicketControls(departmentCode, queueNumber) {
	// Instead of allowing selecting a counter here, redirect staff to the Call Next panel
	// where they can choose a counter and perform the call. This prevents calling from
	// the status panel and ensures consistent counter assignment flow.
	return `
		<div class="ticket-call-panel">
			<button class="secondary-action" type="button" data-redirect-call data-department-code="${escapeHtml(departmentCode)}" data-queue-number="${escapeHtml(queueNumber)}">Call on Counter</button>
		</div>
	`;
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
				<h3>${escapeHtml(ticket.queueNumber)}</h3>
				<p>${escapeHtml(ticket.departmentName)} · ${ticket.peopleAhead} ahead</p>
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
			${detailItem("Department", ticket.departmentName)}
			${detailItem("People Ahead", String(ticket.peopleAhead))}
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
	return DEPARTMENT_LOGOS[code] || `${CONTEXT_PATH}/assets/hospital-logo.png?v=20260530-3`;
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
