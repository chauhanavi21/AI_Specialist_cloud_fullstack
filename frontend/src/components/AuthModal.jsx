import React, { useState, useEffect } from 'react';
import Modal, { ModalHeader } from './Modal';
import { useAuth } from '../context/AuthContext';

export default function AuthModal() {
  const { isAuthModalOpen, authModalTab, closeAuthModal, login, register, openAuthModal } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [errors, setErrors] = useState({});
  const [errorMsg, setErrorMsg] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isAuthModalOpen) {
      setErrors({});
      setErrorMsg('');
    }
  }, [isAuthModalOpen, authModalTab]);

  const isLogin = authModalTab === 'login';

  const validate = () => {
    const found = {};
    if (!isLogin && fullName.trim().length < 2) found.fullName = 'Enter your full name';
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) found.email = 'Enter a valid email address';
    if (!password) found.password = 'Enter your password';
    else if (!isLogin && password.length < 6) found.password = 'Use at least 6 characters';
    return found;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const found = validate();
    setErrors(found);
    setErrorMsg('');
    if (Object.keys(found).length > 0) {
      document.getElementById(`auth-${Object.keys(found)[0]}`)?.focus();
      return;
    }
    setLoading(true);
    try {
      if (isLogin) await login(email.trim(), password);
      else await register(fullName.trim(), email.trim(), password, phoneNumber.trim());
    } catch (err) {
      setErrorMsg(err.message || 'Authentication failed. Please verify your details.');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickDemo = async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      await login('alex.dev@ibm.com', 'Password123!');
    } catch (err) {
      setErrorMsg(err.message);
    } finally {
      setLoading(false);
    }
  };

  const tab = (active) =>
    `flex-1 py-2.5 text-sm ${active ? 'bg-layer text-fg font-medium border-b-2 border-brand' : 'text-fg3 hover:text-fg'}`;

  const field = (id, label, value, setValue, props = {}) => (
    <div>
      <label htmlFor={`auth-${id}`} className="field-label">{label}</label>
      <input
        id={`auth-${id}`}
        value={value}
        onChange={(e) => { setValue(e.target.value); if (errors[id]) setErrors({ ...errors, [id]: undefined }); }}
        aria-invalid={!!errors[id]}
        aria-describedby={errors[id] ? `auth-${id}-err` : undefined}
        className="field"
        {...props}
      />
      {errors[id] && <p id={`auth-${id}-err`} className="field-error" role="alert">{errors[id]}</p>}
    </div>
  );

  return (
    <Modal open={isAuthModalOpen} onClose={closeAuthModal} title={isLogin ? 'Sign in' : 'Create account'} size="sm">
      <ModalHeader
        title={isLogin ? 'Sign in' : 'Create your account'}
        subtitle={isLogin ? undefined : 'New accounts start with 150 gift points.'}
        onClose={closeAuthModal}
      />

      <div className="overflow-y-auto p-5 sm:p-6 space-y-4">
        <div className="flex bg-canvas" role="tablist">
          <button role="tab" aria-selected={isLogin} onClick={() => openAuthModal('login')} className={tab(isLogin)}>Sign in</button>
          <button role="tab" aria-selected={!isLogin} onClick={() => openAuthModal('register')} className={tab(!isLogin)}>Create account</button>
        </div>

        {errorMsg && (
          <p className="text-sm text-danger border-l-2 border-danger pl-3" role="alert">{errorMsg}</p>
        )}

        <form onSubmit={handleSubmit} noValidate className="space-y-3">
          {!isLogin && field('fullName', 'Full name', fullName, setFullName, { autoComplete: 'name' })}
          {field('email', 'Email', email, setEmail, { type: 'email', autoComplete: 'email' })}
          {field('password', 'Password', password, setPassword, { type: 'password', autoComplete: isLogin ? 'current-password' : 'new-password' })}
          {!isLogin && field('phoneNumber', 'Phone (optional)', phoneNumber, setPhoneNumber, { type: 'tel', autoComplete: 'tel' })}

          <button type="submit" disabled={loading} className="btn-primary w-full mt-1">
            {loading ? 'Please wait...' : isLogin ? 'Sign in' : 'Create account'}
          </button>
        </form>

        {isLogin && (
          <div className="pt-3 border-t border-line text-center">
            <button type="button" onClick={handleQuickDemo} disabled={loading} className="text-sm text-link hover:underline">
              Use demo account
            </button>
          </div>
        )}
      </div>
    </Modal>
  );
}
