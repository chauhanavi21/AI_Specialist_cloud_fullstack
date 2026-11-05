import React from 'react';
import { Search } from 'lucide-react';

const PRICE_RANGES = [
  { id: '', label: 'All', min: '', max: '' },
  { id: 'u700', label: 'Under ₹700', min: '', max: 700 },
  { id: '700-900', label: '₹700 to ₹900', min: 700, max: 900 },
  { id: 'o900', label: 'Over ₹900', min: 900, max: '' },
];

const SORTS = [
  { id: 'featured', label: 'Relevance' },
  { id: 'price_asc', label: 'Price: low to high' },
  { id: 'price_desc', label: 'Price: high to low' },
  { id: 'rating_desc', label: 'Rating' },
  { id: 'bestselling', label: 'Bestselling' },
  { id: 'newest', label: 'Newest' },
  { id: 'title_asc', label: 'Title: A to Z' },
];

export const priceRangeById = (id) => PRICE_RANGES.find((r) => r.id === id) || PRICE_RANGES[0];

/** The five wireframe controls: search, language, format, price range and sort. */
export default function FilterBar({ filters, setFilters, facets }) {
  const set = (key) => (e) => setFilters({ ...filters, [key]: e.target.value });

  const select = (id, label, value, onChange, options) => (
    <div>
      <label htmlFor={id} className="field-label">{label}</label>
      <select id={id} value={value} onChange={onChange} className="field">
        {options}
      </select>
    </div>
  );

  return (
    <div className="grid grid-cols-2 md:grid-cols-5 gap-3 mb-5">
      <div className="col-span-2 hidden md:block">
        <label htmlFor="filter-search" className="field-label">Search you want to read here</label>
        <div className="relative">
          <input
            id="filter-search"
            type="search"
            placeholder="Search"
            value={filters.query}
            onChange={set('query')}
            className="field pr-9"
          />
          <Search className="w-4 h-4 text-fg3 absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none" aria-hidden="true" />
        </div>
      </div>

      {select('filter-language', 'Language', filters.language, set('language'), [
        <option key="all" value="">All</option>,
        ...(facets.languages || []).map((l) => <option key={l} value={l}>{l}</option>),
      ])}

      {select('filter-format', 'Format (Paperback, ebook etc)', filters.format, set('format'), [
        <option key="all" value="">All</option>,
        ...(facets.formats || []).map((f) => <option key={f} value={f}>{f}</option>),
      ])}

      {select('filter-price', 'Price range', filters.price, set('price'),
        PRICE_RANGES.map((r) => <option key={r.id} value={r.id}>{r.label}</option>))}

      {select('filter-sort', 'Sort by', filters.sortBy, set('sortBy'),
        SORTS.map((s) => <option key={s.id} value={s.id}>{s.label}</option>))}
    </div>
  );
}
