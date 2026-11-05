import React, { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import { api } from '../api/client';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

export default function OrderHistoryModal({ isOpen, onClose }) {
  const { user, refreshUser } = useAuth();
  const { refreshCart, setIsCartOpen } = useCart();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cancelModalOrder, setCancelModalOrder] = useState(null);
  const [cancelReason, setCancelReason] = useState('Ordered wrong edition / book');
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

  const statusClass = (status) =>
    status === 'CANCELLED'
      ? 'bg-slate-100 text-slate-600'
      : 'bg-brand-100 text-brand-800';

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/40 flex items-end sm:items-center justify-center p-0 sm:p-4 overflow-y-auto">
      <div className="relative bg-white rounded-t-xl sm:rounded-lg max-w-2xl w-full overflow-hidden sm:my-8 max-h-[95dvh] sm:max-h-[92dvh] flex flex-col">
        <div className="px-4 sm:px-6 py-4 border-b border-brand-100 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">Your orders</h2>
          <button
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-700 rounded-md hover:bg-brand-50"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {actionMessage && (
          <div className="mx-4 sm:mx-6 mt-4 p-3 bg-brand-50 border border-brand-200 text-brand-800 text-sm rounded-md">
            {actionMessage}
          </div>
        )}

        <div className="overflow-y-auto p-4 sm:p-6 space-y-4">
          {loading ? (
            <p className="text-sm text-slate-500 text-center py-12">Loading...</p>
          ) : orders.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-base text-slate-800">No orders yet</p>
              <p className="text-sm text-slate-500 mt-1">Orders you place will show up here.</p>
            </div>
          ) : (
            orders.map((order) => (
              <div key={order.id} className="p-4 border border-brand-100 rounded-lg space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-slate-900">{order.orderNumber}</span>
                    <span className={`text-xs px-2 py-0.5 rounded ${statusClass(order.orderStatus)}`}>
                      {order.orderStatus}
                    </span>
                  </div>
                  <span className="text-xs text-slate-500">
                    {new Date(order.createdAt).toLocaleDateString()}
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-sm">
                  <div>
                    <span className="text-xs text-slate-400 block">Total</span>
                    <span className="text-slate-900">${order.totalAmount?.toFixed(2)}</span>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400 block">Delivery</span>
                    <span className="text-slate-900">{order.estimatedDeliveryDate}</span>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400 block">Items</span>
                    <span className="text-slate-900">{order.itemCount}</span>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400 block">Points</span>
                    <span className="text-slate-900">+{order.pointsEarned} / -{order.pointsRedeemed}</span>
                  </div>
                </div>

                {order.canCancel && (
                  <div className="flex flex-col min-[420px]:flex-row min-[420px]:items-center justify-between gap-1 min-[420px]:gap-3 text-sm bg-brand-50 rounded-md px-3 py-2">
                    <span className="text-slate-600">
                      {order.hoursRemainingToCancel} hours left to cancel for free
                    </span>
                    <button
                      onClick={() => setCancelModalOrder(order)}
                      className="text-red-600 hover:underline"
                    >
                      Cancel order
                    </button>
                  </div>
                )}

                {order.orderStatus === 'CANCELLED' && (
                  <p className="text-sm text-slate-500">
                    Cancelled. Refunded in full and redeemed points returned.
                  </p>
                )}

                <div className="flex justify-end">
                  <button
                    onClick={() => handleBuyItAgain(order.id)}
                    disabled={isProcessing}
                    className="w-full min-[420px]:w-auto px-4 py-2.5 sm:py-2 bg-brand-600 hover:bg-brand-700 disabled:opacity-60 text-white text-sm font-medium rounded-md"
                  >
                    Buy it again
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {cancelModalOrder && (
        <div className="fixed inset-0 z-[60] bg-slate-900/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg max-w-sm w-full p-6 space-y-4">
            <h3 className="text-lg font-semibold text-slate-900">
              Cancel order {cancelModalOrder.orderNumber}?
            </h3>

            <p className="text-sm text-slate-600 leading-relaxed">
              ${cancelModalOrder.totalAmount?.toFixed(2)} will be refunded in full
              {cancelModalOrder.pointsRedeemed > 0 &&
                ` and ${cancelModalOrder.pointsRedeemed} points returned to your balance`}
              .
            </p>

            <div>
              <label className="block text-sm text-slate-700 mb-1">Reason</label>
              <select
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                className="w-full px-3 py-2 bg-white border border-brand-200 rounded-md text-sm focus:outline-none focus:border-brand-500"
              >
                <option value="Ordered wrong edition / book">Ordered wrong edition / book</option>
                <option value="Found a better price elsewhere">Found a better price elsewhere</option>
                <option value="Changed delivery address">Changed delivery address</option>
                <option value="Accidental purchase">Accidental purchase</option>
              </select>
            </div>

            <div className="flex gap-2 pt-1">
              <button
                onClick={() => setCancelModalOrder(null)}
                className="flex-1 py-2.5 border border-brand-200 hover:bg-brand-50 text-slate-700 text-sm rounded-md"
              >
                Keep order
              </button>
              <button
                onClick={handleConfirmCancel}
                disabled={isProcessing}
                className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 disabled:opacity-60 text-white text-sm font-medium rounded-md"
              >
                {isProcessing ? 'Cancelling...' : 'Cancel order'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
