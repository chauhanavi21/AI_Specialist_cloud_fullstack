import React, { useState, useEffect, useMemo } from 'react';
import { Check, Tag } from 'lucide-react';
import Modal, { ModalHeader } from './Modal';
import BookCover from './BookCover';
import AddressForm from './AddressForm';
import PaymentPanel, { EMPTY_PAYMENT, validatePayment } from './PaymentPanel';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useToast } from '../context/ToastContext';
import { deliveryBy, inr } from '../lib/format';

export default function CheckoutModal({ isOpen, onClose, onOrderSuccess, onOpenWallet }) {
  const { user, refreshUser } = useAuth();
  const { cart, refreshCart, updateQuantity } = useCart();
  const toast = useToast();

  const [addresses, setAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState(null);
  const [showNewAddress, setShowNewAddress] = useState(false);

  const [method, setMethod] = useState('CREDIT_CARD');
  const [payment, setPayment] = useState(EMPTY_PAYMENT);
  const [paymentErrors, setPaymentErrors] = useState({});

  const [couponInput, setCouponInput] = useState('');
  const [appliedCoupon, setAppliedCoupon] = useState('');
  const [couponError, setCouponError] = useState('');
  const [points, setPoints] = useState(0);

  const [quote, setQuote] = useState(null);
  const [quoteError, setQuoteError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');

  // Reset the whole form each time checkout opens.
  useEffect(() => {
    if (!isOpen || !user) return;
    setSubmitError('');
    setPaymentErrors({});
    setPayment(EMPTY_PAYMENT);
    setMethod('CREDIT_CARD');
    setPoints(0);
    setCouponInput('');
    setAppliedCoupon('');
    setCouponError('');

    api.getAddresses()
      .then((data) => {
        setAddresses(data || []);
        if (data && data.length > 0) {
          setSelectedAddressId((data.find((a) => a.isDefault) || data[0]).id);
          setShowNewAddress(false);
        } else {
          setShowNewAddress(true);
        }
      })
      .catch((err) => setSubmitError(err.message || 'Could not load your addresses'));
  }, [isOpen, user?.id]);

  // The server prices the order; the screen only displays what it returns.
  useEffect(() => {
    if (!isOpen || !user || cart.items.length === 0) return;
    let cancelled = false;
    api.quoteOrder({ couponCode: appliedCoupon || null, pointsToRedeem: points })
      .then((q) => { if (!cancelled) { setQuote(q); setQuoteError(''); } })
      .catch((err) => { if (!cancelled) setQuoteError(err.message || 'Could not price your order'); });
    return () => { cancelled = true; };
  }, [isOpen, user?.id, cart, appliedCoupon, points]);

  const walletBalance = quote?.walletBalance ?? user?.walletBalance ?? 0;
  const payable = quote?.payable ?? cart.totalAmount;
  const maxPoints = quote?.maxPoints ?? 0;

  const applyCoupon = async (e) => {
    e?.preventDefault();
    const code = couponInput.trim();
    if (!code) return setCouponError('Enter a coupon code');
    try {
      const q = await api.quoteOrder({ couponCode: code, pointsToRedeem: points });
      setQuote(q);
      setAppliedCoupon(q.couponCode);
      setCouponInput(q.couponCode);
      setCouponError('');
      toast.success(`Coupon ${q.couponCode} applied`);
    } catch (err) {
      setAppliedCoupon('');
      setCouponError(err.message || 'That coupon cannot be used');
    }
    return undefined;
  };

  const removeCoupon = () => {
    setAppliedCoupon('');
    setCouponInput('');
    setCouponError('');
  };

  const placeOrder = async () => {
    setSubmitError('');
    if (!selectedAddressId) {
      return setSubmitError('Select or add a delivery address');
    }
    const errors = payable > 0 ? validatePayment(method, payment, walletBalance, payable) : {};
    setPaymentErrors(errors);
    if (Object.keys(errors).length > 0) {
      setSubmitError('Check your payment details');
      // Jump to the first field that needs fixing so the error is never off screen.
      const first = Object.keys(errors)[0];
      requestAnimationFrame(() => {
        const el = document.getElementById(`pay-${first}`) || document.getElementById('payment-fields');
        el?.scrollIntoView({ block: 'center', behavior: 'smooth' });
        el?.focus?.({ preventScroll: true });
      });
      return undefined;
    }

    setSubmitting(true);
    try {
      const confirmation = await api.placeOrder({
        addressId: selectedAddressId,
        paymentMethod: method,
        ...(method === 'CREDIT_CARD' || method === 'DEBIT_CARD'
          ? { cardNumber: payment.cardNumber, cardName: payment.cardName, cardExpiry: payment.cardExpiry, cardCvv: payment.cardCvv }
          : {}),
        ...(method === 'UPI' ? { upiId: payment.upiId } : {}),
        couponCode: appliedCoupon || null,
        pointsToRedeem: points,
      });
      setPayment(EMPTY_PAYMENT); // never keep card details in memory after use
      await Promise.all([refreshCart(), refreshUser()]);
      onClose();
      onOrderSuccess(confirmation);
    } catch (err) {
      setSubmitError(err.message || 'Payment failed. Please check your details and try again.');
      await refreshCart();
    } finally {
      setSubmitting(false);
    }
    return undefined;
  };

  const emptyBasket = cart.items.length === 0;
  const payLabel = useMemo(() => (quote ? `Pay ${inr(quote.payable)}` : 'Pay now'), [quote]);

  return (
    <Modal open={isOpen} onClose={onClose} title="Checkout" size="xl">
      <ModalHeader title="Shopping Cart" subtitle="Review your books, delivery address and payment" onClose={onClose} />

      {emptyBasket ? (
        <div className="p-12 text-center">
          <p className="text-fg">Your basket is empty.</p>
          <button onClick={onClose} className="btn-primary mt-4">Back to the store</button>
        </div>
      ) : (
        <div className="overflow-y-auto">
          <div className="p-5 sm:p-6 grid grid-cols-1 lg:grid-cols-[1fr_20rem] gap-6">
            <div className="space-y-6 min-w-0">
              <section aria-label="Items">
                <ul className="divide-y divide-line border border-line">
                  {cart.items.map((item) => (
                    <li key={item.id} className="flex gap-3 p-3 bg-canvas">
                      <div className="w-14 shrink-0 aspect-[3/4]"><BookCover book={item.book} compact /></div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-semibold text-fg line-clamp-1">{item.book.title}</p>
                        <p className="text-xs text-link">by {item.book.author}</p>
                        <p className="text-[11px] text-fg3 mt-0.5">{item.book.format} · {deliveryBy(item.book)}</p>
                        <div className="flex items-center gap-3 mt-2">
                          <div className="flex items-center bg-layer2" role="group" aria-label={`Quantity of ${item.book.title}`}>
                            <button
                              onClick={() => item.quantity > 1 && updateQuantity(item.id, item.quantity - 1)}
                              disabled={item.quantity <= 1}
                              aria-label="Decrease quantity"
                              className="size-8 hover:bg-[#4c4c4c] disabled:opacity-40"
                            >−</button>
                            <span className="w-7 text-center text-sm">{item.quantity}</span>
                            <button
                              onClick={() => updateQuantity(item.id, item.quantity + 1)}
                              aria-label="Increase quantity"
                              className="size-8 hover:bg-[#4c4c4c]"
                            >+</button>
                          </div>
                          <span className="text-sm font-semibold text-fg">{inr(item.unitPrice * item.quantity)}</span>
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              </section>

              <section aria-label="Delivery address">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-sm font-semibold">Address</h3>
                  {!showNewAddress && (
                    <button onClick={() => setShowNewAddress(true)} className="text-sm text-link hover:underline">
                      Add new address
                    </button>
                  )}
                </div>

                {showNewAddress ? (
                  <div className="border border-line p-4 bg-canvas">
                    <AddressForm
                      defaults={{ recipientName: user?.fullName || '', email: user?.email || '', phoneNumber: user?.phoneNumber || '' }}
                      canCancel={addresses.length > 0}
                      onCancel={() => setShowNewAddress(false)}
                      onSaved={(created) => {
                        setAddresses((list) => [...list, created]);
                        setSelectedAddressId(created.id);
                        setShowNewAddress(false);
                        setSubmitError('');
                      }}
                    />
                  </div>
                ) : (
                  <div role="radiogroup" aria-label="Saved addresses" className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {addresses.map((addr) => {
                      const selected = selectedAddressId === addr.id;
                      return (
                        <button
                          key={addr.id}
                          role="radio"
                          aria-checked={selected}
                          onClick={() => setSelectedAddressId(addr.id)}
                          className={`text-left p-3 text-sm border ${
                            selected ? 'border-brand bg-layer2' : 'border-line bg-canvas hover:border-line2'
                          }`}
                        >
                          <span className="flex items-center justify-between mb-1">
                            <span className="font-medium text-fg">{addr.recipientName}</span>
                            {selected && <Check className="w-4 h-4 text-link" aria-hidden="true" />}
                          </span>
                          <span className="block text-fg2">{addr.streetAddress}{addr.addressLine2 ? `, ${addr.addressLine2}` : ''}</span>
                          <span className="block text-fg3">{addr.city}, {addr.state} {addr.postalCode}</span>
                          <span className="block text-fg3">{addr.country} · {addr.phoneNumber}</span>
                          {addr.isDefault && <span className="text-[11px] text-link">Default</span>}
                        </button>
                      );
                    })}
                  </div>
                )}
              </section>

              <section aria-label="Payment">
                <h3 className="text-sm font-semibold mb-2">Complete Payment</h3>
                {payable <= 0 ? (
                  <p className="text-sm text-fg2 border border-line p-4 bg-canvas">
                    Your gift points cover this whole order. No payment is needed.
                  </p>
                ) : (
                  <PaymentPanel
                    method={method}
                    onMethodChange={(m) => { setMethod(m); setPaymentErrors({}); setSubmitError(''); }}
                    values={payment}
                    onChange={setPayment}
                    errors={paymentErrors}
                    walletBalance={walletBalance}
                    payable={payable}
                    onTopUp={onOpenWallet}
                  />
                )}
              </section>
            </div>

            <aside className="lg:sticky lg:top-0 self-start bg-canvas border border-line p-4 space-y-4" aria-label="Order summary">
              <h3 className="text-sm font-semibold">Grand Total</h3>

              <form onSubmit={applyCoupon} className="space-y-1">
                <label htmlFor="coupon" className="field-label">Coupon code</label>
                {appliedCoupon ? (
                  <div className="flex items-center justify-between bg-layer2 px-3 py-2 text-sm">
                    <span className="flex items-center gap-2 text-ok"><Tag className="w-4 h-4" aria-hidden="true" />{appliedCoupon}</span>
                    <button type="button" onClick={removeCoupon} className="text-xs text-link hover:underline">Remove</button>
                  </div>
                ) : (
                  <div className="flex">
                    <input
                      id="coupon"
                      value={couponInput}
                      onChange={(e) => { setCouponInput(e.target.value.toUpperCase()); setCouponError(''); }}
                      placeholder="Apply Coupon"
                      aria-invalid={!!couponError}
                      aria-describedby={couponError ? 'coupon-err' : undefined}
                      className="field flex-1 min-w-0"
                    />
                    <button type="submit" className="btn-primary">Apply</button>
                  </div>
                )}
                {couponError && <p id="coupon-err" className="field-error" role="alert">{couponError}</p>}
                {!appliedCoupon && !couponError && <p className="text-[11px] text-fg3">Try WELCOME10 or BOOKS100.</p>}
              </form>

              {maxPoints > 0 && (
                <div>
                  <label htmlFor="points" className="field-label flex justify-between">
                    <span>Gift points</span>
                    <span>{points} of {maxPoints} (1 point = ₹1)</span>
                  </label>
                  <input
                    id="points"
                    type="range"
                    min="0"
                    max={maxPoints}
                    step="1"
                    value={points}
                    onChange={(e) => setPoints(Number(e.target.value))}
                    className="w-full accent-[#0f62fe]"
                  />
                  <button type="button" onClick={() => setPoints(maxPoints)} className="text-xs text-link hover:underline">
                    Use maximum
                  </button>
                </div>
              )}

              {quote ? (
                <dl className="space-y-1.5 text-sm">
                  <div className="flex justify-between text-fg2"><dt>Price ({cart.itemCount} items)</dt><dd>{inr(quote.subtotal)}</dd></div>
                  {quote.couponDiscount > 0 && (
                    <div className="flex justify-between text-ok"><dt>Coupon</dt><dd>−{inr(quote.couponDiscount)}</dd></div>
                  )}
                  <div className="flex justify-between text-fg2"><dt>Tax (12%)</dt><dd>{inr(quote.tax)}</dd></div>
                  <div className="flex justify-between text-fg2">
                    <dt>Delivery Charges</dt><dd>{quote.shippingFee === 0 ? 'Free' : inr(quote.shippingFee)}</dd>
                  </div>
                  {quote.pointsDiscount > 0 && (
                    <div className="flex justify-between text-ok"><dt>Gift points</dt><dd>−{inr(quote.pointsDiscount)}</dd></div>
                  )}
                  <div className="flex justify-between font-semibold text-fg pt-2 border-t border-line text-base">
                    <dt>Total Amount</dt><dd>{inr(quote.payable)}</dd>
                  </div>
                  {quote.pointsToEarn > 0 && (
                    <p className="text-[11px] text-fg3">You will earn {quote.pointsToEarn} gift points.</p>
                  )}
                </dl>
              ) : (
                <p className="text-xs text-fg3" role="status">{quoteError || 'Calculating your total...'}</p>
              )}

              {submitError && (
                <p className="text-sm text-danger border-l-2 border-danger pl-3" role="alert">{submitError}</p>
              )}

              <button onClick={placeOrder} disabled={submitting || !quote} className="btn-primary w-full">
                {submitting ? 'Processing...' : payLabel}
              </button>
            </aside>
          </div>
        </div>
      )}
    </Modal>
  );
}
