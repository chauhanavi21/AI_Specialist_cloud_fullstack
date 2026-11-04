import React, { useState, useEffect } from 'react';
import { X, Gift, ArrowUpRight, ArrowDownLeft, RefreshCcw, Sparkles } from 'lucide-react';
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

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto animate-fadeIn">
      <div className="relative bg-white rounded-3xl max-w-lg w-full shadow-2xl overflow-hidden my-8 p-6 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-gray-100">
          <div className="flex items-center gap-2">
            <div className="w-10 h-10 rounded-2xl bg-amber-100 text-amber-700 flex items-center justify-center">
              <Gift className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-gray-900">Loyalty Gift Points Wallet</h2>
              <p className="text-xs text-gray-500">10 Points = $1.00 USD Discount</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-gray-400 hover:text-gray-700 rounded-full hover:bg-gray-100 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Balance Card */}
        <div className="bg-gradient-to-br from-amber-500 to-amber-700 text-white rounded-2xl p-5 shadow-lg relative overflow-hidden">
          <div className="relative z-10 flex items-center justify-between">
            <div>
              <span className="text-xs font-semibold text-amber-100 uppercase tracking-wider block">Available Balance</span>
              <div className="text-3xl font-black mt-1">{pointsData?.currentBalance ?? user?.giftPointsBalance ?? 0} <span className="text-sm font-normal">pts</span></div>
              <span className="text-xs text-amber-100 mt-1 block">
                ≈ ${((pointsData?.currentBalance ?? user?.giftPointsBalance ?? 0) * 0.10).toFixed(2)} USD Store Credit
              </span>
            </div>
            <Sparkles className="w-12 h-12 text-amber-200/40" />
          </div>
        </div>

        {/* Ledger Transaction History */}
        <div className="space-y-3">
          <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider">Points Ledger Activity</h4>

          {loading ? (
            <div className="p-8 text-center">
              <div className="w-6 h-6 border-3 border-amber-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
              <p className="text-xs text-gray-400">Loading wallet history...</p>
            </div>
          ) : pointsData?.history?.length === 0 ? (
            <p className="text-xs text-gray-500 text-center py-6">No points activity yet.</p>
          ) : (
            <div className="max-h-60 overflow-y-auto space-y-2 pr-1">
              {pointsData?.history?.map((tx) => (
                <div
                  key={tx.id}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-xl border border-gray-100 text-xs"
                >
                  <div className="flex items-center gap-2.5">
                    {tx.type === 'EARNED' && (
                      <div className="w-7 h-7 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
                        <ArrowDownLeft className="w-4 h-4" />
                      </div>
                    )}
                    {tx.type === 'REDEEMED' && (
                      <div className="w-7 h-7 rounded-lg bg-rose-100 text-rose-700 flex items-center justify-center shrink-0">
                        <ArrowUpRight className="w-4 h-4" />
                      </div>
                    )}
                    {tx.type === 'REFUNDED' && (
                      <div className="w-7 h-7 rounded-lg bg-blue-100 text-ibm-blue flex items-center justify-center shrink-0">
                        <RefreshCcw className="w-3.5 h-3.5" />
                      </div>
                    )}
                    <div>
                      <span className="font-bold text-gray-800 block line-clamp-1">{tx.description}</span>
                      <span className="text-[10px] text-gray-400">
                        {new Date(tx.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                  </div>

                  <span className={`font-black ${
                    tx.type === 'REDEEMED' ? 'text-rose-600' : 'text-emerald-600'
                  }`}>
                    {tx.type === 'REDEEMED' ? `-${tx.points}` : `+${tx.points}`} pts
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        <button
          onClick={onClose}
          className="w-full py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-800 text-xs font-bold rounded-xl transition"
        >
          Close Wallet
        </button>
      </div>
    </div>
  );
}
