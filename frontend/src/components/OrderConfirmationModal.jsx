import React from 'react';
import { CheckCircle2 } from 'lucide-react';
import Modal from './Modal';
import BookCover from './BookCover';
import { fullDate, inr } from '../lib/format';

export default function OrderConfirmationModal({ confirmation, onClose, onViewHistory }) {
  const row = 'flex items-center justify-between';

  return (
    <Modal open={!!confirmation} onClose={onClose} title="Order confirmed" size="md">
      {confirmation && (
        <div className="overflow-y-auto p-5 sm:p-8 space-y-5">
          <div className="text-center">
            <CheckCircle2 className="w-12 h-12 text-ok mx-auto mb-3" aria-hidden="true" />
            <h2 className="text-xl font-semibold text-fg">Your purchase of the following reads is successful</h2>
            <p className="text-sm text-fg3 mt-1">Order #{confirmation.orderNumber}</p>
          </div>

          <ul className="grid grid-cols-1 min-[420px]:grid-cols-2 gap-3">
            {confirmation.items.map((item) => (
              <li key={item.id} className="flex gap-3 bg-canvas border border-line p-3">
                <div className="w-14 shrink-0 aspect-[3/4]">
                  <BookCover book={{ id: item.bookId, title: item.bookTitle, author: item.bookAuthor, coverImageUrl: item.coverImageUrl }} compact />
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-fg line-clamp-2">{item.bookTitle}</p>
                  <p className="text-xs text-link truncate">by {item.bookAuthor}</p>
                  <p className="text-sm font-semibold text-fg mt-1">{inr(item.totalPrice)}</p>
                  <p className="text-[11px] text-fg3">Qty {item.quantity}</p>
                </div>
              </li>
            ))}
          </ul>

          <dl className="bg-canvas border border-line p-4 text-sm space-y-2">
            <div className={row}>
              <dt className="text-fg3">Amount paid</dt>
              <dd className="font-semibold text-fg">{inr(confirmation.totalAmount)}</dd>
            </div>
            {confirmation.couponDiscount > 0 && (
              <div className={row}><dt className="text-fg3">Coupon savings</dt><dd className="text-ok">−{inr(confirmation.couponDiscount)}</dd></div>
            )}
            {confirmation.walletAmountUsed > 0 && (
              <div className={row}><dt className="text-fg3">Paid from wallet</dt><dd>{inr(confirmation.walletAmountUsed)}</dd></div>
            )}
            <div className={row}><dt className="text-fg3">Estimated delivery</dt><dd>{fullDate(confirmation.estimatedDeliveryDate)}</dd></div>
            <div className={row}><dt className="text-fg3">Payment reference</dt><dd className="font-mono text-xs">{confirmation.paymentReference}</dd></div>
            {confirmation.pointsRedeemed > 0 && (
              <div className={row}><dt className="text-fg3">Points redeemed</dt><dd>−{confirmation.pointsRedeemed}</dd></div>
            )}
            <div className={row}><dt className="text-fg3">Points earned</dt><dd>+{confirmation.pointsEarned}</dd></div>
            <div className={`${row} pt-2 border-t border-line`}>
              <dt className="text-fg3">Points balance</dt>
              <dd className="font-semibold text-fg">{confirmation.newGiftPointsBalance}</dd>
            </div>
          </dl>

          <p className="text-xs text-fg3">
            You can cancel this order from My Orders within 48 hours for a full refund and your points restored.
          </p>

          <div className="flex flex-col-reverse sm:flex-row gap-2">
            <button onClick={() => { onClose(); onViewHistory(); }} className="btn-secondary flex-1">
              View my orders
            </button>
            <button onClick={onClose} className="btn-primary flex-1">
              Continue your Shopping
            </button>
          </div>
        </div>
      )}
    </Modal>
  );
}
