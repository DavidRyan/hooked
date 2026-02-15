import type { ApiErrorPayload } from '../types/api'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function parseJsonSafely<T>(res: Response): Promise<T | null> {
  try {
    return (await res.json()) as T
  } catch {
    return null
  }
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, init)
  const payload = await parseJsonSafely<T & ApiErrorPayload>(res)

  if (!res.ok) {
    const message = payload?.message || 'Request failed'
    throw new ApiError(res.status, message)
  }

  if (payload === null) {
    throw new ApiError(res.status, 'Invalid response')
  }

  return payload as T
}
