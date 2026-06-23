<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<meta name="context-path" content="${pageContext.request.contextPath}">
	<link rel="icon" href="data:,">
	<title>Smart Hospital Queue</title>
	<link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css?v=20260623-7">
</head>
<body class="patient-page">
	<div class="topbar" aria-label="Hospital queue status">
		<span>Smart Queue · Public Hospital Malaysia</span>
		<span>Online numbers open daily from 6:00 AM</span>
	</div>

	<header class="app-header">
		<img class="brand-logo" src="${pageContext.request.contextPath}/assets/hospital-logo.png?v=20260530-3" alt="Public Hospital Malaysia logo">
		<div class="brand-copy">
			<p class="eyebrow">Hospital Awam Malaysia</p>
			<h1>Smart Queue</h1>
		</div>
		<div class="header-status">
			<span id="clock">--:--</span>
			<span class="status-dot"></span>
			<span>Customer Queue</span>
			<a class="portal-link" href="${pageContext.request.contextPath}/staff">Staff Portal</a>
		</div>
	</header>

	<main class="layout">
		<section class="queue-workspace" aria-labelledby="take-queue-heading">
			<div class="tabs" role="tablist" aria-label="Queue actions">
				<button class="tab-button active" type="button" data-tab-target="take-panel" role="tab" aria-selected="true">Take Queue</button>
				<button class="tab-button" type="button" data-tab-target="check-panel" role="tab" aria-selected="false">Check Ticket</button>
				<button class="tab-button" type="button" data-tab-target="current-panel" role="tab" aria-selected="false">Current Queue</button>
			</div>

			<section id="take-panel" class="tab-panel active" role="tabpanel">
				<div class="section-heading">
					<span class="step-badge">Online registration</span>
					<h2 id="take-queue-heading">Take Queue Number</h2>
					<p>Submit your identity, contact number, and department before coming to the hospital.</p>
				</div>

				<form id="queue-form" class="form-grid" novalidate>
					<div class="field-group">
						<label for="identity-type">Identity Type</label>
						<select name="identityType" id="identity-type" required>
							<option value="MALAYSIAN">Malaysian</option>
							<option value="NON_MALAYSIAN">Non-Malaysian</option>
						</select>
						<small class="field-error" data-error-for="identityType" aria-live="polite"></small>
					</div>
					<div class="field-group">
						<label id="identity-number-label" for="identity-number">Identity Number</label>
						<div class="masked-input">
							<span id="identity-mask" class="input-mask" aria-hidden="true"></span>
							<input name="identityNumber" id="identity-number" type="text" inputmode="numeric" autocomplete="off" placeholder="e.g. IC No." maxlength="14" required>
						</div>
						<small id="identity-number-hint" class="field-hint">12-digit MyKad number</small>
						<small class="field-error" data-error-for="identityNumber" aria-live="polite"></small>
					</div>
					<div class="field-group full">
						<label for="phone-number">Mobile Number</label>
						<div class="phone-input-group">
							<div id="malaysia-prefix" class="malaysia-prefix" aria-label="Malaysia phone prefix plus 60">
								<span class="country-code">MY</span>
								<strong>+60</strong>
								<input id="phone-country-fixed" name="phoneCountryCode" type="hidden" value="+60">
							</div>
							<div id="foreign-prefix" class="foreign-prefix" hidden>
								<span id="phone-flag" class="phone-flag" aria-hidden="true">🌐</span>
								<select name="phoneCountryCode" id="phone-country-code" required disabled aria-label="Phone country code">
									<c:forEach items="${phoneCodes}" var="phoneCode">
										<c:if test="${phoneCode.dialCode ne '+60'}">
											<option value="<c:out value='${phoneCode.dialCode}'/>" data-iso="<c:out value='${phoneCode.isoCode}'/>" title="<c:out value='${phoneCode.countryName}'/>"><c:out value="${phoneCode.dialCode}"/> — <c:out value="${phoneCode.countryName}"/></option>
										</c:if>
									</c:forEach>
								</select>
							</div>
							<input name="phoneNumber" id="phone-number" type="tel" inputmode="numeric" autocomplete="tel" placeholder="1123456789" maxlength="15" required aria-label="Mobile number">
						</div>
						<small id="phone-number-hint" class="field-hint">Enter the Malaysian number without the leading 0.</small>
						<small class="field-error" data-error-for="phoneNumber" aria-live="polite"></small>
						<small class="field-error" data-error-for="phoneCountryCode" aria-live="polite"></small>
					</div>
					<div class="field-group full">
						<label for="department-select">Department</label>
						<select name="departmentCode" id="department-select" required>
							<option value="">Select department</option>
							<c:forEach items="${departments}" var="department">
								<option value="<c:out value='${department.code}'/>"><c:out value="${department.name}"/> (<c:out value="${department.code}"/>)</option>
							</c:forEach>
						</select>
						<small class="field-error" data-error-for="departmentCode" aria-live="polite"></small>
					</div>
					<div id="department-preview" class="department-preview full" aria-live="polite">
						<div class="department-logo-frame empty">
							<span>Dept</span>
						</div>
						<div>
							<strong>Select a department</strong>
							<p>The matching clinic logo will appear here.</p>
						</div>
					</div>
					<div id="form-error" class="form-error full" role="alert" hidden></div>
					<button class="primary-action full submit-queue" type="submit"><span>Get Queue Number</span><span aria-hidden="true">→</span></button>
				</form>

				<article id="ticket-result" class="ticket-result" hidden aria-live="polite"></article>
			</section>

			<section id="check-panel" class="tab-panel" role="tabpanel">
				<div class="section-heading">
					<h2>Check My Ticket</h2>
					<p>Enter your queue number to view status and people ahead.</p>
				</div>

				<form id="check-form" class="search-row">
					<input name="queueNumber" type="text" placeholder="GEN001" autocomplete="off" required>
					<button class="secondary-action" type="submit">Check</button>
				</form>

				<article id="check-result" class="ticket-detail" hidden aria-live="polite"></article>
			</section>

			<section id="current-panel" class="tab-panel" role="tabpanel">
				<div class="section-heading">
					<h2>Current Queue</h2>
					<p>See the current serving number and waiting count by department.</p>
				</div>
				<div id="current-queue-list" class="queue-list" aria-live="polite"></div>
			</section>

		</section>

		<aside class="side-panel" aria-label="Queue information">
			<section class="info-block">
				<h2>Before You Go</h2>
				<ol>
					<li>Take a number online after 6:00 AM.</li>
					<li>Check your queue before travelling.</li>
					<li>Arrive when your number is close.</li>
				</ol>
			</section>
			<section class="info-block">
				<h2>Daily Rules</h2>
				<ul id="quota-summary">
					<c:forEach items="${departments}" var="department">
						<li><c:out value="${department.name}"/>: <c:out value="${department.dailyQuota}"/> slots daily</li>
					</c:forEach>
				</ul>
			</section>
		</aside>
	</main>

	<div id="toast" class="toast" role="status" aria-live="polite" hidden></div>
	<script src="${pageContext.request.contextPath}/app.js?v=20260623-11" defer></script>
</body>
</html>
