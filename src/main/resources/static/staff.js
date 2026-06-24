const CONTEXT_PATH = document.querySelector('meta[name="context-path"]')?.content || "";

const API = {
    departments: `${CONTEXT_PATH}/api/departments`,
    counters: `${CONTEXT_PATH}/api/counters`,
    currentQueues: `${CONTEXT_PATH}/api/queues/current`,
    currentServices: `${CONTEXT_PATH}/api/queues/activeServices`,
    queues: `${CONTEXT_PATH}/api/queueTickets`,
    ticket: (queueNumber) => `${CONTEXT_PATH}/api/queueTickets/${encodeURIComponent(queueNumber)}`,
    nextCall: `${CONTEXT_PATH}/api/queueCalls`,
    callTicket: (queueNumber) => `${CONTEXT_PATH}/api/queueTickets/${encodeURIComponent(queueNumber)}/call`,
    updateStatus: (queueNumber) => `${CONTEXT_PATH}/api/queueTickets/${encodeURIComponent(queueNumber)}/status`
};

let counterData = [];
let activeServices = [];
let departmentData = [];
let currentQueueData = [];
let lastDepartmentCallMarkup = "";

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
    departmentCallGrid: document.querySelector("#department-call-grid"),
    staffCallResult: document.querySelector("#staff-call-result"),
    statusLookupForm: document.querySelector("#status-lookup-form"),
    statusUpdateResult: document.querySelector("#status-update-result"),
    statusActionPanel: document.querySelector("#status-action-panel"),
    queueListForm: document.querySelector("#queue-list-form"),
    staffTicketList: document.querySelector("#staff-ticket-list"),
    currentQueueList: document.querySelector("#current-queue-list"),
    toast: document.querySelector("#toast")
};

document.addEventListener("DOMContentLoaded", () => {
    document.body.classList.add("is-ready");
    bindStaffTabs();
    bindDepartmentCallCards();
    bindStatusLookupForm();
    bindStatusWorkflowActions();
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

function bindDepartmentCallCards() {
    elements.departmentCallGrid.addEventListener("click", async (event) => {
        const button = event.target.closest("[data-call-next]");
        if (!button || button.disabled) return;

        button.disabled = true;
        button.textContent = "Calling…";
        try {
            const ticket = await requestJson(API.nextCall, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    departmentCode: button.dataset.departmentCode,
                    counterName: button.dataset.counterName
                })
            });
            renderStaffCallResult(ticket);
            showToast(`Now calling ${ticket.queueNumber} at ${ticket.counterName}.`, "success");
            await refreshStaffDashboard();
            loadStaffTicketList();
        }
        catch (error) {
            showToast(error.message, "error");
            lastDepartmentCallMarkup = "";
            renderDepartmentCallCards();
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
            await callTicket(queueNumber, callButton.dataset.counterName);
        }
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
        departmentData = departments;
        const departmentOptions = [
            `<option value="">Select department</option>`,
            ...departments.map((department) =>
                `<option value="${department.code}">${escapeHtml(department.name)} (${department.code})</option>`
            )
        ].join("");
        document.querySelectorAll(".department-select").forEach((select) => {
            select.innerHTML = departmentOptions;
        });
        renderDepartmentCallCards();
    }
    catch (error) {
        showToast(error.message, "error");
    }
}

async function loadCounters() {
    try {
        counterData = await requestJson(API.counters);
        renderDepartmentCallCards();
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
    const [queuesResult, servicesResult] = await Promise.allSettled([
        requestJson(API.currentQueues),
        requestJson(API.currentServices)
    ]);

    if (queuesResult.status === "fulfilled") {
        currentQueueData = queuesResult.value;
        elements.currentQueueList.innerHTML = currentQueueData.map((queue, index) => `
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
    else if (!currentQueueData.length) {
        elements.currentQueueList.innerHTML = `<p>${escapeHtml(queuesResult.reason.message)}</p>`;
    }

    if (servicesResult.status === "fulfilled") activeServices = servicesResult.value;
    renderDepartmentCallCards();
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
    elements.staffCallResult.dataset.jumpToStatus = ticket.queueNumber;
    elements.staffCallResult.title = `Open ${ticket.queueNumber} status`;
    elements.staffCallResult.innerHTML = `
        <img src="${departmentLogo(ticket.departmentCode)}" alt="">
        <div>
            <span>Now calling</span>
            <strong>${escapeHtml(ticket.queueNumber)}</strong>
            <p>${escapeHtml(ticket.departmentName)} · ${escapeHtml(ticket.counterName)}</p>
        </div>
        <span class="status-pill">${escapeHtml(ticket.status)}</span>
    `;
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

function renderDepartmentCallCards() {
    if (!departmentData.length || !counterData.length) return;

    const servicesByCounter = new Map(activeServices
        .filter((service) => service.counterName)
        .map((service) => [normalizeCounterName(service.counterName), service]));
    const queuesByDepartment = new Map(currentQueueData
        .map((queue) => [queue.departmentCode, queue]));

    const markup = departmentData.map((department, index) => {
        const queue = queuesByDepartment.get(department.code);
        const waitingCount = Number(queue?.waitingCount || 0);
        const counters = counterData.filter((counter) => counter.departmentCode === department.code);
        const counterRows = counters.length ? counters.map((counter) => {
            const service = servicesByCounter.get(normalizeCounterName(counter.name));
            const available = counter.status === "OPEN" && !service;
            const status = service ? "BUSY" : available ? "AVAILABLE" : counter.status;
            const disabled = !available || waitingCount === 0;
            const buttonLabel = waitingCount === 0 ? "No patients waiting"
                : available ? `Call next at ${counter.name}`
                : service ? `${service.queueNumber} in service`
                : "Counter unavailable";
            return `
                <div class="department-counter-row ${service ? "is-busy" : available ? "is-available" : "is-closed"}"
                    ${service ? `data-jump-to-status="${escapeHtml(service.queueNumber)}" title="Open ${escapeHtml(service.queueNumber)}"` : ""}>
                    <div class="department-counter-state">
                        <strong>${escapeHtml(counter.name)}</strong>
                        <span class="counter-state-pill ${status.toLowerCase()}">${escapeHtml(status)}</span>
                        ${service ? `<small>${escapeHtml(service.queueNumber)} · ${escapeHtml(service.status)}</small>` : `<small>${available ? "Ready for the next patient" : "Not accepting patients"}</small>`}
                    </div>
                    <button class="call-counter-action" type="button" data-call-next
                        data-department-code="${escapeHtml(department.code)}"
                        data-counter-name="${escapeHtml(counter.name)}" ${disabled ? "disabled" : ""}>${escapeHtml(buttonLabel)}</button>
                </div>
            `;
        }).join("") : `<p class="empty-state compact">No counters configured for this department.</p>`;

        return `
            <article class="department-call-card" style="--row-index: ${index}">
                <header class="department-call-header">
                    <img src="${departmentLogo(department.code)}" alt="" loading="lazy">
                    <div>
                        <span class="department-code">${escapeHtml(department.code)}</span>
                        <h3>${escapeHtml(department.name)}</h3>
                    </div>
                    <div class="department-waiting-count">
                        <strong>${waitingCount}</strong>
                        <span>waiting</span>
                    </div>
                </header>
                <div class="department-usage">${Number(queue?.usedSlots || 0)} of ${Number(queue?.dailyQuota || department.dailyQuota || 0)} daily slots used</div>
                <div class="department-counter-list">${counterRows}</div>
            </article>
        `;
    }).join("");
    if (markup === lastDepartmentCallMarkup) return;
    lastDepartmentCallMarkup = markup;
    elements.departmentCallGrid.innerHTML = markup;
}

function renderCallTicketControls(departmentCode, queueNumber) {
    const busyCounters = new Set(activeServices.map((service) => normalizeCounterName(service.counterName)).filter(Boolean));
    const availableCounters = counterData.filter((counter) => counter.departmentCode === departmentCode
        && counter.status === "OPEN" && !busyCounters.has(normalizeCounterName(counter.name)));
    if (!availableCounters.length) {
        return `<p class="muted-copy">No counters are currently available for this department.</p>`;
    }
    return `
        <div class="ticket-call-panel">
            ${availableCounters.map((counter) => `
                <button class="secondary-action" type="button" data-call-ticket
                    data-counter-name="${escapeHtml(counter.name)}"
                    data-queue-number="${escapeHtml(queueNumber)}">Call at ${escapeHtml(counter.name)}</button>
            `).join("")}
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
