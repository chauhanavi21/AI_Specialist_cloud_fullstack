import React from 'react';
import { CheckCircle2, Truck, Gift, ArrowRight, ShieldCheck, Download } from 'lucide-react';

export default function OrderConfirmationModal({ confirmation, onClose, onViewHistory }) {
  if (!confirmation) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto animate-fadeIn">
      <div className="bg-white rounded-3xl max-w-lg w-full shadow-2xl p-6 sm:p-8 text-center space-y-6">
        {/* Animated Green Checkmark */}
        <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto shadow-inner">
          <CheckCircle2 className="w-10 h-10" />
        </div>

        <div>
          <span className="text-xs font-bold text-ibm-blue uppercase tracking-wider bg-blue-50 px-2.5 py-1 rounded-md">
            Order #{confirmation.orderNumber}
          </span>
          <h2 className="text-2xl font-black text-gray-900 mt-2">
            Payment & Purchase Confirmed!
          </h2>
          <p className="text-xs text-gray-600 mt-2 max-w-sm mx-auto leading-relaxed">
            {confirmation.message}
          </p>
        </div>

        {/* Order Highlights Box */}
        <div className="bg-gray-50 border border-gray-200 rounded-2xl p-4 text-left space-y-3 text-xs">
          <div className="flex items-center justify-between pb-2 border-b border-gray-200">
            <span className="text-gray-500">Amount Paid</span>
            <span className="text-sm font-black text-gray-900">${confirmation.totalAmount?.toFixed(2)}</span>
          </div>

          <div className="flex items-center justify-between">
            <span className="text-gray-500 flex items-center gap-1.5">
              <Truck className="w-3.5 h-3.5 text-ibm-blue" />
              <span>Estimated Delivery</span>
            </span>
            <span className="font-bold text-gray-900">{confirmation.estimatedDeliveryDate}</span>
          </div>

          {confirmation.pointsRedeemed > 0 && (
            <div className="flex items-center justify-between text-amber-700">
              <span className="flex items-center gap-1.5">
                <Gift className="w-3.5 h-3.5" />
                <span>Points Redeemed</span>
              </span>
              <span className="font-bold">-{confirmation.pointsRedeemed} pts</span>
            </div>
          )}

          <div className="flex items-center justify-between text-emerald-700">
            <span className="flex items-center gap-1.5">
              <Gift className="w-3.5 h-3.5" />
              <span>Points Earned on this Purchase</span>
            </span>
            <span className="font-bold">+{confirmation.pointsEarned} pts</span>
          </div>

          <div className="flex items-center justify-between pt-2 border-t border-gray-200 font-semibold text-gray-800">
            <span>New Loyalty Points Balance</span>
            <span className="text-amber-900 bg-amber-100 px-2 py-0.5 rounded-full">{confirmation.newGiftPointsBalance} pts</span>
          </div>
        </div>

        {/* 48-Hour Guarantee Callout (Slide 3 Node 12) */}
        <div className="p-3 bg-blue-50 border border-blue-200 rounded-xl text-left flex items-start gap-2.5">
          <ShieldCheck className="w-5 h-5 text-ibm-blue shrink-0 mt-0.5" />
          <div className="text-[11px] text-gray-700 leading-tight">
            <strong className="block text-gray-900">48-Hour Order Cancellation Rule:</strong>
            You can modify or cancel this order with a 100% full refund & points restoration from your Order History within the next 48 hours.
          </div>
        </div>

        {/* Buttons */}
        <div className="space-y-2 pt-2">
          <button
            onClick={() => {
              onClose();
              onViewHistory();
            }}
            className="w-full py-3 px-4 bg-ibm-blue hover:bg-ibm-darkBlue text-white text-xs font-bold rounded-xl flex items-center justify-center gap-2 shadow-md transition"
          >
            <span>View in Order History & Tracking</span>
            <ArrowRight className="w-4 h-4" />
          </button>
          <button
            onClick={onClose}
            className="w-full py-2.5 text-xs font-semibold text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-xl transition"
          >
            Continue Browsing Catalog
          </button>
        </div>
      </div>
    </div>
  );
}
