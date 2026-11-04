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
import { ArrowUpDown, BookOpen, Frown, Sparkles } from 'lucide-react';

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
    <div className="min-h-screen flex flex-col bg-[#fafafa]">
      <Navbar
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        onOpenHistory={() => setIsHistoryOpen(true)}
        onOpenPoints={() => setIsPointsOpen(true)}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
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
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
            <div>
              <h2 className="text-xl sm:text-2xl font-black text-gray-900 flex items-center gap-2">
                <BookOpen className="w-6 h-6 text-ibm-blue" />
                <span>Explore Book Catalog</span>
                <span className="text-xs font-semibold text-gray-400 bg-gray-100 px-2.5 py-0.5 rounded-full ml-1">
                  {totalBooks} titles
                </span>
              </h2>
              <p className="text-xs text-gray-500 mt-0.5">
                Browse by topic category or publisher brand
              </p>
            </div>

            {/* Sort Dropdown */}
            <div className="flex items-center gap-2 text-xs">
              <span className="text-gray-500 font-medium flex items-center gap-1">
                <ArrowUpDown className="w-3.5 h-3.5" />
                Sort by:
              </span>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="bg-white border border-gray-300 rounded-xl px-3 py-1.5 text-xs font-semibold text-gray-800 focus:outline-none focus:border-ibm-blue shadow-xs"
              >
                <option value="featured">Featured & Top Rated</option>
                <option value="price_asc">Price: Low to High</option>
                <option value="price_desc">Price: High to Low</option>
                <option value="rating_desc">Highest Customer Rating</option>
                <option value="title_asc">Title: A to Z</option>
                <option value="newest">Newest Releases</option>
              </select>
            </div>
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
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6 py-8">
              {[...Array(8)].map((_, i) => (
                <div key={i} className="bg-white rounded-2xl border border-gray-200 aspect-[3/4] animate-pulse p-4 flex flex-col justify-between">
                  <div className="bg-gray-200 rounded-xl w-full h-44 mb-3" />
                  <div className="space-y-2">
                    <div className="bg-gray-200 h-3 w-3/4 rounded" />
                    <div className="bg-gray-200 h-3 w-1/2 rounded" />
                  </div>
                </div>
              ))}
            </div>
          ) : books.length === 0 ? (
            <div className="bg-white rounded-3xl p-12 text-center border border-gray-200 my-6 shadow-xs">
              <Frown className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <h3 className="text-base font-bold text-gray-800">No books found</h3>
              <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
                No titles match your active filters. Try searching for other topics or resetting category and brand filters.
              </p>
              <button
                onClick={() => {
                  setSearchQuery('');
                  setSelectedCategory(null);
                  setSelectedPublisher(null);
                }}
                className="mt-4 px-4 py-2 bg-ibm-blue text-white text-xs font-semibold rounded-xl hover:bg-ibm-darkBlue transition"
              >
                Reset Filters
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
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
