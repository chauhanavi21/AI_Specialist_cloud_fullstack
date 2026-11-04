import React, { useState, useEffect } from 'react';
import { X, History, RefreshCw, XCircle, Clock, Truck, ShieldCheck, CheckCircle2, AlertTriangle, AlertCircle } from 'lucide-react';
import { api } from '../api/client';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

export default function OrderHistoryModal({ isOpen, onClose }) {
  const { user, refreshUser } = useAuth();
  const { refreshCart, setIsCartOpen } = useCart();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedOrderDetails, setSelectedOrderDetails] = useState(null);
  const [cancelModalOrder, setCancelModalOrder] = useState(null);
  const [cancelReason, setCancelReason] = useState('Found a better price / changed mind');
  const [isProcessing, setIsProcessing] = useState(false);
  const [actionMessage, setActionMessage] = useState('');

  const loadOrders = () => {
    if (!user) return;
    setLoading(true);
    api.getOrderHistory()
      .then((data) => setOrders(data || []))
      .catch((err) => console.error('Failed to load orders', err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (isOpen) {
      loadOrders();
      setActionMessage('');
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleBuyItAgain = async (orderId) => {
    try {
      setIsProcessing(true);
      await api.buyItAgain(orderId);
      await refreshCart();
      onClose();
      setIsCartOpen(true);
    } catch (err) {
      alert(err.message || 'Failed to reorder items');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleConfirmCancel = async () => {
    if (!cancelModalOrder) return;
    try {
      setIsProcessing(true);
      const res = await api.cancelOrder(cancelModalOrder.id, cancelReason);
      setActionMessage(res.message);
      setCancelModalOrder(null);
      await loadOrders();
      await refreshUser();
    } catch (err) {
      alert(err.message || 'Failed to cancel order');
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto animate-fadeIn">
      <div className="relative bg-white rounded-3xl max-w-3xl w-full shadow-2xl overflow-hidden my-8 max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="p-6 border-b border-gray-200 flex items-center justify-between bg-gray-50">
          <div className="flex items-center gap-2">
            <History className="w-5 h-5 text-ibm-blue" />
            <div>
              <h2 className="text-xl font-extrabold text-gray-900">Order History & Tracking</h2>
              <p className="text-xs text-gray-500">Buy It Again & 48-Hour Order Cancellation Manager</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-gray-400 hover:text-gray-700 rounded-full hover:bg-gray-200 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {actionMessage && (
          <div className="mx-6 mt-4 p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-xl font-semibold flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            <span>{actionMessage}</span>
          </div>
        )}

        {/* Content */}
        <div className="overflow-y-auto p-6 space-y-4">
          {loading ? (
            <div className="p-16 text-center">
              <div className="w-8 h-8 border-4 border-ibm-blue border-t-transparent rounded-full animate-spin mx-auto mb-3" />
              <p className="text-xs text-gray-500">Retrieving order history...</p>
            </div>
          ) : orders.length === 0 ? (
            <div className="text-center py-16">
              <History className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <p className="text-base font-semibold text-gray-800">No previous orders found</p>
              <p className="text-xs text-gray-500 mt-1">Place your first order to test our 48-hour cancellation and points loyalty system!</p>
            </div>
          ) : (
            orders.map((order) => (
              <div
                key={order.id}
                className="p-5 bg-gray-50 rounded-2xl border border-gray-200 space-y-3 hover:border-gray-300 transition"
              >
                {/* Order Top Bar */}
                <div className="flex flex-wrap items-center justify-between gap-2 border-b border-gray-200/80 pb-3">
                  <div className="flex items-center gap-2">
                    <span className="font-extrabold text-sm text-gray-900">{order.orderNumber}</span>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                      order.orderStatus === 'CANCELLED'
                        ? 'bg-rose-100 text-rose-800'
                        : order.orderStatus === 'CONFIRMED'
                        ? 'bg-blue-100 text-blue-800'
                        : 'bg-emerald-100 text-emerald-800'
                    }`}>
                      {order.orderStatus} · {order.paymentStatus}
                    </span>
                  </div>

                  <div className="text-xs text-gray-500">
                    Placed on {new Date(order.createdAt).toLocaleDateString()}
                  </div>
                </div>

                {/* Info row */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                  <div>
                    <span className="text-gray-400 block text-[10px]">Total Amount</span>
                    <span className="font-bold text-gray-900">${order.totalAmount?.toFixed(2)}</span>
                  </div>
                  <div>
                    <span className="text-gray-400 block text-[10px]">Estimated Delivery</span>
                    <span className="font-medium text-gray-800 flex items-center gap-1">
                      <Truck className="w-3 h-3 text-ibm-blue" />
                      {order.estimatedDeliveryDate}
                    </span>
                  </div>
                  <div>
                    <span className="text-gray-400 block text-[10px]">Items</span>
                    <span className="font-medium text-gray-800">{order.itemCount} book(s)</span>
                  </div>
                  <div>
                    <span className="text-gray-400 block text-[10px]">Points Earned / Used</span>
                    <span className="font-medium text-amber-700">+{order.pointsEarned} / -{order.pointsRedeemed}</span>
                  </div>
                </div>

                {/* 48-Hour Cancellation Eligibility Callout (Slide 3 Node 12, Slide 12) */}
                {order.canCancel && (
                  <div className="p-2.5 bg-blue-50/70 border border-blue-200 rounded-xl flex items-center justify-between text-xs">
                    <div className="flex items-center gap-1.5 text-ibm-blue font-semibold">
                      <Clock className="w-4 h-4 text-ibm-blue" />
                      <span>{order.hoursRemainingToCancel} hrs left in 48-hour free cancellation window</span>
                    </div>
                    <button
                      onClick={() => setCancelModalOrder(order)}
                      className="px-3 py-1 bg-white hover:bg-rose-50 text-rose-600 border border-rose-300 rounded-lg font-bold text-[11px] transition shadow-xs"
                    >
                      Cancel Order
                    </button>
                  </div>
                )}

                {order.orderStatus === 'CANCELLED' && (
                  <div className="p-2 bg-rose-50 text-rose-700 text-xs rounded-xl flex items-center gap-1.5 font-medium">
                    <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                    <span>Order cancelled within 48h guarantee. Full refund initiated & redeemed points restored.</span>
                  </div>
                )}

                {/* Action Buttons: Buy It Again (Slide 3 Node 2, Slide 6) */}
                <div className="flex items-center justify-end gap-2 pt-2 border-t border-gray-200/60">
                  <button
                    onClick={() => handleBuyItAgain(order.id)}
                    disabled={isProcessing}
                    className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl flex items-center gap-1.5 shadow-sm transition active:scale-95"
                    title="1-Click Reorder items into active shopping cart"
                  >
                    <RefreshCw className="w-3.5 h-3.5" />
                    <span>Buy It Again (1-Click Reorder)</span>
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Cancel Order Confirmation Dialog */}
      {cancelModalOrder && (
        <div className="fixed inset-0 z-60 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl space-y-4 text-center">
            <div className="w-12 h-12 bg-rose-100 text-rose-600 rounded-full flex items-center justify-center mx-auto">
              <AlertTriangle className="w-6 h-6" />
            </div>

            <h3 className="text-lg font-bold text-gray-900">
              Cancel Order #{cancelModalOrder.orderNumber}?
            </h3>

            <p className="text-xs text-gray-600 leading-relaxed">
              This order was placed within the last 48 hours. If you proceed:
              <br />
              • <strong>${cancelModalOrder.totalAmount?.toFixed(2)}</strong> will be 100% refunded.
              <br />
              • Any redeemed gift points (<strong>{cancelModalOrder.pointsRedeemed} pts</strong>) will be credited back to your balance immediately.
            </p>

            <div className="text-left space-y-1">
              <label className="text-[11px] font-bold text-gray-700">Reason for Cancellation</label>
              <select
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                className="w-full p-2 bg-gray-50 border border-gray-300 rounded-xl text-xs focus:outline-none focus:border-ibm-blue"
              >
                <option value="Ordered wrong edition / book">Ordered wrong edition / book</option>
                <option value="Found a better price elsewhere">Found a better price elsewhere</option>
                <option value="Changed delivery address">Changed delivery address</option>
                <option value="Accidental purchase">Accidental purchase</option>
              </select>
            </div>

            <div className="flex gap-2 pt-2">
              <button
                onClick={() => setCancelModalOrder(null)}
                className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold rounded-xl"
              >
                Keep Order
              </button>
              <button
                onClick={handleConfirmCancel}
                disabled={isProcessing}
                className="flex-1 py-2.5 bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold rounded-xl shadow-md"
              >
                {isProcessing ? 'Cancelling...' : 'Confirm 100% Refund'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
