document.addEventListener("DOMContentLoaded", () => {
	document.body.classList.add("is-ready");
	const button = document.querySelector("[data-toggle-password]");
	const password = document.querySelector("#staff-password");
	const toast = document.querySelector("[data-logout-toast='true']");
	button?.addEventListener("click", () => {
		const showing = password.type === "text";
		password.type = showing ? "password" : "text";
		button.textContent = showing ? "Show" : "Hide";
		button.setAttribute("aria-label", showing ? "Show password" : "Hide password");
		button.setAttribute("aria-pressed", String(!showing));
	});
	if (toast?.dataset.logoutToast === "true" && toast.dataset.message) {
		toast.textContent = toast.dataset.message;
		toast.hidden = false;
		window.setTimeout(() => {
			toast.hidden = true;
		}, 3200);
	}
});
