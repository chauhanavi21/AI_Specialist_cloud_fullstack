import React, { useState } from 'react';
import { api } from '../api/client';

const EMPTY = {
  recipientName: '',
  email: '',
  phoneNumber: '',
  streetAddress: '',
  addressLine2: '',
  city: '',
  state: '',
  postalCode: '',
  country: 'India',
  isDefault: false,
};

const COUNTRIES = ['India', 'United States', 'United Kingdom', 'Canada', 'Australia', 'Singapore', 'United Arab Emirates'];

function validate(v) {
  const errors = {};
  if (!v.recipientName.trim()) errors.recipientName = 'Enter the recipient name';
  if (!/^\S+@\S+\.\S+$/.test(v.email.trim())) errors.email = 'Enter a valid email address';
  if (!/^[+0-9][0-9 -]{6,18}$/.test(v.phoneNumber.trim())) errors.phoneNumber = 'Enter a valid phone number';
  if (!v.streetAddress.trim()) errors.streetAddress = 'Enter the address';
  if (!v.city.trim()) errors.city = 'Enter the city';
  if (!v.state.trim()) errors.state = 'Enter the state';
  if (!v.postalCode.trim()) errors.postalCode = 'Enter the PIN code';
  else if (v.country === 'India' && !/^[1-9][0-9]{5}$/.test(v.postalCode.trim())) errors.postalCode = 'PIN code must be 6 digits';
  return errors;
}

/** Delivery address form with every field in the wireframe, validated before it is sent. */
export default function AddressForm({ defaults, canCancel, onCancel, onSaved }) {
  const [values, setValues] = useState({ ...EMPTY, ...defaults });
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState('');
  const [saving, setSaving] = useState(false);

  const set = (key) => (e) => {
    setValues({ ...values, [key]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });
    if (errors[key]) setErrors({ ...errors, [key]: undefined });
  };

  const submit = async (e) => {
    e.preventDefault();
    const found = validate(values);
    setErrors(found);
    if (Object.keys(found).length > 0) {
      document.getElementById(`addr-${Object.keys(found)[0]}`)?.focus();
      return;
    }
    setSaving(true);
    setSubmitError('');
    try {
      onSaved(await api.addAddress(values));
    } catch (err) {
      setSubmitError(err.message || 'Could not save the address');
    } finally {
      setSaving(false);
    }
  };

  const field = (key, label, extra = {}) => (
    <div className={extra.className}>
      <label htmlFor={`addr-${key}`} className="field-label">{label}</label>
      <input
        id={`addr-${key}`}
        type={extra.type || 'text'}
        value={values[key]}
        onChange={set(key)}
        placeholder={extra.placeholder || label}
        autoComplete={extra.autoComplete}
        inputMode={extra.inputMode}
        aria-invalid={!!errors[key]}
        aria-describedby={errors[key] ? `addr-${key}-err` : undefined}
        className="field"
      />
      {errors[key] && <p id={`addr-${key}-err`} className="field-error" role="alert">{errors[key]}</p>}
    </div>
  );

  return (
    <form onSubmit={submit} noValidate className="space-y-3">
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        {field('recipientName', 'First and last name', { autoComplete: 'name', placeholder: 'Full name' })}
        {field('email', 'Email', { type: 'email', autoComplete: 'email', placeholder: 'name@example.com' })}
        {field('phoneNumber', 'Phone number', { type: 'tel', autoComplete: 'tel', inputMode: 'tel', placeholder: '+91 98765 43210' })}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {field('streetAddress', 'Address', { autoComplete: 'address-line1', placeholder: 'House, street, area' })}
        {field('addressLine2', 'Address line 2 (optional)', { autoComplete: 'address-line2', placeholder: 'Landmark, floor' })}
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {field('city', 'City', { autoComplete: 'address-level2' })}
        {field('state', 'State', { autoComplete: 'address-level1' })}
        {field('postalCode', 'PIN code', { autoComplete: 'postal-code', inputMode: 'numeric', placeholder: '000000' })}
        <div>
          <label htmlFor="addr-country" className="field-label">Country</label>
          <select id="addr-country" value={values.country} onChange={set('country')} autoComplete="country-name" className="field">
            {COUNTRIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>
      </div>

      <label className="flex items-center gap-2 text-xs text-fg2">
        <input type="checkbox" checked={values.isDefault} onChange={set('isDefault')} className="accent-brand size-4" />
        Make this my default address
      </label>

      {submitError && <p className="field-error" role="alert">{submitError}</p>}

      <div className="flex justify-end gap-2">
        {canCancel && <button type="button" onClick={onCancel} className="btn-ghost">Cancel</button>}
        <button type="submit" disabled={saving} className="btn-primary">
          {saving ? 'Saving...' : 'Save address'}
        </button>
      </div>
    </form>
  );
}
