import React, { useEffect, useState } from 'react';
import { CheckCircle2, Circle } from 'lucide-react';
import Modal, { ModalHeader } from './Modal';
import { api } from '../api/client';
import { fullDate, inr } from '../lib/format';

const STEPS = ['ORDER_PLACED', 'LABEL_CREATED', 'SHIPPED', 'DELIVERED'];
const STEP_LABELS = {
  ORDER_PLACED: 'Order placed',
  LABEL_CREATED: 'Label created',
  SHIPPED: 'Shipped',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
};

const when = (iso) => (iso ? new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' }) : '');

/** Where the parcel is: carrier, tracking number and a timeline of what has happened and what is next. */
export default function TrackingModal({ order, onClose }) {
  const [shipment, setShipment] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!order) return undefined;
    let cancelled = false;
    setShipment(null);
    setError('');
    api.trackShipment(order.id)
      .then((s) => { if (!cancelled) setShipment(s); })
      .catch((err) => { if (!cancelled) setError(err.message || 'Could not load tracking'); });
    return () => { cancelled = true; };
  }, [order]);

  const done = new Set((shipment?.events || []).map((e) => e.status));
  const cancelled = shipment?.status === 'CANCELLED';

  return (
    <Modal open={!!order} onClose={onClose} title="Track shipment" size="sm" z={60}>
      <ModalHeader title="Track shipment" subtitle={order?.orderNumber} onClose={onClose} />
      <div className="p-5 sm:p-6 space-y-4">
        {error && <p className="text-sm text-danger" role="alert">{error}</p>}
        {!shipment && !error && <p className="text-sm text-fg3" role="status">Loading...</p>}

        {shipment && (
          <>
            <dl className="grid grid-cols-2 gap-3 text-sm">
              <div><dt className="text-xs text-fg3">Carrier</dt><dd>{shipment.carrier}</dd></div>
              <div><dt className="text-xs text-fg3">Tracking number</dt><dd className="break-all">{shipment.trackingNumber}</dd></div>
              <div><dt className="text-xs text-fg3">Delivery zone</dt><dd>{shipment.shippingZone ? shipment.shippingZone.charAt(0) + shipment.shippingZone.slice(1).toLowerCase() : '-'}</dd></div>
              <div><dt className="text-xs text-fg3">Delivery charge</dt><dd>{Number(shipment.shippingFee) === 0 ? 'Free' : inr(shipment.shippingFee)}</dd></div>
            </dl>

            <ol className="space-y-3" aria-label="Shipment progress">
              {(cancelled ? ['ORDER_PLACED', 'LABEL_CREATED', 'CANCELLED'] : STEPS).map((step) => {
                const event = shipment.events.find((e) => e.status === step);
                const reached = done.has(step);
                return (
                  <li key={step} className="flex gap-3">
                    {reached
                      ? <CheckCircle2 className="w-5 h-5 text-ok shrink-0" aria-hidden="true" />
                      : <Circle className="w-5 h-5 text-fg3 shrink-0" aria-hidden="true" />}
                    <div>
                      <p className={`text-sm ${reached ? 'text-fg' : 'text-fg3'}`}>
                        {STEP_LABELS[step]}
                        <span className="sr-only">{reached ? ' (done)' : ' (not yet)'}</span>
                      </p>
                      <p className="text-xs text-fg3">
                        {event ? `${event.description} · ${when(event.at)}` : step === 'DELIVERED' ? `Expected by ${fullDate(shipment.estimatedDeliveryDate)}` : ''}
                      </p>
                    </div>
                  </li>
                );
              })}
            </ol>
          </>
        )}
      </div>
    </Modal>
  );
}
