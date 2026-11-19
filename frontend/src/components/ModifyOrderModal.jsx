import React, { useEffect, useState } from 'react';
import Modal, { ModalHeader } from './Modal';
import { api } from '../api/client';
import { useToast } from '../context/ToastContext';
import { inr } from '../lib/format';

/**
 * Change an order that has not shipped: send it to another saved address, or take books off it (they are
 * refunded). The server decides what is allowed; this dialog only collects the choice.
 */
export default function ModifyOrderModal({ order, onClose, onDone }) {
  const toast = useToast();
  const [detail, setDetail] = useState(null);
  const [addresses, setAddresses] = useState([]);
  const [remove, setRemove] = useState({});        // orderItemId -> quantity to remove
  const [addressId, setAddressId] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!order) return undefined;
    let cancelled = false;
    setDetail(null); setRemove({}); setAddressId(''); setError('');
    Promise.all([api.getOrderById(order.id), api.getAddresses()])
      .then(([d, a]) => { if (!cancelled) { setDetail(d); setAddresses(a || []); } })
      .catch((err) => { if (!cancelled) setError(err.message || 'Could not load this order'); });
    return () => { cancelled = true; };
  }, [order]);

  const remaining = (item) => item.quantity - (item.refundedQuantity || 0);
  const totalLeft = detail ? detail.items.reduce((n, i) => n + remaining(i), 0) : 0;
  const totalRemoving = Object.values(remove).reduce((n, q) => n + q, 0);
  const currentAddressId = detail?.deliveryAddress?.id;
  const changedAddress = addressId && Number(addressId) !== currentAddressId;
  const removesEverything = totalRemoving >= totalLeft && totalLeft > 0;

  const submit = async () => {
    setError('');
    const removeItems = Object.entries(remove).filter(([, q]) => q > 0).map(([id, quantity]) => ({ orderItemId: Number(id), quantity }));
    if (removeItems.length === 0 && !changedAddress) {
      setError('Choose a book to remove or a different address.');
      return;
    }
    if (removesEverything) {
      setError('That would remove every book. Cancel the order instead.');
      return;
    }
    setBusy(true);
    try {
      const res = await api.modifyOrder(order.id, {
        addressId: changedAddress ? Number(addressId) : null,
        removeItems: removeItems.length ? removeItems : null,
      });
      toast.success(res.message);
      onDone();
    } catch (err) {
      setError(err.message || 'Could not change this order');
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal open={!!order} onClose={() => !busy && onClose()} title="Change order" size="md" z={60}>
      <ModalHeader title="Change order" subtitle={order?.orderNumber} onClose={() => !busy && onClose()} />
      <div className="p-5 sm:p-6 space-y-5 overflow-y-auto">
        {!detail && !error && <p className="text-sm text-fg3" role="status">Loading...</p>}

        {detail && (
          <>
            <fieldset>
              <legend className="text-sm font-semibold mb-2">Remove books (refunded to you)</legend>
              <ul className="space-y-2">
                {detail.items.filter((i) => remaining(i) > 0).map((item) => (
                  <li key={item.id} className="flex items-center justify-between gap-3 bg-canvas px-3 py-2">
                    <div className="min-w-0">
                      <p className="text-sm text-fg truncate">{item.bookTitle}</p>
                      <p className="text-xs text-fg3">{inr(item.unitPrice)} each · {remaining(item)} on the order</p>
                    </div>
                    <label className="text-xs text-fg3 shrink-0">
                      <span className="sr-only">Quantity of {item.bookTitle} to remove</span>
                      <select
                        value={remove[item.id] || 0}
                        onChange={(e) => setRemove({ ...remove, [item.id]: Number(e.target.value) })}
                        className="field w-20"
                      >
                        {Array.from({ length: remaining(item) + 1 }, (_, n) => <option key={n} value={n}>{n}</option>)}
                      </select>
                    </label>
                  </li>
                ))}
              </ul>
              <p className="text-xs text-fg3 mt-2">
                You get back the books' price, their share of the tax and any coupon, and the gift points you spent on them.
                Delivery stays unless the whole order is cancelled.
              </p>
            </fieldset>

            <div>
              <label htmlFor="modify-address" className="text-sm font-semibold block mb-2">Deliver to a different address</label>
              <select id="modify-address" value={addressId} onChange={(e) => setAddressId(e.target.value)} className="field">
                <option value="">Keep the current address</option>
                {addresses.filter((a) => a.id !== currentAddressId).map((a) => (
                  <option key={a.id} value={a.id}>{a.recipientName}, {a.city}, {a.state} {a.postalCode}</option>
                ))}
              </select>
              <p className="text-xs text-fg3 mt-1">An address in a farther delivery zone than the one you paid for needs a new order.</p>
            </div>
          </>
        )}

        {error && <p className="field-error" role="alert">{error}</p>}

        <div className="flex gap-2">
          <button onClick={onClose} disabled={busy} className="btn-secondary flex-1">Keep as is</button>
          <button onClick={submit} disabled={busy || !detail} className="btn-primary flex-1">{busy ? 'Saving...' : 'Save changes'}</button>
        </div>
      </div>
    </Modal>
  );
}
