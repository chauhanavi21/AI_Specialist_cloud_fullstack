import React, { useEffect, useState } from 'react';
import Modal, { ModalHeader } from './Modal';
import { api } from '../api/client';
import { useToast } from '../context/ToastContext';
import { fullDate, inr } from '../lib/format';

const REASONS = [
  'Wrong edition or book',
  'Arrived damaged',
  'Pages missing or misprinted',
  'No longer needed',
  'Other',
];

/** Send delivered books back. Shows what can be returned and until when; the refund is paid once the parcel arrives. */
export default function ReturnModal({ order, onClose, onDone }) {
  const toast = useToast();
  const [eligibility, setEligibility] = useState(null);
  const [qty, setQty] = useState({});
  const [reason, setReason] = useState(REASONS[0]);
  const [other, setOther] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!order) return undefined;
    let cancelled = false;
    setEligibility(null); setQty({}); setReason(REASONS[0]); setOther(''); setError('');
    api.getReturnEligibility(order.id)
      .then((e) => { if (!cancelled) setEligibility(e); })
      .catch((err) => { if (!cancelled) setError(err.message || 'Could not check this order'); });
    return () => { cancelled = true; };
  }, [order]);

  const chosen = eligibility ? eligibility.lines.filter((l) => (qty[l.orderItemId] || 0) > 0) : [];
  const worth = chosen.reduce((sum, l) => sum + l.unitPrice * qty[l.orderItemId], 0);

  const submit = async () => {
    setError('');
    if (chosen.length === 0) { setError('Choose at least one book to return.'); return; }
    const text = reason === 'Other' ? other.trim() : reason;
    if (!text) { setError('Tell us why you are returning the books.'); return; }
    setBusy(true);
    try {
      const res = await api.requestReturn(order.id, {
        reason: text,
        items: chosen.map((l) => ({ orderItemId: l.orderItemId, quantity: qty[l.orderItemId] })),
      });
      toast.success(res.message);
      onDone();
    } catch (err) {
      setError(err.message || 'Could not start this return');
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal open={!!order} onClose={() => !busy && onClose()} title="Return books" size="md" z={60}>
      <ModalHeader title="Return books" subtitle={order?.orderNumber} onClose={() => !busy && onClose()} />
      <div className="p-5 sm:p-6 space-y-5 overflow-y-auto">
        {!eligibility && !error && <p className="text-sm text-fg3" role="status">Checking this order...</p>}

        {eligibility && !eligibility.eligible && (
          <p className="text-sm text-fg2" role="status">{eligibility.reason}</p>
        )}

        {eligibility?.eligible && (
          <>
            <p className="text-xs text-fg3">
              Return by {fullDate(eligibility.returnDeadline)}. A carrier collects the parcel, and your refund is paid when we receive it.
            </p>
            <ul className="space-y-2">
              {eligibility.lines.map((l) => (
                <li key={l.orderItemId} className="flex items-center justify-between gap-3 bg-canvas px-3 py-2">
                  <div className="min-w-0">
                    <p className="text-sm text-fg truncate">{l.bookTitle}</p>
                    <p className="text-xs text-fg3">{inr(l.unitPrice)} each · up to {l.maxQuantity}</p>
                  </div>
                  <label>
                    <span className="sr-only">Quantity of {l.bookTitle} to return</span>
                    <select
                      value={qty[l.orderItemId] || 0}
                      onChange={(e) => setQty({ ...qty, [l.orderItemId]: Number(e.target.value) })}
                      className="field w-20"
                    >
                      {Array.from({ length: l.maxQuantity + 1 }, (_, n) => <option key={n} value={n}>{n}</option>)}
                    </select>
                  </label>
                </li>
              ))}
            </ul>

            <div>
              <label htmlFor="return-reason" className="field-label">Reason</label>
              <select id="return-reason" value={reason} onChange={(e) => setReason(e.target.value)} className="field">
                {REASONS.map((r) => <option key={r} value={r}>{r}</option>)}
              </select>
              {reason === 'Other' && (
                <input
                  aria-label="Describe the reason"
                  value={other}
                  onChange={(e) => setOther(e.target.value)}
                  maxLength={300}
                  placeholder="Tell us what went wrong"
                  className="field mt-2"
                />
              )}
            </div>

            {chosen.length > 0 && (
              <p className="text-sm text-fg2">Books worth {inr(worth)}. The tax and any gift points on them come back with the refund.</p>
            )}
          </>
        )}

        {error && <p className="field-error" role="alert">{error}</p>}

        <div className="flex gap-2">
          <button onClick={onClose} disabled={busy} className="btn-secondary flex-1">Close</button>
          {eligibility?.eligible && (
            <button onClick={submit} disabled={busy} className="btn-primary flex-1">{busy ? 'Sending...' : 'Request return'}</button>
          )}
        </div>
      </div>
    </Modal>
  );
}
