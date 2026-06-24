document.addEventListener("DOMContentLoaded", () => {
	document.body.classList.add("is-ready");
	const button = document.querySelector("[data-toggle-password]");
	const password = document.querySelector("#staff-password");
	button?.addEventListener("click", () => {
		const showing = password.type === "text";
		password.type = showing ? "password" : "text";
		button.textContent = showing ? "Show" : "Hide";
		button.setAttribute("aria-label", showing ? "Show password" : "Hide password");
		button.setAttribute("aria-pressed", String(!showing));
	});
});
