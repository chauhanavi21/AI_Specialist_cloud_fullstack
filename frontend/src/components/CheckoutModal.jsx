import React, { useState, useEffect } from 'react';
import { X, MapPin, CreditCard, Gift, ShieldCheck, CheckCircle2, Plus, Sparkles, Lock } from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

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
  const [cardDetails, setCardDetails] = useState({
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

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto animate-fadeIn">
      <div className="relative bg-white rounded-3xl max-w-2xl w-full shadow-2xl overflow-hidden my-8 max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="p-6 border-b border-gray-200 flex items-center justify-between bg-gray-50">
          <div>
            <h2 className="text-xl font-extrabold text-gray-900">Secure Checkout & Payment</h2>
            <p className="text-xs text-gray-500">Step 3 of 3 · AI Specialist Bookstore</p>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-gray-400 hover:text-gray-700 rounded-full hover:bg-gray-200 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {errorMsg && (
          <div className="mx-6 mt-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl font-medium">
            {errorMsg}
          </div>
        )}

        {/* Content Body */}
        <div className="overflow-y-auto p-6 space-y-6">
          {/* Section 1: Delivery Address (Slide 3 Node 9, Slide 8) */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-gray-900 flex items-center gap-2">
                <MapPin className="w-4 h-4 text-ibm-blue" />
                <span>1. Select Delivery Address</span>
              </h3>
              {!showNewAddressForm && (
                <button
                  onClick={() => setShowNewAddressForm(true)}
                  className="text-xs font-semibold text-ibm-blue hover:text-ibm-darkBlue flex items-center gap-1"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Add New</span>
                </button>
              )}
            </div>

            {showNewAddressForm ? (
              <form onSubmit={handleCreateAddress} className="bg-gray-50 p-4 rounded-2xl border border-gray-200 space-y-3 text-xs">
                <div className="font-bold text-gray-800">Add New Shipping Address</div>
                <div className="grid grid-cols-2 gap-2">
                  <input
                    type="text"
                    required
                    placeholder="Recipient Full Name"
                    value={newAddress.recipientName}
                    onChange={(e) => setNewAddress({ ...newAddress, recipientName: e.target.value })}
                    className="p-2.5 bg-white border border-gray-300 rounded-lg focus:outline-none focus:border-ibm-blue"
                  />
                  <input
                    type="text"
                    required
                    placeholder="Street Address"
                    value={newAddress.streetAddress}
                    onChange={(e) => setNewAddress({ ...newAddress, streetAddress: e.target.value })}
                    className="p-2.5 bg-white border border-gray-300 rounded-lg focus:outline-none focus:border-ibm-blue"
                  />
                </div>
                <div className="grid grid-cols-3 gap-2">
                  <input
                    type="text"
                    required
                    placeholder="City"
                    value={newAddress.city}
                    onChange={(e) => setNewAddress({ ...newAddress, city: e.target.value })}
                    className="p-2.5 bg-white border border-gray-300 rounded-lg focus:outline-none focus:border-ibm-blue"
                  />
                  <input
                    type="text"
                    required
                    placeholder="State"
                    value={newAddress.state}
                    onChange={(e) => setNewAddress({ ...newAddress, state: e.target.value })}
                    className="p-2.5 bg-white border border-gray-300 rounded-lg focus:outline-none focus:border-ibm-blue"
                  />
                  <input
                    type="text"
                    required
                    placeholder="Postal Code"
                    value={newAddress.postalCode}
                    onChange={(e) => setNewAddress({ ...newAddress, postalCode: e.target.value })}
                    className="p-2.5 bg-white border border-gray-300 rounded-lg focus:outline-none focus:border-ibm-blue"
                  />
                </div>
                <div className="flex items-center justify-end gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setShowNewAddressForm(false)}
                    className="px-3 py-1.5 text-gray-600 hover:bg-gray-200 rounded-lg"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-1.5 bg-ibm-blue hover:bg-ibm-darkBlue text-white font-bold rounded-lg shadow-xs"
                  >
                    Save Address
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
                      className={`p-3.5 rounded-2xl border transition cursor-pointer flex flex-col justify-between ${
                        isSelected
                          ? 'border-ibm-blue bg-blue-50/50 ring-2 ring-ibm-blue/20'
                          : 'border-gray-200 hover:border-gray-300 bg-white'
                      }`}
                    >
                      <div>
                        <div className="flex items-center justify-between mb-1">
                          <span className="text-xs font-bold text-gray-900">{addr.recipientName}</span>
                          {addr.isDefault && (
                            <span className="text-[10px] bg-blue-100 text-ibm-blue px-1.5 py-0.5 rounded font-semibold">
                              Default
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-gray-600">{addr.streetAddress}</p>
                        <p className="text-xs text-gray-500">{addr.city}, {addr.state} {addr.postalCode}</p>
                      </div>
                      <div className="mt-2 text-right">
                        {isSelected && (
                          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-ibm-blue">
                            <CheckCircle2 className="w-3.5 h-3.5" /> Selected
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Section 2: Redeem Gift Loyalty Points (Slide 3 Node 10, Slide 8) */}
          <div className="p-4 bg-amber-50/70 border border-amber-200 rounded-2xl space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Gift className="w-4 h-4 text-amber-600" />
                <h3 className="text-xs font-bold text-amber-950 uppercase tracking-wide">
                  2. Redeem Loyalty Gift Points
                </h3>
              </div>
              <span className="text-xs font-bold text-amber-900 bg-amber-100 px-2.5 py-0.5 rounded-full">
                Balance: {user?.giftPointsBalance || 0} pts
              </span>
            </div>

            <p className="text-xs text-amber-800 leading-relaxed">
              Every 10 gift points gives you <span className="font-bold">$1.00 USD</span> instant discount.
            </p>

            {maxPossiblePoints > 0 ? (
              <div className="space-y-2 pt-1">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-gray-600">Points to redeem: <strong className="text-gray-900">{pointsToRedeem}</strong></span>
                  <span className="font-bold text-emerald-700">-${pointsDiscount.toFixed(2)} USD</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max={maxPossiblePoints}
                  step="10"
                  value={pointsToRedeem}
                  onChange={(e) => setPointsToRedeem(Number(e.target.value))}
                  className="w-full accent-amber-600 cursor-pointer"
                />
                <div className="flex justify-between text-[10px] text-gray-400">
                  <span>0 pts ($0)</span>
                  <button
                    type="button"
                    onClick={() => setPointsToRedeem(maxPossiblePoints)}
                    className="text-amber-800 font-bold hover:underline"
                  >
                    Use Max ({maxPossiblePoints} pts)
                  </button>
                </div>
              </div>
            ) : (
              <p className="text-xs text-gray-500 italic">No points available or cart is already free.</p>
            )}
          </div>

          {/* Section 3: Payment Options (Slide 3 Node 10, Slide 9) */}
          <div className="space-y-3">
            <h3 className="text-sm font-bold text-gray-900 flex items-center gap-2">
              <CreditCard className="w-4 h-4 text-ibm-blue" />
              <span>3. Payment Method</span>
            </h3>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {[
                { id: 'CREDIT_CARD', label: 'Credit Card', icon: '💳' },
                { id: 'DEBIT_CARD', label: 'Debit Card', icon: '💳' },
                { id: 'UPI', label: 'UPI / Instant', icon: '⚡' },
                { id: 'NET_BANKING', label: 'Net Banking', icon: '🏦' },
              ].map((opt) => (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => setPaymentMethod(opt.id)}
                  className={`p-3 rounded-xl border text-xs font-semibold flex flex-col items-center gap-1 transition ${
                    paymentMethod === opt.id
                      ? 'border-ibm-blue bg-blue-50 text-ibm-blue font-bold shadow-xs'
                      : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  <span className="text-base">{opt.icon}</span>
                  <span>{opt.label}</span>
                </button>
              ))}
            </div>

            {/* Simulated Card Form */}
            {(paymentMethod === 'CREDIT_CARD' || paymentMethod === 'DEBIT_CARD') && (
              <div className="p-4 bg-gray-50 border border-gray-200 rounded-2xl space-y-2 text-xs">
                <div>
                  <label className="block text-[11px] font-semibold text-gray-600 mb-1">Card Number (Demo Sandbox)</label>
                  <input
                    type="text"
                    disabled
                    value={cardDetails.cardNumber}
                    className="w-full p-2.5 bg-white border border-gray-300 rounded-lg text-gray-800 font-mono"
                  />
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-[11px] font-semibold text-gray-600 mb-1">Expiry Date</label>
                    <input
                      type="text"
                      disabled
                      value={cardDetails.expiry}
                      className="w-full p-2.5 bg-white border border-gray-300 rounded-lg text-gray-800 font-mono"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-gray-600 mb-1">CVV</label>
                    <input
                      type="text"
                      disabled
                      value={cardDetails.cvv}
                      className="w-full p-2.5 bg-white border border-gray-300 rounded-lg text-gray-800 font-mono"
                    />
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Footer Summary & Authorization Button */}
        <div className="p-6 border-t border-gray-200 bg-gray-50 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="text-left w-full sm:w-auto">
            <div className="text-xs text-gray-500">
              Subtotal: ${cart.subtotal.toFixed(2)} {pointsDiscount > 0 && `· Discount: -$${pointsDiscount.toFixed(2)}`}
            </div>
            <div className="text-xl font-black text-gray-900">
              Total: <span className="text-ibm-blue">${finalTotal.toFixed(2)}</span>
            </div>
          </div>

          <button
            onClick={handlePlaceOrder}
            disabled={isSubmitting || !selectedAddressId}
            className="w-full sm:w-auto px-8 py-3.5 bg-ibm-blue hover:bg-ibm-darkBlue disabled:bg-gray-400 text-white font-bold rounded-xl flex items-center justify-center gap-2 shadow-xl transition active:scale-98"
          >
            <Lock className="w-4 h-4" />
            <span>{isSubmitting ? 'Authorizing Payment...' : `Authorize & Pay $${finalTotal.toFixed(2)}`}</span>
          </button>
        </div>
      </div>
    </div>
  );
}
