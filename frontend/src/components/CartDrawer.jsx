import React from 'react';
import { X, Trash2 } from 'lucide-react';
import { useCart } from '../context/CartContext';

export default function CartDrawer({ onProceedToCheckout }) {
  const { cart, isCartOpen, setIsCartOpen, updateQuantity, removeItem, clearCart } = useCart();

  if (!isCartOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-slate-900/40">
      <div className="absolute inset-y-0 right-0 max-w-full flex pl-6 sm:pl-10">
        <div className="w-screen max-w-md h-dvh bg-white border-l border-brand-100 flex flex-col">
          <div className="px-4 sm:px-5 py-4 border-b border-brand-100 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-900">Basket ({cart.itemCount})</h2>
            <button
              onClick={() => setIsCartOpen(false)}
              className="p-2 text-slate-400 hover:text-slate-700 rounded-md hover:bg-brand-50"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto px-4 sm:px-5 py-4 space-y-3">
            {cart.items.length === 0 ? (
              <div className="text-center py-16">
                <p className="text-base text-slate-800">Your basket is empty</p>
                <p className="text-sm text-slate-500 mt-1">Add a few books from the catalog.</p>
              </div>
            ) : (
              cart.items.map((item) => (
                <div key={item.id} className="flex gap-3 pb-3 border-b border-brand-100">
                  <img
                    src={item.book.coverImageUrl}
                    alt={item.book.title}
                    className="w-14 h-20 object-cover rounded shrink-0"
                  />
                  <div className="flex-1 flex flex-col justify-between">
                    <div>
                      <h4 className="text-sm font-medium text-slate-900 line-clamp-2 leading-snug">
                        {item.book.title}
                      </h4>
                      <p className="text-xs text-slate-500 mt-0.5">{item.book.author}</p>
                    </div>

                    <div className="flex items-center justify-between mt-2">
                      <div className="flex items-center border border-brand-200 rounded-md">
                        <button
                          onClick={() => {
                            if (item.quantity > 1) {
                              updateQuantity(item.id, item.quantity - 1);
                            } else {
                              removeItem(item.id);
                            }
                          }}
                          className="size-8 sm:size-7 flex items-center justify-center text-slate-600 hover:bg-brand-50"
                        >
                          -
                        </button>
                        <span className="w-7 text-center text-sm text-slate-800">{item.quantity}</span>
                        <button
                          onClick={() => updateQuantity(item.id, item.quantity + 1)}
                          className="size-8 sm:size-7 flex items-center justify-center text-slate-600 hover:bg-brand-50"
                        >
                          +
                        </button>
                      </div>

                      <span className="text-sm font-medium text-slate-900">
                        ${item.unitPrice.toFixed(2)}
                      </span>

                      <button
                        onClick={() => removeItem(item.id)}
                        className="p-1.5 text-slate-400 hover:text-slate-700 rounded-md hover:bg-brand-50"
                        title="Remove"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {cart.items.length > 0 && (
            <div className="px-4 sm:px-5 py-4 pb-[max(1rem,env(safe-area-inset-bottom))] border-t border-brand-100 bg-brand-50 space-y-3">
              <p className="text-xs text-slate-600">
                {cart.subtotal >= 50
                  ? 'Free standard delivery applied.'
                  : `Add $${(50 - cart.subtotal).toFixed(2)} more for free shipping.`}
              </p>

              <div className="space-y-1 text-sm text-slate-600">
                <div className="flex justify-between">
                  <span>Subtotal</span>
                  <span>${cart.subtotal.toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Tax (8%)</span>
                  <span>${cart.estimatedTax.toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Shipping</span>
                  <span>{cart.shippingFee === 0 ? 'Free' : `$${cart.shippingFee.toFixed(2)}`}</span>
                </div>
                <div className="flex justify-between font-semibold text-slate-900 pt-2 border-t border-brand-200">
                  <span>Total</span>
                  <span>${cart.totalAmount.toFixed(2)}</span>
                </div>
              </div>

              <button
                onClick={() => {
                  setIsCartOpen(false);
                  onProceedToCheckout();
                }}
                className="w-full py-2.5 bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium rounded-md"
              >
                Checkout
              </button>

              <button
                onClick={clearCart}
                className="text-xs text-slate-500 hover:text-slate-800 underline"
              >
                Empty basket
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
