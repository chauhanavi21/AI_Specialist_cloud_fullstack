import React from 'react';
import { inr } from '../lib/format';

const METHODS = [
  { id: 'CREDIT_CARD', label: 'Credit Card' },
  { id: 'DEBIT_CARD', label: 'Debit card' },
  { id: 'UPI', label: 'UPI' },
  { id: 'WALLET', label: 'Wallet' },
];

export const EMPTY_PAYMENT = { cardNumber: '', cardName: '', cardExpiry: '', cardCvv: '', upiId: '' };

/** Groups digits in fours for display: 4242424242424242 -> 4242 4242 4242 4242 */
const groupCard = (v) => v.replace(/\D/g, '').slice(0, 19).replace(/(.{4})/g, '$1 ').trim();

/** Auto-inserts the slash: 1230 -> 12/30 */
const formatExpiry = (v) => {
  const d = v.replace(/\D/g, '').slice(0, 4);
  return d.length > 2 ? `${d.slice(0, 2)}/${d.slice(2)}` : d;
};

/** Returns a map of field -> message for the chosen payment method. */
export function validatePayment(method, values, walletBalance, payable) {
  const errors = {};
  if (method === 'CREDIT_CARD' || method === 'DEBIT_CARD') {
    const digits = values.cardNumber.replace(/\D/g, '');
    if (digits.length < 13 || digits.length > 19) errors.cardNumber = 'Enter the full card number';
    if (!values.cardName.trim()) errors.cardName = 'Enter the name on the card';
    const m = /^(\d{2})\/(\d{2})$/.exec(values.cardExpiry);
    if (!m || Number(m[1]) < 1 || Number(m[1]) > 12) {
      errors.cardExpiry = 'Use MM/YY';
    } else {
      const expiry = new Date(2000 + Number(m[2]), Number(m[1]), 0, 23, 59, 59);
      if (expiry < new Date()) errors.cardExpiry = 'This card has expired';
    }
    if (!/^\d{3,4}$/.test(values.cardCvv)) errors.cardCvv = 'Enter the 3 or 4 digit CVV';
  } else if (method === 'UPI') {
    if (!/^[a-zA-Z0-9.\-_]{2,64}@[a-zA-Z]{2,32}$/.test(values.upiId.trim())) errors.upiId = 'Enter a UPI ID like name@bank';
  } else if (method === 'WALLET' && payable > 0 && walletBalance < payable) {
    errors.wallet = `Your wallet has ${inr(walletBalance)} but this order needs ${inr(payable)}`;
  }
  return errors;
}

export default function PaymentPanel({ method, onMethodChange, values, onChange, errors, walletBalance, payable, onTopUp }) {
  const set = (key, fmt = (v) => v) => (e) => onChange({ ...values, [key]: fmt(e.target.value) });

  const input = (key, label, props = {}) => (
    <div className={props.className}>
      <label htmlFor={`pay-${key}`} className="field-label">{label}</label>
      <input
        id={`pay-${key}`}
        value={values[key]}
        onChange={set(key, props.format)}
        placeholder={props.placeholder}
        inputMode={props.inputMode}
        autoComplete={props.autoComplete}
        maxLength={props.maxLength}
        type={props.type || 'text'}
        aria-invalid={!!errors[key]}
        aria-describedby={errors[key] ? `pay-${key}-err` : undefined}
        className="field"
      />
      {errors[key] && <p id={`pay-${key}-err`} className="field-error" role="alert">{errors[key]}</p>}
    </div>
  );

  return (
    <div className="grid grid-cols-1 sm:grid-cols-[10rem_1fr] border border-line">
      <div role="tablist" aria-label="Payment method" className="flex sm:flex-col bg-canvas overflow-x-auto scrollbar-none">
        {METHODS.map((m) => (
          <button
            key={m.id}
            role="tab"
            id={`tab-${m.id}`}
            aria-selected={method === m.id}
            aria-controls="payment-fields"
            onClick={() => onMethodChange(m.id)}
            className={`px-4 py-3 text-sm text-left whitespace-nowrap border-b-2 sm:border-b-0 sm:border-l-2 ${
              method === m.id
                ? 'bg-layer text-fg font-medium border-brand'
                : 'text-fg2 hover:bg-layer border-transparent'
            }`}
          >
            {m.label}
          </button>
        ))}
      </div>

      <div id="payment-fields" role="tabpanel" aria-labelledby={`tab-${method}`} className="p-4 space-y-3 bg-layer">
        {(method === 'CREDIT_CARD' || method === 'DEBIT_CARD') && (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {input('cardNumber', 'Card number', { format: groupCard, placeholder: 'XXXX XXXX XXXX XXXX', inputMode: 'numeric', autoComplete: 'cc-number', maxLength: 23 })}
              {input('cardName', 'Name on card', { placeholder: 'Name', autoComplete: 'cc-name' })}
              {input('cardCvv', 'CVV', { format: (v) => v.replace(/\D/g, '').slice(0, 4), placeholder: 'XXX', inputMode: 'numeric', autoComplete: 'cc-csc', type: 'password', maxLength: 4 })}
              {input('cardExpiry', 'Date of expiry', { format: formatExpiry, placeholder: 'MM/YY', inputMode: 'numeric', autoComplete: 'cc-exp', maxLength: 5 })}
            </div>
            <p className="text-xs text-fg3">
              This is a demo store: no real payment is taken and card details are never saved.
              Use test card 4242 4242 4242 4242 with any future date. 4000 0000 0000 0002 is declined.
            </p>
          </>
        )}

        {method === 'UPI' && (
          <>
            {input('upiId', 'UPI ID', { placeholder: 'name@bank', autoComplete: 'off' })}
            <p className="text-xs text-fg3">Demo store: any ID in the form name@bank is accepted and nothing is charged.</p>
          </>
        )}

        {method === 'WALLET' && (
          <div className="space-y-3">
            <p className="text-sm text-fg2">
              Wallet balance <strong className="text-fg">{inr(walletBalance)}</strong>
            </p>
            {errors.wallet ? (
              <p className="field-error" role="alert">{errors.wallet}</p>
            ) : (
              <p className="text-xs text-fg3">{inr(payable)} will be taken from your wallet.</p>
            )}
            <button type="button" onClick={onTopUp} className="btn-secondary">Add money to wallet</button>
          </div>
        )}
      </div>
    </div>
  );
}
