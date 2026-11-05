import React from 'react';
import { CheckCircle2 } from 'lucide-react';

export default function OrderConfirmationModal({ confirmation, onClose, onViewHistory }) {
  if (!confirmation) return null;

  const row = 'flex items-center justify-between';

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/40 flex items-end sm:items-center justify-center p-0 sm:p-4 overflow-y-auto">
      <div className="bg-white rounded-t-xl sm:rounded-lg max-w-md w-full max-h-[95dvh] overflow-y-auto p-5 sm:p-8 space-y-5">
        <div className="text-center">
          <CheckCircle2 className="w-10 h-10 text-brand-600 mx-auto mb-3" />
          <h2 className="text-xl font-semibold text-slate-900">Order confirmed</h2>
          <p className="text-sm text-slate-500 mt-1">Order #{confirmation.orderNumber}</p>
          {confirmation.message && (
            <p className="text-sm text-slate-600 mt-2">{confirmation.message}</p>
          )}
        </div>

        <div className="border border-brand-100 rounded-lg p-4 text-sm space-y-2">
          <div className={row}>
            <span className="text-slate-500">Amount paid</span>
            <span className="font-semibold text-slate-900">${confirmation.totalAmount?.toFixed(2)}</span>
          </div>
          <div className={row}>
            <span className="text-slate-500">Estimated delivery</span>
            <span className="text-slate-900">{confirmation.estimatedDeliveryDate}</span>
          </div>
          {confirmation.pointsRedeemed > 0 && (
            <div className={row}>
              <span className="text-slate-500">Points redeemed</span>
              <span className="text-slate-900">-{confirmation.pointsRedeemed}</span>
            </div>
          )}
          <div className={row}>
            <span className="text-slate-500">Points earned</span>
            <span className="text-slate-900">+{confirmation.pointsEarned}</span>
          </div>
          <div className={`${row} pt-2 border-t border-brand-100`}>
            <span className="text-slate-500">Points balance</span>
            <span className="font-semibold text-slate-900">{confirmation.newGiftPointsBalance}</span>
          </div>
        </div>

        <p className="text-xs text-slate-500">
          You can cancel this order from your order history within 48 hours for a full refund
          and your points restored.
        </p>

        <div className="space-y-2">
          <button
            onClick={() => {
              onClose();
              onViewHistory();
            }}
            className="w-full py-2.5 bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium rounded-md"
          >
            View orders
          </button>
          <button
            onClick={onClose}
            className="w-full py-2.5 text-sm text-slate-600 hover:bg-brand-50 rounded-md"
          >
            Continue shopping
          </button>
        </div>
      </div>
    </div>
  );
}
