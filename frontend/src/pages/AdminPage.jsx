import React, { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import { useToast } from '../context/ToastContext';
import { fullDate, inr } from '../lib/format';

const TABS = [
  { id: 'overview', label: 'Overview' },
  { id: 'orders', label: 'Orders & shipping' },
  { id: 'returns', label: 'Returns' },
  { id: 'catalogue', label: 'Catalogue' },
  { id: 'stores', label: 'Stores & policies' },
];

/** Store administration. The server enforces the admin role on every call; this page is only shown to admins. */
export default function AdminPage() {
  const [tab, setTab] = useState('overview');
  return (
    <section aria-labelledby="admin-title" className="space-y-5">
      <div>
        <h1 id="admin-title" className="text-xl font-semibold mb-1">Administration</h1>
        <p className="text-xs text-fg3">Manage orders, returns, the catalogue and store policies.</p>
      </div>

      <div role="tablist" aria-label="Administration sections" className="flex flex-wrap gap-1 border-b border-line">
        {TABS.map((t) => (
          <button
            key={t.id}
            role="tab"
            id={`tab-${t.id}`}
            aria-selected={tab === t.id}
            aria-controls={`panel-${t.id}`}
            onClick={() => setTab(t.id)}
            className={`px-3 py-2 text-sm border-b-2 -mb-px ${tab === t.id ? 'border-brand text-fg' : 'border-transparent text-fg2 hover:text-fg hover:bg-layer2'}`}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div role="tabpanel" id={`panel-${tab}`} aria-labelledby={`tab-${tab}`} className="min-w-0">
        {tab === 'overview' && <Overview />}
        {tab === 'orders' && <Orders />}
        {tab === 'returns' && <Returns />}
        {tab === 'catalogue' && <Catalogue />}
        {tab === 'stores' && <Stores />}
      </div>
    </section>
  );
}

/** Loads something on mount and exposes a reload; keeps loading and error handling out of every tab. */
function useLoad(fn, deps = []) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const load = useCallback(() => {
    setError('');
    return fn().then(setData).catch((err) => setError(err.message || 'Could not load this section'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  useEffect(() => { load(); }, [load]);
  return { data, error, reload: load };
}

function State({ error, data, children }) {
  if (error) return <p className="text-sm text-danger" role="alert">{error}</p>;
  if (!data) return <p className="text-sm text-fg3" role="status">Loading...</p>;
  return children;
}

// ---- Overview -----------------------------------------------------------------------------

function Overview() {
  const { data, error } = useLoad(() => api.adminOverview());
  return (
    <State error={error} data={data}>
      {data && (
        <dl className="grid grid-cols-2 lg:grid-cols-5 gap-3">
          {[
            ['Orders', data.orders],
            ['Customers', data.customers],
            ['Books', data.books],
            ['Open returns', data.openReturns],
            ['Revenue after refunds', inr(data.revenue)],
          ].map(([label, value]) => (
            <div key={label} className="panel p-4">
              <dt className="text-xs text-fg3">{label}</dt>
              <dd className="text-xl font-semibold mt-1">{value}</dd>
            </div>
          ))}
        </dl>
      )}
    </State>
  );
}

// ---- Orders & shipping --------------------------------------------------------------------------

function Orders() {
  const toast = useToast();
  const { data, error, reload } = useLoad(() => api.adminOrders());
  const [busy, setBusy] = useState(null);

  const ship = async (order, status) => {
    setBusy(order.id);
    try {
      await api.adminShipOrder(order.id, status);
      toast.success(`${order.orderNumber} marked as ${status.toLowerCase()}`);
      await reload();
    } catch (err) {
      toast.error(err.message || 'Could not update the shipment');
    } finally {
      setBusy(null);
    }
  };

  return (
    <State error={error} data={data}>
      {data && (data.length === 0 ? <p className="text-sm text-fg3">No orders yet.</p> : (
        <div className="overflow-x-auto max-w-full">
          <table className="w-full min-w-[40rem] text-sm">
            <caption className="sr-only">Recent orders</caption>
            <thead>
              <tr className="text-left text-xs text-fg3 border-b border-line">
                <th scope="col" className="py-2 pr-3">Order</th>
                <th scope="col" className="py-2 pr-3">Customer</th>
                <th scope="col" className="py-2 pr-3">Status</th>
                <th scope="col" className="py-2 pr-3">Total</th>
                <th scope="col" className="py-2 pr-3">Placed</th>
                <th scope="col" className="py-2"><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              {data.map((o) => (
                <tr key={o.id} className="border-b border-line">
                  <td className="py-2 pr-3 whitespace-nowrap">{o.orderNumber}</td>
                  <td className="py-2 pr-3">{o.customerEmail}</td>
                  <td className="py-2 pr-3">{o.status}{o.paymentStatus !== 'PAID' ? ` · ${o.paymentStatus.replace('_', ' ').toLowerCase()}` : ''}</td>
                  <td className="py-2 pr-3">{inr(o.totalAmount)}</td>
                  <td className="py-2 pr-3 whitespace-nowrap">{fullDate(o.createdAt)}</td>
                  <td className="py-2 text-right whitespace-nowrap">
                    {(o.status === 'CONFIRMED' || o.status === 'PROCESSING') && (
                      <button disabled={busy === o.id} onClick={() => ship(o, 'SHIPPED')} className="btn-secondary min-h-[2rem]">Mark shipped</button>
                    )}
                    {o.status === 'SHIPPED' && (
                      <button disabled={busy === o.id} onClick={() => ship(o, 'DELIVERED')} className="btn-secondary min-h-[2rem]">Mark delivered</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ))}
    </State>
  );
}

// ---- Returns ---------------------------------------------------------------------------------------

function Returns() {
  const toast = useToast();
  const [all, setAll] = useState(false);
  const { data, error, reload } = useLoad(() => api.adminReturns(all), [all]);
  const [busy, setBusy] = useState(null);

  const act = async (r, action, done) => {
    setBusy(r.id);
    try {
      const res = await api.adminReturnAction(r.id, action, null);
      toast.success(res.message || done);
      await reload();
    } catch (err) {
      toast.error(err.message || 'Could not update this return');
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="space-y-3">
      <label className="inline-flex items-center gap-2 text-sm text-fg2">
        <input type="checkbox" checked={all} onChange={(e) => setAll(e.target.checked)} />
        Show closed returns too
      </label>
      <State error={error} data={data}>
        {data && (data.length === 0 ? <p className="text-sm text-fg3">No returns to handle.</p> : (
          <ul className="space-y-3">
            {data.map((r) => (
              <li key={r.id} className="panel p-4 space-y-2">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-medium">{r.rmaNumber} <span className="text-fg3 text-xs">for {r.orderNumber}</span></span>
                  <span className="text-xs px-2 py-0.5 bg-layer2">{r.status.replace('_', ' ')}</span>
                </div>
                <p className="text-sm text-fg2">{r.items.map((i) => `${i.quantity} × ${i.bookTitle}`).join(', ')}</p>
                <p className="text-xs text-fg3">Reason: {r.reason}. Collection {r.returnCarrier}, {r.returnTrackingNumber}.</p>
                {r.status === 'REFUNDED' && <p className="text-xs text-fg3">Refunded {inr(r.refundAmount)} ({r.refundReference}).</p>}
                {(r.status === 'REQUESTED' || r.status === 'PICKED_UP') && (
                  <div className="flex flex-wrap gap-2">
                    {r.status === 'REQUESTED' && (
                      <button disabled={busy === r.id} onClick={() => act(r, 'pickup', 'Marked as picked up')} className="btn-secondary min-h-[2rem]">Mark picked up</button>
                    )}
                    <button disabled={busy === r.id} onClick={() => act(r, 'receive', 'Refund paid')} className="btn-primary min-h-[2rem]">Received: restock and refund</button>
                    <button disabled={busy === r.id} onClick={() => act(r, 'reject', 'Return rejected')} className="btn-danger min-h-[2rem]">Reject</button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        ))}
      </State>
    </div>
  );
}

// ---- Catalogue -----------------------------------------------------------------------------------------

function Catalogue() {
  const toast = useToast();
  const cats = useLoad(() => api.getCategories());
  const pubs = useLoad(() => api.getPublishers());
  const books = useLoad(() => api.getBooks({ size: 100 }));
  const [catName, setCatName] = useState('');
  const [catVisibility, setCatVisibility] = useState('PUBLIC');
  const [book, setBook] = useState({ title: '', author: '', isbn: '', categoryId: '', publisherId: '', price: '', stockQuantity: '', description: '', publishedYear: new Date().getFullYear(), pageCount: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const addCategory = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      await api.adminCreateCategory({ name: catName, visibility: catVisibility });
      toast.success(`Category "${catName}" created`);
      setCatName('');
      await cats.reload();
    } catch (err) {
      toast.error(err.message || 'Could not create the category');
    } finally {
      setBusy(false);
    }
  };

  const addBook = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await api.adminCreateBook({
        ...book,
        categoryId: Number(book.categoryId),
        publisherId: Number(book.publisherId),
        price: Number(book.price),
        stockQuantity: Number(book.stockQuantity),
        publishedYear: Number(book.publishedYear),
        pageCount: Number(book.pageCount),
      });
      toast.success(`"${book.title}" added to the catalogue`);
      setBook({ ...book, title: '', author: '', isbn: '', price: '', stockQuantity: '', description: '', pageCount: '' });
      await Promise.all([books.reload(), cats.reload()]);
    } catch (err) {
      setError(err.message || 'Could not add the book');
    } finally {
      setBusy(false);
    }
  };

  const setStock = async (b, value) => {
    try {
      await api.adminSetStock(b.id, Number(value));
      toast.success(`Stock for "${b.title}" set to ${value}`);
      await books.reload();
    } catch (err) {
      toast.error(err.message || 'Could not update the stock');
    }
  };

  const field = (key, label, props = {}) => (
    <div>
      <label htmlFor={`book-${key}`} className="field-label">{label}</label>
      <input id={`book-${key}`} value={book[key]} onChange={(e) => setBook({ ...book, [key]: e.target.value })} className="field" required {...props} />
    </div>
  );

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div className="space-y-6 min-w-0">
        <form onSubmit={addCategory} className="panel p-4 space-y-3" aria-labelledby="new-cat">
          <h2 id="new-cat" className="text-sm font-semibold">New category</h2>
          <div>
            <label htmlFor="cat-name" className="field-label">Name</label>
            <input id="cat-name" value={catName} onChange={(e) => setCatName(e.target.value)} className="field" required />
          </div>
          <div>
            <label htmlFor="cat-vis" className="field-label">Who can see it</label>
            <select id="cat-vis" value={catVisibility} onChange={(e) => setCatVisibility(e.target.value)} className="field">
              <option value="PUBLIC">Everyone, including guests</option>
              <option value="MEMBERS">Signed-in members only</option>
            </select>
          </div>
          <button type="submit" disabled={busy} className="btn-primary">Create category</button>
        </form>

        <form onSubmit={addBook} className="panel p-4 space-y-3" aria-labelledby="new-book">
          <h2 id="new-book" className="text-sm font-semibold">New book</h2>
          {field('title', 'Title', { maxLength: 255 })}
          {field('author', 'Author', { maxLength: 255 })}
          {field('isbn', 'ISBN', { maxLength: 20 })}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label htmlFor="book-cat" className="field-label">Category</label>
              <select id="book-cat" value={book.categoryId} onChange={(e) => setBook({ ...book, categoryId: e.target.value })} className="field" required>
                <option value="">Choose...</option>
                {(cats.data || []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <div>
              <label htmlFor="book-pub" className="field-label">Publisher</label>
              <select id="book-pub" value={book.publisherId} onChange={(e) => setBook({ ...book, publisherId: e.target.value })} className="field" required>
                <option value="">Choose...</option>
                {(pubs.data || []).map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {field('price', 'Price (₹)', { type: 'number', min: 1, step: '0.01' })}
            {field('stockQuantity', 'Stock', { type: 'number', min: 0 })}
            {field('publishedYear', 'Year', { type: 'number', min: 1900, max: 2100 })}
            {field('pageCount', 'Pages', { type: 'number', min: 1 })}
          </div>
          <div>
            <label htmlFor="book-description" className="field-label">Description</label>
            <textarea id="book-description" rows={3} value={book.description} onChange={(e) => setBook({ ...book, description: e.target.value })} className="field resize-none" required maxLength={4000} />
          </div>
          {error && <p className="field-error" role="alert">{error}</p>}
          <button type="submit" disabled={busy} className="btn-primary">Add book</button>
        </form>
      </div>

      <div className="panel p-4 min-w-0">
        <h2 className="text-sm font-semibold mb-3">Stock</h2>
        <State error={books.error} data={books.data}>
          {books.data && (
            <ul className="divide-y divide-line max-h-[36rem] overflow-y-auto">
              {books.data.content.map((b) => (
                <li key={b.id} className="py-2 flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-sm truncate">{b.title}</p>
                    <p className="text-xs text-fg3">{b.categoryName} · {inr(b.price)}</p>
                  </div>
                  <label className="shrink-0">
                    <span className="sr-only">Stock for {b.title}</span>
                    <input
                      type="number"
                      min={0}
                      defaultValue={b.stockQuantity}
                      key={`${b.id}-${b.stockQuantity}`}
                      onBlur={(e) => e.target.value !== String(b.stockQuantity) && e.target.value !== '' && setStock(b, e.target.value)}
                      className="field w-20"
                    />
                  </label>
                </li>
              ))}
            </ul>
          )}
        </State>
        <p className="text-[11px] text-fg3 mt-2">Change a number and tab away to save it.</p>
      </div>
    </div>
  );
}

// ---- Stores and policies ----------------------------------------------------------------------------

function Stores() {
  const toast = useToast();
  const { data, error, reload } = useLoad(() => api.adminStores());
  const [name, setName] = useState('');
  const [tagline, setTagline] = useState('');
  const [days, setDays] = useState(7);
  const [policy, setPolicy] = useState({ storeId: '', type: 'RETURN', title: '', body: '' });

  const addStore = async (e) => {
    e.preventDefault();
    try {
      await api.adminCreateStore({ name, tagline, returnWindowDays: Number(days) });
      toast.success(`Store "${name}" created`);
      setName(''); setTagline('');
      await reload();
    } catch (err) {
      toast.error(err.message || 'Could not create the store');
    }
  };

  const addPolicy = async (e) => {
    e.preventDefault();
    try {
      await api.adminAddPolicy(policy.storeId, { type: policy.type, title: policy.title, body: policy.body });
      toast.success('Policy added');
      setPolicy({ ...policy, title: '', body: '' });
      await reload();
    } catch (err) {
      toast.error(err.message || 'Could not add the policy');
    }
  };

  const removePolicy = async (storeId, policyId) => {
    try {
      await api.adminDeletePolicy(storeId, policyId);
      toast.success('Policy removed');
      await reload();
    } catch (err) {
      toast.error(err.message || 'Could not remove the policy');
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div className="space-y-4 min-w-0">
        <State error={error} data={data}>
          {data && data.map((s) => (
            <div key={s.id} className="panel p-4 space-y-2">
              <div className="flex flex-wrap items-baseline justify-between gap-2">
                <h2 className="text-sm font-semibold">{s.name}{!s.active && <span className="text-xs text-fg3"> (closed)</span>}</h2>
                <span className="text-xs text-fg3">{s.returnWindowDays}-day returns</span>
              </div>
              <ul className="space-y-2">
                {s.policies.map((p) => (
                  <li key={p.id} className="flex items-start justify-between gap-3 text-sm">
                    <span><span className="text-xs text-link">{p.type}</span> {p.title}</span>
                    <button onClick={() => removePolicy(s.id, p.id)} className="text-xs text-danger hover:underline shrink-0">Remove</button>
                  </li>
                ))}
                {s.policies.length === 0 && <li className="text-xs text-fg3">No policies yet.</li>}
              </ul>
            </div>
          ))}
        </State>
      </div>

      <div className="space-y-6 min-w-0">
        <form onSubmit={addStore} className="panel p-4 space-y-3" aria-labelledby="new-store">
          <h2 id="new-store" className="text-sm font-semibold">New store</h2>
          <div>
            <label htmlFor="store-name" className="field-label">Name</label>
            <input id="store-name" value={name} onChange={(e) => setName(e.target.value)} className="field" required maxLength={150} />
          </div>
          <div>
            <label htmlFor="store-tagline" className="field-label">Tagline</label>
            <input id="store-tagline" value={tagline} onChange={(e) => setTagline(e.target.value)} className="field" maxLength={255} />
          </div>
          <div>
            <label htmlFor="store-days" className="field-label">Return window (days)</label>
            <input id="store-days" type="number" min={0} max={90} value={days} onChange={(e) => setDays(e.target.value)} className="field" required />
          </div>
          <button type="submit" className="btn-primary">Create store</button>
        </form>

        <form onSubmit={addPolicy} className="panel p-4 space-y-3" aria-labelledby="new-policy">
          <h2 id="new-policy" className="text-sm font-semibold">New policy</h2>
          <div>
            <label htmlFor="policy-store" className="field-label">Store</label>
            <select id="policy-store" value={policy.storeId} onChange={(e) => setPolicy({ ...policy, storeId: e.target.value })} className="field" required>
              <option value="">Choose...</option>
              {(data || []).map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div>
            <label htmlFor="policy-type" className="field-label">Type</label>
            <select id="policy-type" value={policy.type} onChange={(e) => setPolicy({ ...policy, type: e.target.value })} className="field">
              {['CANCELLATION', 'RETURN', 'SHIPPING', 'PRIVACY', 'PAYMENT', 'OTHER'].map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div>
            <label htmlFor="policy-title" className="field-label">Title</label>
            <input id="policy-title" value={policy.title} onChange={(e) => setPolicy({ ...policy, title: e.target.value })} className="field" required maxLength={150} />
          </div>
          <div>
            <label htmlFor="policy-body" className="field-label">Policy text</label>
            <textarea id="policy-body" rows={4} value={policy.body} onChange={(e) => setPolicy({ ...policy, body: e.target.value })} className="field resize-none" required maxLength={4000} />
          </div>
          <button type="submit" className="btn-primary">Add policy</button>
        </form>
      </div>
    </div>
  );
}
