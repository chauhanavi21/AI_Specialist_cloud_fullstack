import React, { useEffect, useState } from 'react';
import { api } from '../api/client';

const TYPE_LABELS = {
  CANCELLATION: 'Cancellation',
  RETURN: 'Returns',
  SHIPPING: 'Shipping',
  PRIVACY: 'Privacy',
  PAYMENT: 'Payment',
  OTHER: 'Other',
};

/** Store policies and a delivery-rate checker. Open to everyone, signed in or not. */
export default function PoliciesPage() {
  const [stores, setStores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [rate, setRate] = useState(null);
  const [rateError, setRateError] = useState('');
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    api.getStores()
      .then((data) => setStores(data || []))
      .catch((err) => setError(err.message || 'Could not load the store policies'))
      .finally(() => setLoading(false));
  }, []);

  const checkRate = async (e) => {
    e.preventDefault();
    setRate(null);
    if (!city.trim() || !state.trim()) {
      setRateError('Enter a city and a state');
      return;
    }
    setRateError('');
    setChecking(true);
    try {
      setRate(await api.quoteShipping({ city: city.trim(), state: state.trim() }));
    } catch (err) {
      setRateError(err.message || 'Could not check the delivery charge');
    } finally {
      setChecking(false);
    }
  };

  return (
    <section aria-labelledby="policies-title" className="max-w-3xl space-y-8">
      <div>
        <h1 id="policies-title" className="text-xl font-semibold mb-1">Store policies</h1>
        <p className="text-xs text-fg3">How cancellation, returns, delivery and payments work.</p>
      </div>

      {loading ? (
        <p className="text-sm text-fg3 py-8 text-center" role="status">Loading policies...</p>
      ) : error ? (
        <p className="text-sm text-danger" role="alert">{error}</p>
      ) : stores.length === 0 ? (
        <div className="panel p-8 text-center text-sm text-fg3">No store is open right now.</div>
      ) : (
        stores.map((store) => (
          <div key={store.id} className="space-y-3">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <h2 className="text-lg font-semibold">{store.name}</h2>
              <span className="text-xs text-fg3">{store.returnWindowDays}-day return window</span>
            </div>
            {store.tagline && <p className="text-sm text-fg2 -mt-2">{store.tagline}</p>}
            {store.policies.length === 0 ? (
              <p className="text-sm text-fg3">This store has not published any policies yet.</p>
            ) : (
              <ul className="space-y-3">
                {store.policies.map((p) => (
                  <li key={p.id} className="panel p-4">
                    <p className="text-[11px] uppercase tracking-wide text-link">{TYPE_LABELS[p.type] || p.type}</p>
                    <h3 className="text-sm font-semibold mt-0.5">{p.title}</h3>
                    <p className="text-sm text-fg2 mt-1 whitespace-pre-line">{p.body}</p>
                  </li>
                ))}
              </ul>
            )}
          </div>
        ))
      )}

      <form onSubmit={checkRate} className="panel p-4 space-y-3" aria-labelledby="rate-title">
        <h2 id="rate-title" className="text-sm font-semibold">Check the delivery charge</h2>
        <div className="grid sm:grid-cols-2 gap-3">
          <div>
            <label htmlFor="rate-city" className="field-label">City</label>
            <input id="rate-city" value={city} onChange={(e) => setCity(e.target.value)} className="field" autoComplete="address-level2" />
          </div>
          <div>
            <label htmlFor="rate-state" className="field-label">State</label>
            <input id="rate-state" value={state} onChange={(e) => setState(e.target.value)} className="field" autoComplete="address-level1" />
          </div>
        </div>
        {rateError && <p className="field-error" role="alert">{rateError}</p>}
        <button type="submit" disabled={checking} className="btn-secondary">{checking ? 'Checking...' : 'Check'}</button>

        {rate && (
          <p className="text-sm text-fg2" role="status">
            {rate.zone.charAt(0) + rate.zone.slice(1).toLowerCase()} delivery by {rate.carrier}: ₹{rate.fee} per order
            {rate.extraTransitDays > 0 ? `, about ${rate.extraTransitDays} extra day${rate.extraTransitDays === 1 ? '' : 's'} in transit` : ''}.
            Free above ₹{rate.freeDeliveryThreshold}.
          </p>
        )}
      </form>
    </section>
  );
}
