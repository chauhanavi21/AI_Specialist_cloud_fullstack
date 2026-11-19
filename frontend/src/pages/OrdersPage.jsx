import React, { useState, useEffect, useCallback } from 'react';
import Modal, { ModalHeader } from '../components/Modal';
import TrackingModal from '../components/TrackingModal';
import ModifyOrderModal from '../components/ModifyOrderModal';
import ReturnModal from '../components/ReturnModal';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useToast } from '../context/ToastContext';
import { fullDate, inr } from '../lib/format';

const REASONS = [
  'Ordered wrong edition / book',
  'Found a better price elsewhere',
  'Changed delivery address',
  'Accidental purchase',
];

export default function OrdersPage() {
  const { user, refreshUser, openAuthModal } = useAuth();
  const { refreshCart, setIsCartOpen } = useCart();
  const toast = useToast();

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [cancelOrder, setCancelOrder] = useState(null);
  const [reason, setReason] = useState(REASONS[0]);
  const [busy, setBusy] = useState(false);
  const [trackOrder, setTrackOrder] = useState(null);
  const [modifyOrder, setModifyOrder] = useState(null);
  const [returnOrder, setReturnOrder] = useState(null);
  const [returns, setReturns] = useState([]);

  const load = useCallback(() => {
    if (!user) return Promise.resolve();
    setError('');
    return Promise.all([api.getOrderHistory(), api.getReturns().catch(() => [])])
      .then(([data, rets]) => { setOrders(data || []); setReturns(rets || []); })
      .catch((err) => setError(err.message || 'Could not load your orders'));
  }, [user?.id]);

  useEffect(() => {
    setLoading(true);
    load().finally(() => setLoading(false));
  }, [load]);

  if (!user) {
    return (
      <div className="panel p-10 text-center">
        <p className="text-fg">Sign in to see your orders.</p>
        <button onClick={() => openAuthModal('login')} className="btn-primary mt-4">Sign in</button>
      </div>
    );
  }

  const buyAgain = async (orderId) => {
    setBusy(true);
    try {
      await api.buyItAgain(orderId);
      await refreshCart();
      setIsCartOpen(true);
    } catch (err) {
      toast.error(err.message || 'Could not add these books to your basket');
    } finally {
      setBusy(false);
    }
  };

  const afterChange = async () => {
    setModifyOrder(null);
    setReturnOrder(null);
    await Promise.all([load(), refreshUser()]);
  };

  const withdraw = async (returnId) => {
    setBusy(true);
    try {
      const res = await api.withdrawReturn(returnId);
      toast.success(res.message);
      await load();
    } catch (err) {
      toast.error(err.message || 'Could not withdraw this return');
    } finally {
      setBusy(false);
    }
  };

  const confirmCancel = async () => {
    setBusy(true);
    try {
      const res = await api.cancelOrder(cancelOrder.id, reason);
      toast.success(res.message);
      setCancelOrder(null);
      await Promise.all([load(), refreshUser()]);
    } catch (err) {
      toast.error(err.message || 'Could not cancel this order');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section aria-labelledby="orders-title" className="max-w-3xl">
      <h1 id="orders-title" className="text-xl font-semibold mb-1">My Orders</h1>
      <p className="text-xs text-fg3 mb-5">Cancel within 48 hours for a full refund, change an order before it ships, and return delivered books within the return window. Use Buy it again to reorder in one click.</p>

      {loading ? (
        <p className="text-sm text-fg3 py-12 text-center" role="status">Loading your orders...</p>
      ) : error ? (
        <div className="panel p-8 text-center space-y-3">
          <p className="text-sm text-danger" role="alert">{error}</p>
          <button onClick={() => { setLoading(true); load().finally(() => setLoading(false)); }} className="btn-secondary">Try again</button>
        </div>
      ) : orders.length === 0 ? (
        <div className="panel p-10 text-center">
          <p className="text-fg">No orders yet</p>
          <p className="text-sm text-fg3 mt-1">Orders you place will show up here.</p>
        </div>
      ) : (
        <ul className="space-y-4">
          {orders.map((order) => (
            <li key={order.id} className="panel p-4 space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <span className="font-medium">{order.orderNumber}</span>
                  <span
                    className={`text-xs px-2 py-0.5 ${
                      order.orderStatus === 'CANCELLED' || order.orderStatus === 'RETURNED'
                        ? 'bg-layer2 text-fg3'
                        : 'bg-[#044317] text-[#a7f0ba]'
                    }`}
                  >
                    {order.orderStatus}
                  </span>
                  {order.paymentStatus === 'PARTIALLY_REFUNDED' && (
                    <span className="text-xs px-2 py-0.5 bg-layer2 text-fg2">Partly refunded</span>
                  )}
                </div>
                <span className="text-xs text-fg3">{fullDate(order.createdAt)}</span>
              </div>

              <dl className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-sm">
                <div><dt className="text-xs text-fg3">Total</dt><dd>{inr(order.totalAmount)}</dd></div>
                <div><dt className="text-xs text-fg3">Delivery</dt><dd>{fullDate(order.estimatedDeliveryDate)}</dd></div>
                <div><dt className="text-xs text-fg3">Items</dt><dd>{order.itemCount}</dd></div>
                <div><dt className="text-xs text-fg3">Points</dt><dd>+{order.pointsEarned} / −{order.pointsRedeemed}</dd></div>
              </dl>

              {Number(order.refundedAmount) > 0 && (
                <p className="text-sm text-fg2">{inr(order.refundedAmount)} refunded so far.</p>
              )}

              {order.canCancel && (
                <div className="flex flex-col min-[420px]:flex-row min-[420px]:items-center justify-between gap-2 text-sm bg-canvas px-3 py-2">
                  <span className="text-fg2">{order.hoursRemainingToCancel} hours left to cancel for free</span>
                  <button onClick={() => { setReason(REASONS[0]); setCancelOrder(order); }} className="text-danger hover:underline text-left">
                    Cancel order
                  </button>
                </div>
              )}

              {order.orderStatus === 'CANCELLED' && (
                <p className="text-sm text-fg3">Cancelled. Refunded in full and redeemed points returned.</p>
              )}

              {order.canReturn && order.returnDeadline && (
                <p className="text-sm text-fg2">Delivered. You can return books until {fullDate(order.returnDeadline)}.</p>
              )}

              {returns.filter((r) => r.orderId === order.id).map((r) => (
                <div key={r.id} className="text-sm bg-canvas px-3 py-2 space-y-1">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="text-fg">Return {r.rmaNumber}: <strong>{r.status.replace('_', ' ').toLowerCase()}</strong></span>
                    {r.status === 'REQUESTED' && (
                      <button onClick={() => withdraw(r.id)} disabled={busy} className="text-danger hover:underline">Withdraw</button>
                    )}
                  </div>
                  <p className="text-xs text-fg3">
                    {r.items.map((i) => `${i.quantity} × ${i.bookTitle}`).join(', ')}
                    {r.status === 'REFUNDED'
                      ? ` · ${inr(r.refundAmount)} refunded (${r.refundReference})`
                      : r.status === 'REJECTED'
                        ? (r.adminNote ? ` · ${r.adminNote}` : '')
                        : ` · about ${inr(r.estimatedRefund)} once we receive it · collection ${r.returnTrackingNumber}`}
                  </p>
                </div>
              ))}

              <div className="flex flex-wrap justify-end gap-2">
                {order.shipmentStatus && order.orderStatus !== 'CANCELLED' && (
                  <button onClick={() => setTrackOrder(order)} className="btn-secondary w-full min-[420px]:w-auto">Track</button>
                )}
                {order.canModify && (
                  <button onClick={() => setModifyOrder(order)} className="btn-secondary w-full min-[420px]:w-auto">Change order</button>
                )}
                {order.canReturn && (
                  <button onClick={() => setReturnOrder(order)} className="btn-secondary w-full min-[420px]:w-auto">Return books</button>
                )}
                <button onClick={() => buyAgain(order.id)} disabled={busy} className="btn-primary w-full min-[420px]:w-auto">
                  Buy it again
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <TrackingModal order={trackOrder} onClose={() => setTrackOrder(null)} />
      <ModifyOrderModal order={modifyOrder} onClose={() => setModifyOrder(null)} onDone={afterChange} />
      <ReturnModal order={returnOrder} onClose={() => setReturnOrder(null)} onDone={afterChange} />

      <Modal open={!!cancelOrder} onClose={() => !busy && setCancelOrder(null)} title="Cancel order" size="sm" z={60}>
        <ModalHeader title={`Cancel order ${cancelOrder?.orderNumber || ''}?`} onClose={() => !busy && setCancelOrder(null)} />
        <div className="p-5 sm:p-6 space-y-4">
          <p className="text-sm text-fg2">
            {inr(cancelOrder?.totalAmount)} will be refunded in full
            {cancelOrder?.pointsRedeemed > 0 && ` and ${cancelOrder.pointsRedeemed} points returned to your balance`}.
          </p>
          <div>
            <label htmlFor="cancel-reason" className="field-label">Reason</label>
            <select id="cancel-reason" value={reason} onChange={(e) => setReason(e.target.value)} className="field">
              {REASONS.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
          </div>
          <div className="flex gap-2">
            <button onClick={() => setCancelOrder(null)} disabled={busy} className="btn-secondary flex-1">Keep order</button>
            <button onClick={confirmCancel} disabled={busy} className="btn-danger flex-1">
              {busy ? 'Cancelling...' : 'Cancel order'}
            </button>
          </div>
        </div>
      </Modal>
    </section>
  );
}
