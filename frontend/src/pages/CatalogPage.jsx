import React, { useState, useEffect, useCallback } from 'react';
import { Frown } from 'lucide-react';
import CategorySidebar from '../components/CategorySidebar';
import FilterBar, { priceRangeById } from '../components/FilterBar';
import BookCard from '../components/BookCard';
import Shelf from '../components/Shelf';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { plural } from '../lib/format';

export const DEFAULT_FILTERS = { query: '', language: '', format: '', price: '', sortBy: 'featured' };

export default function CatalogPage({ filters, setFilters, onOpenDetail }) {
  const { user } = useAuth();

  const [categories, setCategories] = useState([]);
  const [publishers, setPublishers] = useState([]);
  const [facets, setFacets] = useState({ languages: [], formats: [] });
  const [shelves, setShelves] = useState(null);
  const [recommendations, setRecommendations] = useState([]);

  const [selectedCategory, setSelectedCategory] = useState(null);
  const [selectedPublisher, setSelectedPublisher] = useState(null);

  const [books, setBooks] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([api.getCategories(), api.getPublishers(), api.getFacets(), api.getShelves(3)])
      .then(([cats, pubs, f, s]) => {
        setCategories(cats || []);
        setPublishers(pubs || []);
        setFacets(f || { languages: [], formats: [] });
        setShelves(s);
      })
      .catch((err) => console.error('Failed to load store metadata', err));
  }, [user?.id]);

  useEffect(() => {
    if (!user) { setRecommendations([]); return; }
    api.getHistoryRecommendations(3)
      .then((data) => setRecommendations(data || []))
      .catch((err) => console.error('Failed to load recommendations', err));
  }, [user?.id]);

  useEffect(() => {
    if (selectedCategory !== null && categories.length > 0 && !categories.some((c) => c.id === selectedCategory)) {
      setSelectedCategory(null);
    }
  }, [categories, selectedCategory]);

  const isFiltering =
    !!filters.query || !!filters.language || !!filters.format || !!filters.price ||
    selectedCategory !== null || selectedPublisher !== null || filters.sortBy !== 'featured';

  const fetchBooks = useCallback(() => {
    const range = priceRangeById(filters.price);
    setLoading(true);
    setError('');
    return api.getBooks({
      query: filters.query,
      categoryId: selectedCategory,
      publisherId: selectedPublisher,
      language: filters.language,
      format: filters.format,
      minPrice: range.min,
      maxPrice: range.max,
      sortBy: filters.sortBy,
      size: 30,
    })
      .then((res) => { setBooks(res.content || []); setTotal(res.totalElements || 0); })
      .catch((err) => setError(err.message || 'Could not load books'))
      .finally(() => setLoading(false));
  }, [filters, selectedCategory, selectedPublisher]);

  useEffect(() => {
    const timer = setTimeout(fetchBooks, 250);
    return () => clearTimeout(timer);
  }, [fetchBooks, user?.id]);

  const reset = () => {
    setFilters(DEFAULT_FILTERS);
    setSelectedCategory(null);
    setSelectedPublisher(null);
  };

  const pub = (selected) =>
    `px-3 py-1.5 text-xs whitespace-nowrap shrink-0 ${selected ? 'bg-fg text-canvas' : 'bg-layer2 text-fg2 hover:text-fg'}`;

  return (
    <div className="grid grid-cols-1 lg:grid-cols-[13rem_1fr] gap-x-8 gap-y-4">
      <CategorySidebar categories={categories} selectedCategory={selectedCategory} onSelectCategory={setSelectedCategory} />

      <div className="min-w-0">
        <FilterBar filters={filters} setFilters={setFilters} facets={facets} />

        <div className="flex items-center gap-2 overflow-x-auto scrollbar-none pb-1 mb-6" role="group" aria-label="Publisher">
          <span className="text-xs text-fg3 shrink-0">Publisher</span>
          <button onClick={() => setSelectedPublisher(null)} className={pub(selectedPublisher === null)}>All</button>
          {publishers.map((p) => (
            <button key={p.id} onClick={() => setSelectedPublisher(p.id)} className={pub(selectedPublisher === p.id)}>
              {p.name}
            </button>
          ))}
        </div>

        {!isFiltering && (
          <>
            <Shelf title="Recommended for You" subtitle="Based on your previous orders" books={recommendations} onOpenDetail={onOpenDetail} />
            <Shelf title="Bestsellers this Month" books={shelves?.bestsellers} onOpenDetail={onOpenDetail} />
            <Shelf title="New Launches" books={shelves?.newLaunches} onOpenDetail={onOpenDetail} />
          </>
        )}

        <section aria-labelledby="catalog-title">
          <div className="flex items-baseline justify-between mb-3">
            <h2 id="catalog-title" className="text-base font-semibold text-fg">
              {isFiltering ? 'Results' : 'All books'}
            </h2>
            <p className="text-xs text-fg3" role="status">{plural(total, 'title')}</p>
          </div>

          {loading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3" aria-busy="true">
              {[...Array(6)].map((_, i) => <div key={i} className="bg-layer h-44 animate-pulse" />)}
            </div>
          ) : error ? (
            <div className="panel p-8 text-center space-y-3">
              <p className="text-sm text-danger" role="alert">{error}</p>
              <button onClick={fetchBooks} className="btn-secondary">Try again</button>
            </div>
          ) : books.length === 0 ? (
            <div className="panel p-10 text-center">
              <Frown className="w-10 h-10 text-fg3 mx-auto mb-3" aria-hidden="true" />
              <h3 className="text-base font-medium">No books found</h3>
              <p className="text-sm text-fg3 mt-1">Nothing matches the current search or filters.</p>
              <button onClick={reset} className="btn-primary mt-4">Reset filters</button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
              {books.map((book) => <BookCard key={book.id} book={book} onOpenDetail={onOpenDetail} />)}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
