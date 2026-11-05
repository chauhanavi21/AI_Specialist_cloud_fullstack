import React, { useState } from 'react';
import { X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const inputClass =
  'w-full px-3 py-2 text-base sm:text-sm bg-white border border-brand-200 rounded-md focus:border-brand-500 focus:outline-none';

export default function AuthModal() {
  const { isAuthModalOpen, authModalTab, closeAuthModal, login, register, openAuthModal } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [loading, setLoading] = useState(false);

  if (!isAuthModalOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setLoading(true);

    try {
      if (authModalTab === 'login') {
        await login(email, password);
      } else {
        await register(fullName, email, password, phoneNumber);
      }
    } catch (err) {
      setErrorMsg(err.message || 'Authentication failed. Please verify credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickDemo = async () => {
    setEmail('alex.dev@ibm.com');
    setPassword('Password123!');
    setLoading(true);
    try {
      await login('alex.dev@ibm.com', 'Password123!');
    } catch (err) {
      setErrorMsg(err.message);
    } finally {
      setLoading(false);
    }
  };

  const tabClass = (active) =>
    `flex-1 py-2 text-sm rounded-md ${
      active ? 'bg-white text-slate-900 font-medium' : 'text-slate-500 hover:text-slate-800'
    }`;

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/40 flex items-end sm:items-center justify-center p-0 sm:p-4 overflow-y-auto">
      <div className="relative bg-white rounded-t-xl sm:rounded-lg max-w-sm sm:max-w-sm w-full max-h-[95dvh] overflow-y-auto p-4 sm:p-6 space-y-5">
        <button
          onClick={closeAuthModal}
          className="absolute top-3 right-3 p-2 text-slate-400 hover:text-slate-700 rounded-md hover:bg-brand-50"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex bg-brand-100 p-1 rounded-md mr-8">
          <button
            onClick={() => { openAuthModal('login'); setErrorMsg(''); }}
            className={tabClass(authModalTab === 'login')}
          >
            Sign in
          </button>
          <button
            onClick={() => { openAuthModal('register'); setErrorMsg(''); }}
            className={tabClass(authModalTab === 'register')}
          >
            Create account
          </button>
        </div>

        <div>
          <h2 className="text-xl font-semibold text-slate-900">
            {authModalTab === 'login' ? 'Sign in' : 'Create your account'}
          </h2>
          {authModalTab === 'register' && (
            <p className="text-sm text-slate-500 mt-1">New accounts start with 150 gift points.</p>
          )}
        </div>

        {errorMsg && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-md">
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-3">
          {authModalTab === 'register' && (
            <div>
              <label className="block text-sm text-slate-700 mb-1">Full name</label>
              <input
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className={inputClass}
              />
            </div>
          )}

          <div>
            <label className="block text-sm text-slate-700 mb-1">Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className={inputClass}
            />
          </div>

          <div>
            <label className="block text-sm text-slate-700 mb-1">Password</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={inputClass}
            />
          </div>

          {authModalTab === 'register' && (
            <div>
              <label className="block text-sm text-slate-700 mb-1">Phone (optional)</label>
              <input
                type="tel"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                className={inputClass}
              />
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 bg-brand-600 hover:bg-brand-700 disabled:opacity-60 text-white text-sm font-medium rounded-md mt-1"
          >
            {loading ? 'Please wait...' : authModalTab === 'login' ? 'Sign in' : 'Create account'}
          </button>
        </form>

        {authModalTab === 'login' && (
          <div className="pt-4 border-t border-brand-100 text-center">
            <button
              type="button"
              onClick={handleQuickDemo}
              disabled={loading}
              className="text-sm text-brand-700 hover:underline"
            >
              Use demo account
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
