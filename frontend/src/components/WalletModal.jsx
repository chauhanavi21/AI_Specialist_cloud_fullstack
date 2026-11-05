import React, { useState, useEffect } from 'react';
import Modal, { ModalHeader } from './Modal';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { inr, inr2 } from '../lib/format';

const QUICK_AMOUNTS = [500, 1000, 2000];

export default function WalletModal({ isOpen, onClose }) {
  const { user, refreshUser } = useAuth();
  const toast = useToast();
  const [wallet, setWallet] = useState(null);
  const [loading, setLoading] = useState(true);
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!isOpen || !user) return;
    setLoading(true);
    setError('');
    setAmount('');
    api.getWallet()
      .then(setWallet)
      .catch((err) => setError(err.message || 'Could not load your wallet'))
      .finally(() => setLoading(false));
  }, [isOpen, user?.id]);

  const topUp = async (e) => {
    e.preventDefault();
    const value = Number(amount);
    if (!value || value < 1) return setError('Enter an amount of at least ₹1');
    if (value > 50000) return setError('The most you can add at once is ₹50,000');
    setSaving(true);
    setError('');
    try {
      setWallet(await api.topUpWallet(value));
      await refreshUser();
      setAmount('');
      toast.success(`${inr(value)} added to your wallet`);
    } catch (err) {
      setError(err.message || 'Could not add money');
    } finally {
      setSaving(false);
    }
    return undefined;
  };

  return (
    <Modal open={isOpen} onClose={onClose} title="Wallet" size="md">
      <ModalHeader title="Wallet" subtitle="Pay for orders from your balance. Cancelled orders are refunded here." onClose={onClose} />

      <div className="overflow-y-auto p-5 sm:p-6 space-y-5">
        <div className="bg-canvas border border-line p-4">
          <span className="text-xs text-fg3">Balance</span>
          <div className="text-3xl font-semibold text-fg">{inr2(wallet?.balance ?? user?.walletBalance ?? 0)}</div>
        </div>

        <form onSubmit={topUp} noValidate className="space-y-2">
          <label htmlFor="topup" className="field-label">Add money (demo: no real payment is taken)</label>
          <div className="flex">
            <input
              id="topup"
              inputMode="numeric"
              value={amount}
              onChange={(e) => { setAmount(e.target.value.replace(/\D/g, '')); setError(''); }}
              placeholder="Amount in ₹"
              aria-invalid={!!error}
              aria-describedby={error ? 'topup-err' : undefined}
              className="field flex-1 min-w-0"
            />
            <button type="submit" disabled={saving} className="btn-primary">{saving ? 'Adding...' : 'Add'}</button>
          </div>
          <div className="flex gap-2">
            {QUICK_AMOUNTS.map((n) => (
              <button key={n} type="button" onClick={() => { setAmount(String(n)); setError(''); }} className="btn-secondary min-h-[2.25rem]">
                +{inr(n)}
              </button>
            ))}
          </div>
          {error && <p id="topup-err" className="field-error" role="alert">{error}</p>}
        </form>

        <div>
          <h3 className="text-sm font-semibold mb-2">Transactions</h3>
          {loading ? (
            <p className="text-sm text-fg3 text-center py-6" role="status">Loading...</p>
          ) : !wallet?.transactions?.length ? (
            <p className="text-sm text-fg3 text-center py-6">No transactions yet.</p>
          ) : (
            <ul className="max-h-60 overflow-y-auto divide-y divide-line">
              {wallet.transactions.map((t) => (
                <li key={t.id} className="flex items-center justify-between py-2.5 text-sm gap-3">
                  <div className="min-w-0">
                    <span className="text-fg block truncate">{t.description}</span>
                    <span className="text-xs text-fg3">{new Date(t.createdAt).toLocaleDateString('en-IN')}</span>
                  </div>
                  <span className={`shrink-0 font-medium ${t.amount < 0 ? 'text-fg3' : 'text-ok'}`}>
                    {t.amount < 0 ? '−' : '+'}{inr2(Math.abs(t.amount))}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </Modal>
  );
}
