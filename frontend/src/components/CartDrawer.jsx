import React from 'react';
import { X, ShoppingBag, Trash2, ArrowRight, Truck } from 'lucide-react';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

export default function CartDrawer({ onProceedToCheckout }) {
  const { cart, isCartOpen, setIsCartOpen, updateQuantity, removeItem, clearCart } = useCart();
  const { user } = useAuth();

  if (!isCartOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-black/50 backdrop-blur-xs animate-fadeIn">
      <div className="absolute inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-white shadow-2xl flex flex-col">
          {/* Header */}
          <div className="p-6 border-b border-gray-200 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ShoppingBag className="w-5 h-5 text-ibm-blue" />
              <h2 className="text-lg font-bold text-gray-900">Your Basket ({cart.itemCount})</h2>
            </div>
            <button
              onClick={() => setIsCartOpen(false)}
              className="p-2 text-gray-400 hover:text-gray-700 rounded-full hover:bg-gray-100 transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Cart Item List */}
          <div className="flex-1 overflow-y-auto p-6 space-y-4">
            {cart.items.length === 0 ? (
              <div className="text-center py-16">
                <ShoppingBag className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                <p className="text-base font-semibold text-gray-800">Your basket is empty</p>
                <p className="text-xs text-gray-500 mt-1">Explore our catalog and find your next tech read!</p>
              </div>
            ) : (
              cart.items.map((item) => (
                <div key={item.id} className="flex gap-4 p-3 bg-gray-50 rounded-2xl border border-gray-100">
                  <img
                    src={item.book.coverImageUrl}
                    alt={item.book.title}
                    className="w-16 h-22 object-cover rounded-xl shadow-xs shrink-0"
                  />
                  <div className="flex-1 flex flex-col justify-between">
                    <div>
                      <h4 className="text-xs font-bold text-gray-900 line-clamp-2 leading-tight">
                        {item.book.title}
                      </h4>
                      <p className="text-[11px] text-gray-500 mt-0.5">{item.book.author}</p>
                      <div className="text-xs font-extrabold text-ibm-blue mt-1">
                        ${item.unitPrice.toFixed(2)}
                      </div>
                    </div>

                    <div className="flex items-center justify-between mt-2 pt-2 border-t border-gray-200/60">
                      {/* Quantity Stepper */}
                      <div className="flex items-center border border-gray-300 rounded-lg bg-white">
                        <button
                          onClick={() => {
                            if (item.quantity > 1) {
                              updateQuantity(item.id, item.quantity - 1);
                            } else {
                              removeItem(item.id);
                            }
                          }}
                          className="w-6 h-6 flex items-center justify-center text-xs font-bold text-gray-600 hover:bg-gray-100 rounded-l"
                        >
                          -
                        </button>
                        <span className="w-6 text-center text-xs font-bold text-gray-800">
                          {item.quantity}
                        </span>
                        <button
                          onClick={() => updateQuantity(item.id, item.quantity + 1)}
                          className="w-6 h-6 flex items-center justify-center text-xs font-bold text-gray-600 hover:bg-gray-100 rounded-r"
                        >
                          +
                        </button>
                      </div>

                      <button
                        onClick={() => removeItem(item.id)}
                        className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
                        title="Remove item"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Cart Footer Summary */}
          {cart.items.length > 0 && (
            <div className="p-6 border-t border-gray-200 bg-gray-50 space-y-4">
              {/* Shipping incentive */}
              <div className="flex items-center gap-2 text-xs font-semibold text-emerald-700 bg-emerald-50 px-3 py-2 rounded-xl border border-emerald-200">
                <Truck className="w-4 h-4" />
                <span>
                  {cart.subtotal >= 50
                    ? 'You qualify for Free Standard Delivery!'
                    : `Add $${(50 - cart.subtotal).toFixed(2)} more for Free Shipping`}
                </span>
              </div>

              <div className="space-y-1.5 text-xs text-gray-600">
                <div className="flex justify-between">
                  <span>Subtotal</span>
                  <span className="font-semibold text-gray-900">${cart.subtotal.toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Estimated Tax (8%)</span>
                  <span>${cart.estimatedTax.toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Shipping Fee</span>
                  <span>{cart.shippingFee === 0 ? 'FREE' : `$${cart.shippingFee.toFixed(2)}`}</span>
                </div>
                <div className="flex justify-between text-sm font-extrabold text-gray-900 pt-2 border-t border-gray-200">
                  <span>Total Amount</span>
                  <span className="text-ibm-blue">${cart.totalAmount.toFixed(2)}</span>
                </div>
              </div>

              <button
                onClick={() => {
                  setIsCartOpen(false);
                  onProceedToCheckout();
                }}
                className="w-full py-3.5 px-4 bg-ibm-blue hover:bg-ibm-darkBlue text-white font-bold rounded-xl flex items-center justify-center gap-2 shadow-lg transition active:scale-98"
              >
                <span>Proceed to Checkout</span>
                <ArrowRight className="w-4 h-4" />
              </button>

              <div className="flex justify-between items-center pt-1 text-[11px] text-gray-400">
                <button
                  onClick={clearCart}
                  className="hover:text-red-500 underline"
                >
                  Empty basket
                </button>
                <span>48-Hour Free Cancellation Guaranteed</span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
