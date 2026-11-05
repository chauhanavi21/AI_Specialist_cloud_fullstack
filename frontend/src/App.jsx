import React, { useState, useEffect } from 'react';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import Navbar from './components/Navbar';
import HeroBanner from './components/HeroBanner';
import RecommendationsShelf from './components/RecommendationsShelf';
import CategoryPills from './components/CategoryPills';
import PublisherFilter from './components/PublisherFilter';
import BookCard from './components/BookCard';
import BookDetailModal from './components/BookDetailModal';
import CartDrawer from './components/CartDrawer';
import CheckoutModal from './components/CheckoutModal';
import OrderConfirmationModal from './components/OrderConfirmationModal';
import OrderHistoryModal from './components/OrderHistoryModal';
import GiftPointsModal from './components/GiftPointsModal';
import AuthModal from './components/AuthModal';
import Footer from './components/Footer';
import { api } from './api/client';
import { Frown } from 'lucide-react';

function BookstoreMain() {
  const [categories, setCategories] = useState([]);
  const [publishers, setPublishers] = useState([]);
  const [books, setBooks] = useState([]);
  const [totalBooks, setTotalBooks] = useState(0);
  const [loading, setLoading] = useState(true);

  // Filters state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [selectedPublisher, setSelectedPublisher] = useState(null);
  const [sortBy, setSortBy] = useState('featured');

  // Modals state
  const [selectedBookId, setSelectedBookId] = useState(null);
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [isPointsOpen, setIsPointsOpen] = useState(false);
  const [orderConfirmation, setOrderConfirmation] = useState(null);

  // Load Categories & Publishers on mount
  useEffect(() => {
    Promise.all([
      api.getCategories(),
      api.getPublishers()
    ])
      .then(([cats, pubs]) => {
        setCategories(cats || []);
        setPublishers(pubs || []);
      })
      .catch((err) => console.error('Failed to load initial metadata', err));
  }, []);

  // Fetch Books whenever filters change
  const fetchBooks = () => {
    setLoading(true);
    api.getBooks({
      query: searchQuery,
      categoryId: selectedCategory,
      publisherId: selectedPublisher,
      sortBy: sortBy,
      size: 24,
    })
      .then((res) => {
        setBooks(res.content || []);
        setTotalBooks(res.totalElements || 0);
      })
      .catch((err) => console.error('Failed to load books', err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchBooks();
    }, 250);
    return () => clearTimeout(timer);
  }, [searchQuery, selectedCategory, selectedPublisher, sortBy]);

  const handleOpenDetail = (book) => {
    setSelectedBookId(book.id);
  };

  return (
    <div className="min-h-screen flex flex-col bg-brand-50">
      <Navbar
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        onOpenHistory={() => setIsHistoryOpen(true)}
        onOpenPoints={() => setIsPointsOpen(true)}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-3 min-[420px]:px-4 sm:px-6 lg:px-8 py-4 sm:py-6">
        {/* Hero Section */}
        <HeroBanner onExploreClick={() => {
          const el = document.getElementById('catalog-section');
          el?.scrollIntoView({ behavior: 'smooth' });
        }} />

        {/* Dynamic Personalized Recommendations (Order History Powered) */}
        <RecommendationsShelf onSelectBook={(book) => setSelectedBookId(book.id)} />

        {/* Catalog Section */}
        <section id="catalog-section" className="pt-2">
          {/* Header & Sort Controls */}
          <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-3 mb-4">
            <div>
              <h2 className="text-xl font-semibold text-slate-900">Catalog</h2>
              <p className="text-xs text-slate-500">{totalBooks} titles</p>
            </div>

            <label className="flex items-center justify-between sm:justify-start gap-2 text-sm text-slate-600">
              Sort by
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="bg-white border border-brand-200 rounded-md px-2.5 py-2 sm:py-1.5 text-base sm:text-sm text-slate-800 focus:outline-none focus:border-brand-500"
              >
                <option value="featured">Featured</option>
                <option value="price_asc">Price: low to high</option>
                <option value="price_desc">Price: high to low</option>
                <option value="rating_desc">Rating</option>
                <option value="title_asc">Title: A to Z</option>
                <option value="newest">Newest</option>
              </select>
            </label>
          </div>

          {/* Category Filter Pills (Slide 3 Node 5, Slide 6) */}
          <CategoryPills
            categories={categories}
            selectedCategory={selectedCategory}
            onSelectCategory={setSelectedCategory}
          />

          {/* Publisher / Brand Filter (Slide 3 Node 6, Slide 6) */}
          <PublisherFilter
            publishers={publishers}
            selectedPublisher={selectedPublisher}
            onSelectPublisher={setSelectedPublisher}
          />

          {/* Book Cards Grid */}
          {loading ? (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3 sm:gap-5 lg:gap-6 py-4">
              {[...Array(10)].map((_, i) => (
                <div key={i} className="bg-white rounded-lg border border-brand-100 aspect-[3/4] animate-pulse" />
              ))}
            </div>
          ) : books.length === 0 ? (
            <div className="bg-white rounded-lg p-6 sm:p-12 text-center border border-brand-100 my-6">
              <Frown className="w-10 h-10 text-brand-300 mx-auto mb-3" />
              <h3 className="text-base font-medium text-slate-800">No books found</h3>
              <p className="text-sm text-slate-500 mt-1 max-w-sm mx-auto">
                Nothing matches the current search or filters.
              </p>
              <button
                onClick={() => {
                  setSearchQuery('');
                  setSelectedCategory(null);
                  setSelectedPublisher(null);
                }}
                className="mt-4 px-4 py-2 bg-brand-600 text-white text-sm font-medium rounded-md hover:bg-brand-700"
              >
                Reset filters
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3 sm:gap-5 lg:gap-6">
              {books.map((book) => (
                <BookCard
                  key={book.id}
                  book={book}
                  onOpenDetail={handleOpenDetail}
                />
              ))}
            </div>
          )}
        </section>
      </main>

      <Footer />

      {/* Global Modals & Drawers */}
      <BookDetailModal
        bookId={selectedBookId}
        onClose={() => setSelectedBookId(null)}
        onSelectBook={(b) => setSelectedBookId(b.id)}
      />

      <CartDrawer
        onProceedToCheckout={() => setIsCheckoutOpen(true)}
      />

      <CheckoutModal
        isOpen={isCheckoutOpen}
        onClose={() => setIsCheckoutOpen(false)}
        onOrderSuccess={(conf) => setOrderConfirmation(conf)}
      />

      <OrderConfirmationModal
        confirmation={orderConfirmation}
        onClose={() => setOrderConfirmation(null)}
        onViewHistory={() => setIsHistoryOpen(true)}
      />

      <OrderHistoryModal
        isOpen={isHistoryOpen}
        onClose={() => setIsHistoryOpen(false)}
      />

      <GiftPointsModal
        isOpen={isPointsOpen}
        onClose={() => setIsPointsOpen(false)}
      />

      <AuthModal />
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <BookstoreMain />
      </CartProvider>
    </AuthProvider>
  );
}
