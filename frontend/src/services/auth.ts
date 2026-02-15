import { apiFetch } from './api'
import type { LoginResponse } from '../types/api'

export async function login(email: string, password: string): Promise<string> {
  const data = await apiFetch<LoginResponse>('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })

  return data.data.token
}
