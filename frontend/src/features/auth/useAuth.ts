import { useEffect, useMemo, useState } from 'react'
import { clearStoredToken, getStoredToken, setStoredToken } from './storage'

type AuthState = {
  token: string | null
  isAuthenticated: boolean
  login: (token: string) => void
  logout: () => void
}

export function useAuth(): AuthState {
  const [token, setToken] = useState<string | null>(() => getStoredToken())

  useEffect(() => {
    if (token) {
      setStoredToken(token)
    } else {
      clearStoredToken()
    }
  }, [token])

  const value = useMemo(
    () => ({
      token,
      isAuthenticated: Boolean(token),
      login: setToken,
      logout: () => setToken(null),
    }),
    [token],
  )

  return value
}
