<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<meta name="context-path" content="${pageContext.request.contextPath}">
	<link rel="icon" href="data:,">
	<title>Smart Hospital Queue · Admin Login</title>
	<link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css?v=20260624-1">
</head>
<body class="staff-login-page">
	<main class="staff-login-shell">
		<section class="staff-login-brand" aria-labelledby="login-brand-heading">
			<div class="login-brand-copy">
				<img src="${pageContext.request.contextPath}/assets/hospital-logo.png?v=20260530-3" alt="Public Hospital Malaysia logo">
				<p class="eyebrow">Hospital Awam Malaysia</p>
				<h1 id="login-brand-heading">Admin Queue Portal</h1>
				<p>Secure access for authorised hospital personnel managing patient queues and service counters.</p>
			</div>
			<div class="login-security-note">
				<span aria-hidden="true">✓</span>
				<p><strong>Authorised staff only</strong><br>Your session remains active until you sign out or close the browser.</p>
			</div>
		</section>

		<section class="staff-login-panel" aria-labelledby="staff-login-heading">
			<div class="staff-login-card">
				<p class="step-badge">Admin authentication</p>
				<h2 id="staff-login-heading">Welcome back</h2>
				<p class="login-intro">Enter your staff credentials to continue.</p>

				<c:if test="${param.logout != null}">
					<div class="login-alert success" role="status">You have signed out successfully.</div>
				</c:if>
				<c:if test="${not empty loginError}">
					<div class="login-alert error" role="alert"><c:out value="${loginError}"/></div>
				</c:if>

				<form class="staff-login-form" action="${pageContext.request.contextPath}/admin/login" method="post">
					<label for="staff-username">Admin ID</label>
					<input id="staff-username" name="username" type="text" value="<c:out value='${username}'/>"
						autocomplete="username" placeholder="Enter your admin ID" required autofocus>

					<label for="staff-password">Password</label>
					<div class="password-field">
						<input id="staff-password" name="password" type="password" autocomplete="current-password"
							placeholder="Enter your password" required>
						<button type="button" data-toggle-password aria-label="Show password" aria-pressed="false">Show</button>
					</div>

					<button class="primary-action login-submit" type="submit">Sign In</button>
				</form>
				<p class="login-help">Having trouble signing in? Contact the system administrator.</p>
			</div>
		</section>
	</main>
	<script src="${pageContext.request.contextPath}/staff-login.js?v=20260624-1" defer></script>
</body>
</html>
