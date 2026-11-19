import React, { useEffect, useState } from 'react';
import { Trash2 } from 'lucide-react';
import Modal, { ModalHeader } from './Modal';
import BookCover from './BookCover';
import SuggestionRail from './SuggestionRail';
import { api } from '../api/client';
import { useCart } from '../context/CartContext';
import { inr } from '../lib/format';

export default function CartDrawer({ onProceedToCheckout, onOpenDetail }) {
  const { cart, isCartOpen, setIsCartOpen, updateQuantity, removeItem, clearCart } = useCart();
  const [more, setMore] = useState([]);

  // Books that go with what is in the basket. A failure here must never get in the way of checking out.
  const basketKey = cart.items.map((i) => i.book.id).join(',');
  useEffect(() => {
    if (!isCartOpen || !basketKey) { setMore([]); return undefined; }
    let cancelled = false;
    api.getBasketSuggestions(basketKey.split(','), 3)
      .then((r) => { if (!cancelled) setMore(r?.crossSell || []); })
      .catch(() => { if (!cancelled) setMore([]); });
    return () => { cancelled = true; };
  }, [isCartOpen, basketKey]);

  const close = () => setIsCartOpen(false);
  const threshold = Number(cart.freeShippingThreshold) || 499;
  const remaining = threshold - cart.subtotal;

  return (
    <Modal open={isCartOpen} onClose={close} title="Basket" placement="right">
      <ModalHeader title={`Basket (${cart.itemCount})`} onClose={close} />

      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-4">
        {cart.items.length === 0 ? (
          <div className="text-center py-16">
            <p className="text-base text-fg">Your basket is empty</p>
            <p className="text-sm text-fg3 mt-1">Add a few books from the catalogue.</p>
          </div>
        ) : (
          cart.items.map((item) => (
            <div key={item.id} className="flex gap-3 pb-4 border-b border-line">
              <div className="w-14 h-20 shrink-0"><BookCover book={item.book} compact /></div>
              <div className="flex-1 min-w-0 flex flex-col justify-between">
                <div>
                  <h3 className="text-sm font-medium text-fg line-clamp-2 leading-snug">{item.book.title}</h3>
                  <p className="text-xs text-fg3 mt-0.5 truncate">{item.book.author}</p>
                </div>

                <div className="flex items-center justify-between mt-2 gap-2">
                  <div className="flex items-center bg-layer2" role="group" aria-label={`Quantity of ${item.book.title}`}>
                    <button
                      onClick={() => (item.quantity > 1 ? updateQuantity(item.id, item.quantity - 1) : removeItem(item.id))}
                      aria-label="Decrease quantity"
                      className="size-9 hover:bg-[#4c4c4c]"
                    >
                      −
                    </button>
                    <span className="w-7 text-center text-sm">{item.quantity}</span>
                    <button
                      onClick={() => updateQuantity(item.id, item.quantity + 1)}
                      aria-label="Increase quantity"
                      className="size-9 hover:bg-[#4c4c4c]"
                    >
                      +
                    </button>
                  </div>

                  <span className="text-sm font-medium text-fg">{inr(item.unitPrice * item.quantity)}</span>

                  <button
                    onClick={() => removeItem(item.id)}
                    aria-label={`Remove ${item.book.title}`}
                    className="p-2 text-fg3 hover:text-fg hover:bg-layer2"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          ))
        )}

        {cart.items.length > 0 && (
          <SuggestionRail
            title="You might also like"
            hint="Picked to go with the books in your basket"
            books={more}
            onSelect={(b) => { close(); onOpenDetail?.(b); }}
          />
        )}
      </div>

      {cart.items.length > 0 && (
        <div className="px-5 py-4 pb-[max(1rem,env(safe-area-inset-bottom))] border-t border-line bg-canvas space-y-3 shrink-0">
          <p className="text-xs text-fg2">
            {remaining <= 0
              ? 'Free delivery applied.'
              : `Add ${inr(remaining)} more for free delivery.`}
          </p>

          <dl className="space-y-1 text-sm text-fg2">
            <div className="flex justify-between"><dt>Subtotal</dt><dd>{inr(cart.subtotal)}</dd></div>
            <div className="flex justify-between"><dt>Tax (12%)</dt><dd>{inr(cart.estimatedTax)}</dd></div>
            <div className="flex justify-between">
              <dt>Delivery</dt>
              <dd>{cart.shippingFee === 0 ? 'Free' : inr(cart.shippingFee)}</dd>
            </div>
            <div className="flex justify-between font-semibold text-fg pt-2 border-t border-line">
              <dt>Total</dt><dd>{inr(cart.totalAmount)}</dd>
            </div>
          </dl>

          <button
            onClick={() => { close(); onProceedToCheckout(); }}
            className="btn-primary w-full"
          >
            Checkout
          </button>
          <button onClick={clearCart} className="text-xs text-fg3 hover:text-fg underline">
            Empty basket
          </button>
        </div>
      )}
    </Modal>
  );
}
