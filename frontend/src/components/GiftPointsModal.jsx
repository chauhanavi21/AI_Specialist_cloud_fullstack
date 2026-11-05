import React, { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function GiftPointsModal({ isOpen, onClose }) {
  const { user } = useAuth();
  const [pointsData, setPointsData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isOpen || !user) return;
    setLoading(true);
    api.getGiftPoints()
      .then((data) => setPointsData(data))
      .catch((err) => console.error('Failed to load gift points', err))
      .finally(() => setLoading(false));
  }, [isOpen, user]);

  if (!isOpen) return null;

  const balance = pointsData?.currentBalance ?? user?.giftPointsBalance ?? 0;

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/40 flex items-end sm:items-center justify-center p-0 sm:p-4 overflow-y-auto">
      <div className="relative bg-white rounded-t-xl sm:rounded-lg max-w-md w-full sm:my-8 p-4 sm:p-6 space-y-5 max-h-[95dvh] overflow-y-auto">
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Gift points</h2>
            <p className="text-xs text-slate-500">10 points = $1.00 off at checkout</p>
          </div>
          <button
            onClick={onClose}
            className="p-2 -m-2 text-slate-400 hover:text-slate-700 rounded-md hover:bg-brand-50"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="bg-brand-50 border border-brand-100 rounded-lg p-4">
          <span className="text-xs text-slate-500">Balance</span>
          <div className="text-3xl font-semibold text-slate-900">
            {balance} <span className="text-sm font-normal text-slate-500">pts</span>
          </div>
          <span className="text-xs text-slate-500">Worth ${(balance * 0.1).toFixed(2)}</span>
        </div>

        <div>
          <h4 className="text-sm font-semibold text-slate-900 mb-2">Activity</h4>

          {loading ? (
            <p className="text-sm text-slate-400 py-6 text-center">Loading...</p>
          ) : !pointsData?.history?.length ? (
            <p className="text-sm text-slate-500 text-center py-6">No activity yet.</p>
          ) : (
            <div className="max-h-60 overflow-y-auto divide-y divide-brand-100">
              {pointsData.history.map((tx) => (
                <div key={tx.id} className="flex items-center justify-between py-2.5 text-sm">
                  <div>
                    <span className="text-slate-800 block line-clamp-1">{tx.description}</span>
                    <span className="text-xs text-slate-400">
                      {new Date(tx.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                  <span className={tx.type === 'REDEEMED' ? 'text-slate-500' : 'text-brand-700'}>
                    {tx.type === 'REDEEMED' ? `-${tx.points}` : `+${tx.points}`}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        <button
          onClick={onClose}
          className="w-full py-2.5 border border-brand-200 hover:bg-brand-50 text-slate-700 text-sm rounded-md"
        >
          Close
        </button>
      </div>
    </div>
  );
}
