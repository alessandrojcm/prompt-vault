export type CurrentUserState =
  | { authenticated: false }
  | { authenticated: true }

export async function getCurrentUser(fetchImpl: typeof fetch = fetch): Promise<CurrentUserState> {
  const response = await fetchImpl('/api/user', {
    credentials: 'include',
    headers: {
      Accept: 'application/json',
    },
  })

  if (response.status === 401) {
    return { authenticated: false }
  }

  if (response.status === 204) {
    return { authenticated: true }
  }

  throw new Error(`Unexpected current user response: ${response.status}`)
}
