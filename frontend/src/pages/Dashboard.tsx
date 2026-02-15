import { useState, useEffect, useCallback } from 'react'
import './Dashboard.css'

interface Catch {
  id: string
  species: string | null
  location: string
  latitude: number
  longitude: number
  caught_at: string | null
  notes: string | null
  image_url: string | null
  weather_data: Record<string, unknown> | null
  inserted_at: string
}

interface DashboardProps {
  token: string
  onLogout: () => void
}

export default function Dashboard({ token, onLogout }: DashboardProps) {
  const [catches, setCatches] = useState<Catch[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const fetchCatches = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await fetch('/api/user_catches', {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.status === 401) {
        onLogout()
        return
      }
      if (!res.ok) throw new Error('Failed to load catches')
      const data = await res.json()
      setCatches(data.user_catches ?? [])
    } catch {
      setError('Could not load catches')
    } finally {
      setLoading(false)
    }
  }, [token, onLogout])

  useEffect(() => {
    fetchCatches()
  }, [fetchCatches])

  function formatDate(dateStr: string | null): string {
    if (!dateStr) return ''
    const d = new Date(dateStr)
    return d.toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    })
  }

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <span className="dashboard-title">Hooked</span>
        <button onClick={onLogout} className="dashboard-logout">
          Sign Out
        </button>
      </header>

      <main className="catches-container">
        <div className="catches-toolbar">
          <h2 className="catches-heading">My Catches</h2>
          <button onClick={fetchCatches} className="catches-refresh" disabled={loading}>
            Refresh
          </button>
        </div>

        {loading && catches.length === 0 && (
          <div className="catches-grid">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="catch-card catch-skeleton">
                <div className="catch-skeleton-image" />
                <div className="catch-skeleton-text" />
                <div className="catch-skeleton-text short" />
              </div>
            ))}
          </div>
        )}

        {error && (
          <div className="catches-error">
            {error}
            <button onClick={fetchCatches} className="catches-retry">
              Retry
            </button>
          </div>
        )}

        {!loading && !error && catches.length === 0 && (
          <div className="catches-empty">
            <p className="catches-empty-title">No Catches Yet</p>
            <p className="catches-empty-sub">
              Your catches from the mobile app will appear here.
            </p>
          </div>
        )}

        {catches.length > 0 && (
          <div className="catches-grid">
            {catches.map((c) => (
              <div key={c.id} className="catch-card">
                <div className="catch-image-wrap">
                  {c.image_url ? (
                    <img src={c.image_url} alt={c.species ?? 'Catch'} className="catch-image" />
                  ) : (
                    <div className="catch-no-image">No Photo</div>
                  )}
                </div>
                <div className="catch-info">
                  {c.species && <span className="catch-species">{c.species}</span>}
                  <span className="catch-location">{c.location}</span>
                  {c.caught_at && (
                    <span className="catch-date">{formatDate(c.caught_at)}</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
