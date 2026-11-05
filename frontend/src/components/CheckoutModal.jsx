import React, { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

const inputClass =
  'w-full min-w-0 px-3 py-2 text-base sm:text-sm bg-white border border-brand-200 rounded-md focus:outline-none focus:border-brand-500';

export default function CheckoutModal({ isOpen, onClose, onOrderSuccess }) {
  const { user, refreshUser } = useAuth();
  const { cart, refreshCart } = useCart();

  const [addresses, setAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState(null);
  const [showNewAddressForm, setShowNewAddressForm] = useState(false);
  const [newAddress, setNewAddress] = useState({
    recipientName: '',
    streetAddress: '',
    city: '',
    state: '',
    postalCode: '',
    country: 'United States',
    isDefault: false,
  });

  const [paymentMethod, setPaymentMethod] = useState('CREDIT_CARD');
  const [cardDetails] = useState({
    cardNumber: '4242 •••• •••• 4242',
    expiry: '12/28',
    cvv: '123',
    cardLast4: '4242',
  });

  // Gift Points Redemption Engine
  const [pointsToRedeem, setPointsToRedeem] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    if (!isOpen || !user) return;
    setErrorMsg('');
    setPointsToRedeem(0);

    api.getAddresses()
      .then((data) => {
        setAddresses(data || []);
        if (data && data.length > 0) {
          const defaultAddr = data.find(a => a.isDefault) || data[0];
          setSelectedAddressId(defaultAddr.id);
        } else {
          setShowNewAddressForm(true);
        }
      })
      .catch((err) => console.error('Failed to load addresses', err));
  }, [isOpen, user]);

  if (!isOpen) return null;

  // Max points redeemable cannot exceed total cart value
  const pointsDiscount = (pointsToRedeem * 0.10);
  const finalTotal = Math.max(0, (cart.totalAmount - pointsDiscount));
  const maxPossiblePoints = Math.min(
    user?.giftPointsBalance || 0,
    Math.floor(cart.totalAmount / 0.10)
  );

  const handleCreateAddress = async (e) => {
    e.preventDefault();
    try {
      const created = await api.addAddress(newAddress);
      setAddresses([...addresses, created]);
      setSelectedAddressId(created.id);
      setShowNewAddressForm(false);
      setNewAddress({
        recipientName: '',
        streetAddress: '',
        city: '',
        state: '',
        postalCode: '',
        country: 'United States',
        isDefault: false,
      });
    } catch (err) {
      setErrorMsg(err.message || 'Failed to save address');
    }
  };

  const handlePlaceOrder = async () => {
    if (!selectedAddressId) {
      setErrorMsg('Please select or add a delivery address');
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorMsg('');

      const payload = {
        addressId: selectedAddressId,
        paymentMethod,
        cardLast4: cardDetails.cardLast4,
        pointsToRedeem: Number(pointsToRedeem),
      };

      const confirmation = await api.placeOrder(payload);
      await refreshCart();
      await refreshUser();
      onClose();
      onOrderSuccess(confirmation);
    } catch (err) {
      setErrorMsg(err.message || 'Order payment failed. Please check details.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const updateAddress = (field) => (e) =>
    setNewAddress({ ...newAddress, [field]: e.target.value });

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/40 flex items-end sm:items-center justify-center p-0 sm:p-4 overflow-y-auto">
      <div className="relative bg-white rounded-t-xl sm:rounded-lg max-w-xl w-full overflow-hidden sm:my-8 max-h-[95dvh] sm:max-h-[92dvh] flex flex-col">
        <div className="px-4 sm:px-6 py-4 border-b border-brand-100 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">Checkout</h2>
          <button
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-700 rounded-md hover:bg-brand-50"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {errorMsg && (
          <div className="mx-4 sm:mx-6 mt-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-md">
            {errorMsg}
          </div>
        )}

        <div className="overflow-y-auto p-4 sm:p-6 space-y-6 sm:space-y-7">
          {/* Delivery address */}
          <section className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold text-slate-900">Delivery address</h3>
              {!showNewAddressForm && (
                <button
                  onClick={() => setShowNewAddressForm(true)}
                  className="text-sm text-brand-700 hover:underline"
                >
                  Add new
                </button>
              )}
            </div>

            {showNewAddressForm ? (
              <form onSubmit={handleCreateAddress} className="border border-brand-100 rounded-lg p-4 space-y-3">
                <div className="grid grid-cols-1 min-[420px]:grid-cols-2 gap-2">
                  <input
                    type="text"
                    required
                    placeholder="Recipient name"
                    value={newAddress.recipientName}
                    onChange={updateAddress('recipientName')}
                    className={inputClass}
                  />
                  <input
                    type="text"
                    required
                    placeholder="Street address"
                    value={newAddress.streetAddress}
                    onChange={updateAddress('streetAddress')}
                    className={inputClass}
                  />
                </div>
                <div className="grid grid-cols-1 min-[420px]:grid-cols-3 gap-2">
                  <input
                    type="text"
                    required
                    placeholder="City"
                    value={newAddress.city}
                    onChange={updateAddress('city')}
                    className={inputClass}
                  />
                  <input
                    type="text"
                    required
                    placeholder="State"
                    value={newAddress.state}
                    onChange={updateAddress('state')}
                    className={inputClass}
                  />
                  <input
                    type="text"
                    required
                    placeholder="Postal code"
                    value={newAddress.postalCode}
                    onChange={updateAddress('postalCode')}
                    className={inputClass}
                  />
                </div>
                <div className="flex items-center justify-end gap-2">
                  {addresses.length > 0 && (
                    <button
                      type="button"
                      onClick={() => setShowNewAddressForm(false)}
                      className="px-3 py-1.5 text-sm text-slate-600 hover:bg-brand-50 rounded-md"
                    >
                      Cancel
                    </button>
                  )}
                  <button
                    type="submit"
                    className="px-4 py-1.5 bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium rounded-md"
                  >
                    Save address
                  </button>
                </div>
              </form>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {addresses.map((addr) => {
                  const isSelected = selectedAddressId === addr.id;
                  return (
                    <div
                      key={addr.id}
                      onClick={() => setSelectedAddressId(addr.id)}
                      className={`p-3 rounded-lg border cursor-pointer text-sm ${
                        isSelected
                          ? 'border-brand-500 bg-brand-50'
                          : 'border-brand-100 hover:border-brand-300 bg-white'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className="font-medium text-slate-900">{addr.recipientName}</span>
                        {addr.isDefault && (
                          <span className="text-xs text-slate-500">Default</span>
                        )}
                      </div>
                      <p className="text-slate-600">{addr.streetAddress}</p>
                      <p className="text-slate-500">{addr.city}, {addr.state} {addr.postalCode}</p>
                    </div>
                  );
                })}
              </div>
            )}
          </section>

          {/* Gift points */}
          <section className="space-y-2">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold text-slate-900">Gift points</h3>
              <span className="text-sm text-slate-500">Balance: {user?.giftPointsBalance || 0}</span>
            </div>

            {maxPossiblePoints > 0 ? (
              <div className="border border-brand-100 rounded-lg p-4 space-y-2">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-slate-600">
                    Redeem <strong className="text-slate-900">{pointsToRedeem}</strong> points
                    <span className="text-slate-400"> (10 points = $1.00)</span>
                  </span>
                  <span className="text-slate-900">-${pointsDiscount.toFixed(2)}</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max={maxPossiblePoints}
                  step="10"
                  value={pointsToRedeem}
                  onChange={(e) => setPointsToRedeem(Number(e.target.value))}
                  className="w-full accent-brand-600 cursor-pointer"
                />
                <div className="flex justify-between text-xs text-slate-400">
                  <span>0</span>
                  <button
                    type="button"
                    onClick={() => setPointsToRedeem(maxPossiblePoints)}
                    className="text-brand-700 hover:underline"
                  >
                    Use max ({maxPossiblePoints})
                  </button>
                </div>
              </div>
            ) : (
              <p className="text-sm text-slate-500">No points available to redeem.</p>
            )}
          </section>

          {/* Payment */}
          <section className="space-y-3">
            <h3 className="text-sm font-semibold text-slate-900">Payment method</h3>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {[
                { id: 'CREDIT_CARD', label: 'Credit card' },
                { id: 'DEBIT_CARD', label: 'Debit card' },
                { id: 'UPI', label: 'UPI' },
                { id: 'NET_BANKING', label: 'Net banking' },
              ].map((opt) => (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => setPaymentMethod(opt.id)}
                  className={`py-2.5 rounded-md border text-sm ${
                    paymentMethod === opt.id
                      ? 'border-brand-500 bg-brand-50 text-brand-800 font-medium'
                      : 'border-brand-100 text-slate-700 hover:bg-brand-50'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>

            {(paymentMethod === 'CREDIT_CARD' || paymentMethod === 'DEBIT_CARD') && (
              <div className="border border-brand-100 rounded-lg p-4 space-y-2 text-sm">
                <div>
                  <label className="block text-xs text-slate-500 mb-1">Card number (test card)</label>
                  <input
                    type="text"
                    disabled
                    value={cardDetails.cardNumber}
                    className="w-full px-3 py-2 bg-brand-50 border border-brand-100 rounded-md text-slate-700 font-mono"
                  />
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-xs text-slate-500 mb-1">Expiry</label>
                    <input
                      type="text"
                      disabled
                      value={cardDetails.expiry}
                      className="w-full px-3 py-2 bg-brand-50 border border-brand-100 rounded-md text-slate-700 font-mono"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-slate-500 mb-1">CVV</label>
                    <input
                      type="text"
                      disabled
                      value={cardDetails.cvv}
                      className="w-full px-3 py-2 bg-brand-50 border border-brand-100 rounded-md text-slate-700 font-mono"
                    />
                  </div>
                </div>
              </div>
            )}
          </section>
        </div>

        <div className="px-4 sm:px-6 py-4 border-t border-brand-100 bg-brand-50 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="w-full sm:w-auto">
            <div className="text-xs text-slate-500">
              Subtotal ${cart.subtotal.toFixed(2)}
              {pointsDiscount > 0 && ` · Points -$${pointsDiscount.toFixed(2)}`}
            </div>
            <div className="text-lg font-semibold text-slate-900">
              Total ${finalTotal.toFixed(2)}
            </div>
          </div>

          <button
            onClick={handlePlaceOrder}
            disabled={isSubmitting || !selectedAddressId}
            className="w-full sm:w-auto px-8 py-2.5 bg-brand-600 hover:bg-brand-700 disabled:bg-slate-300 text-white text-sm font-medium rounded-md"
          >
            {isSubmitting ? 'Processing...' : `Pay $${finalTotal.toFixed(2)}`}
          </button>
        </div>
      </div>
    </div>
  );
}
