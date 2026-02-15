import './NotFound.css'

export default function NotFound() {
  return (
    <div className="not-found">
      <div className="not-found-card">
        <p className="not-found-kicker">404</p>
        <h1 className="not-found-title">Page not found</h1>
        <p className="not-found-sub">
          The page you are looking for does not exist.
        </p>
        <a className="not-found-link" href="/">
          Go to dashboard
        </a>
      </div>
    </div>
  )
}
