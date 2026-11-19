import React, { createContext, useContext, useState, useEffect } from 'react';
import { api, getAuthToken, setAuthToken, removeAuthToken } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authModalTab, setAuthModalTab] = useState('login'); // 'login' | 'register'

  useEffect(() => {
    const token = getAuthToken();
    if (token) {
      api.getMe()
        .then(userData => {
          setUser(userData);
        })
        .catch(() => {
          removeAuthToken();
          setUser(null);
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  const login = async (email, password) => {
    const res = await api.login({ email, password });
    setAuthToken(res.token);
    setUser(res.user);
    setIsAuthModalOpen(false);
    return res.user;
  };

  const register = async (fullName, email, password, phoneNumber) => {
    const res = await api.register({ fullName, email, password, phoneNumber });
    setAuthToken(res.token);
    setUser(res.user);
    setIsAuthModalOpen(false);
    return res.user;
  };

  // The server revokes the token so it cannot be reused; the browser forgets it either way.
  const logout = async () => {
    try {
      await api.logout();
    } catch {
      // Signing out must never fail for the user: an expired or unreachable session is already signed out.
    }
    removeAuthToken();
    setUser(null);
  };

  const refreshUser = async () => {
    try {
      const updated = await api.getMe();
      setUser(updated);
    } catch (err) {
      console.error('Failed to refresh user profile', err);
    }
  };

  const openAuthModal = (tab = 'login') => {
    setAuthModalTab(tab);
    setIsAuthModalOpen(true);
  };

  const closeAuthModal = () => {
    setIsAuthModalOpen(false);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        isAuthenticated: !!user,
        isAdmin: user?.role === 'ROLE_ADMIN',
        login,
        register,
        logout,
        refreshUser,
        isAuthModalOpen,
        authModalTab,
        openAuthModal,
        closeAuthModal,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
