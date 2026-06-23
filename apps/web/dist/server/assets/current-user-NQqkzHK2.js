import { queryOptions } from "@tanstack/react-query";
//#region ../../packages/api-client/src/index.ts
async function getCurrentUser(fetchImpl = fetch) {
	const response = await fetchImpl("/api/user", {
		credentials: "include",
		headers: { Accept: "application/json" }
	});
	if (response.status === 401) return { authenticated: false };
	if (response.status === 204) return { authenticated: true };
	throw new Error(`Unexpected current user response: ${response.status}`);
}
//#endregion
//#region src/lib/query-keys.ts
var QueryKeys = { currentUser: () => ["current-user"] };
//#endregion
//#region src/lib/current-user.ts
function toUserSessionViewState(currentUser) {
	return currentUser.authenticated ? "signed-in" : "signed-out";
}
var currentUserQueryOptions = () => queryOptions({
	queryKey: QueryKeys.currentUser(),
	queryFn: async () => toUserSessionViewState(await getCurrentUser())
});
//#endregion
export { currentUserQueryOptions as t };
