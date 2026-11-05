import React, { useState, useEffect } from 'react';
import Modal, { ModalHeader } from './Modal';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { inr } from '../lib/format';

const NEGATIVE = new Set(['REDEEMED', 'REVOKED']);

export default function GiftPointsModal({ isOpen, onClose }) {
  const { user } = useAuth();
  const [pointsData, setPointsData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isOpen || !user) return;
    setLoading(true);
    setError('');
    api.getGiftPoints()
      .then(setPointsData)
      .catch((err) => setError(err.message || 'Could not load your points'))
      .finally(() => setLoading(false));
  }, [isOpen, user]);

  const balance = pointsData?.currentBalance ?? user?.giftPointsBalance ?? 0;

  return (
    <Modal open={isOpen} onClose={onClose} title="Gift points" size="md">
      <ModalHeader title="Gift points" subtitle="1 point = ₹1 off at checkout · earn 1 point for every ₹100 you spend" onClose={onClose} />

      <div className="overflow-y-auto p-5 sm:p-6 space-y-5">
        <div className="bg-canvas border border-line p-4">
          <span className="text-xs text-fg3">Balance</span>
          <div className="text-3xl font-semibold text-fg">
            {balance} <span className="text-sm font-normal text-fg3">pts</span>
          </div>
          <span className="text-xs text-fg3">Worth {inr(balance)}</span>
        </div>

        <div>
          <h3 className="text-sm font-semibold mb-2">Activity</h3>
          {loading ? (
            <p className="text-sm text-fg3 py-6 text-center" role="status">Loading...</p>
          ) : error ? (
            <p className="text-sm text-danger py-6 text-center" role="alert">{error}</p>
          ) : !pointsData?.history?.length ? (
            <p className="text-sm text-fg3 text-center py-6">No activity yet.</p>
          ) : (
            <ul className="max-h-64 overflow-y-auto divide-y divide-line">
              {pointsData.history.map((tx) => (
                <li key={tx.id} className="flex items-center justify-between py-2.5 text-sm gap-3">
                  <div className="min-w-0">
                    <span className="text-fg block truncate">{tx.description}</span>
                    <span className="text-xs text-fg3">{new Date(tx.createdAt).toLocaleDateString('en-IN')}</span>
                  </div>
                  <span className={`shrink-0 font-medium ${NEGATIVE.has(tx.type) ? 'text-fg3' : 'text-ok'}`}>
                    {NEGATIVE.has(tx.type) ? `−${tx.points}` : `+${tx.points}`}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <button onClick={onClose} className="btn-secondary w-full">Close</button>
      </div>
    </Modal>
  );
}
