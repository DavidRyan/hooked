import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useState, useEffect } from 'react'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'

function App() {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem('token')
  )

  useEffect(() => {
    if (token) {
      localStorage.setItem('token', token)
    } else {
      localStorage.removeItem('token')
    }
  }, [token])

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            token ? <Navigate to="/" /> : <Login onLogin={setToken} />
          }
        />
        <Route
          path="/"
          element={
            token ? (
              <Dashboard token={token} onLogout={() => setToken(null)} />
            ) : (
              <Navigate to="/login" />
            )
          }
        />
      </Routes>
    </BrowserRouter>
  )
}

export default App
