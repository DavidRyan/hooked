import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './features/auth/useAuth'
import Dashboard from './pages/Dashboard'
import Login from './pages/Login'
import NotFound from './pages/NotFound'

function App() {
  const { token, isAuthenticated, login, logout } = useAuth()

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            isAuthenticated ? <Navigate to="/" /> : <Login onLogin={login} />
          }
        />
        <Route
          path="/"
          element={
            token ? (
              <Dashboard token={token} onLogout={logout} />
            ) : (
              <Navigate to="/login" />
            )
          }
        />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
