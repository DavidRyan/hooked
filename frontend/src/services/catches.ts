import { apiFetch } from './api'
import type { CatchesResponse } from '../types/api'
import type { Catch } from '../features/catches/types'

export async function getUserCatches(
  token: string,
  signal?: AbortSignal,
): Promise<Catch[]> {
  const data = await apiFetch<CatchesResponse<Catch>>('/api/user_catches', {
    headers: { Authorization: `Bearer ${token}` },
    signal,
  })

  return data.user_catches ?? []
}
