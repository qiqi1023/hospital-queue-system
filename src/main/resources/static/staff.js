const API = {
	departments: "/api/departments",
	currentQueues: "/api/departments/current-queues",
	nextCall: "/api/queues/next-call",
	updateStatus: (queueNumber) => `/api/queues/${encodeURIComponent(queueNumber)}/status`
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
	staffDepartmentSelect: document.querySelector("#staff-department-select"),
	staffCallForm: document.querySelector("#staff-call-form"),
	staffCallResult: document.querySelector("#staff-call-result"),
	statusUpdateForm: document.querySelector("#status-update-form"),
	statusUpdateResult: document.querySelector("#status-update-result"),
	currentQueueList: document.querySelector("#current-queue-list"),
	toast: document.querySelector("#toast")
};

document.addEventListener("DOMContentLoaded", () => {
	document.body.classList.add("is-ready");
	bindStaffTabs();
	bindStaffCallForm();
	bindStatusUpdateForm();
	bindQuickStatusActions();
	startClock();
	loadDepartments();
	loadCurrentQueues();
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
			loadCurrentQueues();
		}
		catch (error) {
			elements.staffCallResult.hidden = false;
			elements.staffCallResult.innerHTML = `<p>${escapeHtml(error.message)}</p>`;
			showToast(error.message, "error");
		}
	});
}

function bindStatusUpdateForm() {
	elements.statusUpdateForm.addEventListener("submit", async (event) => {
		event.preventDefault();
		const payload = Object.fromEntries(new FormData(elements.statusUpdateForm).entries());
		await updateTicketStatus(payload.queueNumber.trim().toUpperCase(), payload.status, elements.statusUpdateResult);
	});
}

function bindQuickStatusActions() {
	elements.staffCallResult.addEventListener("click", async (event) => {
		const button = event.target.closest("[data-status-action]");
		if (!button) {
			return;
		}
		await updateTicketStatus(button.dataset.queueNumber, button.dataset.statusAction, elements.staffCallResult);
	});
}

async function updateTicketStatus(queueNumber, status, resultElement) {
	try {
		const ticket = await requestJson(API.updateStatus(queueNumber), {
			method: "PUT",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ status })
		});
		renderStatusUpdateResult(ticket, resultElement);
		showToast(`${ticket.queueNumber} updated to ${ticket.status}.`, "success");
		loadCurrentQueues();
	}
	catch (error) {
		resultElement.hidden = false;
		resultElement.innerHTML = `<p>${escapeHtml(error.message)}</p>`;
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
		elements.staffDepartmentSelect.innerHTML = [
			`<option value="">Select department</option>`,
			...departments.map((department) =>
				`<option value="${department.code}">${escapeHtml(department.name)} (${department.code})</option>`
			)
		].join("");
	}
	catch (error) {
		showToast(error.message, "error");
	}
}

async function loadCurrentQueues() {
	try {
		const queues = await requestJson(API.currentQueues);
		elements.currentQueueList.innerHTML = queues.map((queue, index) => `
			<div class="queue-row" style="--row-index: ${index}">
				<img class="dept-logo" src="${departmentLogo(queue.departmentCode)}" alt="${escapeHtml(queue.departmentName)} logo" loading="lazy">
				<div>
					<h3>${escapeHtml(queue.departmentName)}</h3>
					<p>${queue.waitingCount} waiting</p>
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

function renderStaffCallResult(ticket) {
	elements.staffCallResult.hidden = false;
	elements.staffCallResult.innerHTML = `
		${ticketSummary(ticket, "Now Calling")}
		<div class="ticket-actions" aria-label="Quick status updates">
			<button class="secondary-action" type="button" data-status-action="SERVING" data-queue-number="${escapeHtml(ticket.queueNumber)}">Start Serving</button>
			<button class="secondary-action" type="button" data-status-action="COMPLETED" data-queue-number="${escapeHtml(ticket.queueNumber)}">Mark Completed</button>
			<button class="danger-action" type="button" data-status-action="MISSED" data-queue-number="${escapeHtml(ticket.queueNumber)}">Mark Missed</button>
		</div>
	`;
}

function renderStatusUpdateResult(ticket, resultElement) {
	resultElement.hidden = false;
	resultElement.innerHTML = ticketSummary(ticket, "Ticket Updated");
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
			${detailItem("Counter", ticket.counterName || "-")}
			${detailItem("Called At", formatTime(ticket.calledAt))}
			${detailItem("Completed At", formatTime(ticket.completedAt))}
		</div>
	`;
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
