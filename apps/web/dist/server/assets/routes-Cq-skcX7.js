import { t as currentUserQueryOptions } from "./current-user-NQqkzHK2.js";
import { jsx, jsxs } from "react/jsx-runtime";
import { useSuspenseQuery } from "@tanstack/react-query";
//#region src/features/user-session/user-session-shell.tsx
function UserSessionShell({ state }) {
	return /* @__PURE__ */ jsxs("main", {
		style: {
			fontFamily: "sans-serif",
			margin: "0 auto",
			maxWidth: 720,
			padding: "4rem 1.5rem"
		},
		children: [
			/* @__PURE__ */ jsx("p", {
				style: {
					letterSpacing: "0.08em",
					textTransform: "uppercase"
				},
				children: "Prompt Vault"
			}),
			/* @__PURE__ */ jsx("h1", { children: "Auth slice tracer" }),
			/* @__PURE__ */ jsx("p", { children: "This shell exercises the current-user contract from the TanStack Start app through the Spring Boot API." }),
			/* @__PURE__ */ jsxs("section", { children: [
				/* @__PURE__ */ jsx("strong", { children: "User status:" }),
				" ",
				state === "signed-in" ? "Authenticated session detected" : "No authenticated session"
			] })
		]
	});
}
//#endregion
//#region src/routes/index.tsx?tsr-split=component
function HomePage() {
	const { data: userSession } = useSuspenseQuery(currentUserQueryOptions());
	return /* @__PURE__ */ jsx(UserSessionShell, { state: userSession });
}
//#endregion
export { HomePage as component };
