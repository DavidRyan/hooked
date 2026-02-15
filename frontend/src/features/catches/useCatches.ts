import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from '../../services/api'
import { getUserCatches } from '../../services/catches'
import type { Catch } from './types'

type UseCatchesState = {
  catches: Catch[]
  loading: boolean
  error: string
  refresh: () => void
}

export function useCatches(token: string, onUnauthorized: () => void): UseCatchesState {
  const [catches, setCatches] = useState<Catch[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const abortRef = useRef<AbortController | null>(null)

  const load = useCallback(async () => {
    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller

    setLoading(true)
    setError('')

    try {
      const data = await getUserCatches(token, controller.signal)
      setCatches(data)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }

      if (err instanceof DOMException && err.name === 'AbortError') {
        return
      }

      setError('Could not load catches')
    } finally {
      setLoading(false)
    }
  }, [onUnauthorized, token])

  useEffect(() => {
    load()
    return () => abortRef.current?.abort()
  }, [load])

  return { catches, loading, error, refresh: load }
}
