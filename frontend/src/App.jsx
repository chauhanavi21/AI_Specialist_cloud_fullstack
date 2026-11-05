import React, { useState } from 'react';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { ToastProvider } from './context/ToastContext';
import { WishlistProvider } from './context/WishlistContext';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import BookDetailModal from './components/BookDetailModal';
import CartDrawer from './components/CartDrawer';
import CheckoutModal from './components/CheckoutModal';
import OrderConfirmationModal from './components/OrderConfirmationModal';
import GiftPointsModal from './components/GiftPointsModal';
import WalletModal from './components/WalletModal';
import AuthModal from './components/AuthModal';
import CatalogPage, { DEFAULT_FILTERS } from './pages/CatalogPage';
import OrdersPage from './pages/OrdersPage';
import WishlistPage from './pages/WishlistPage';
import WritersPage from './pages/WritersPage';
import { useAuth } from './context/AuthContext';

function BookstoreMain() {
  const { loading: authLoading } = useAuth();
  const [view, setView] = useState('catalog'); // catalog | orders | wishlist | writers
  const [filters, setFilters] = useState(DEFAULT_FILTERS);

  const [selectedBookId, setSelectedBookId] = useState(null);
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const [isPointsOpen, setIsPointsOpen] = useState(false);
  const [isWalletOpen, setIsWalletOpen] = useState(false);
  const [orderConfirmation, setOrderConfirmation] = useState(null);

  const openDetail = (book) => setSelectedBookId(book.id);

  // Wait for the stored session to be checked so signed-in pages don't flash "Sign in".
  if (authLoading) {
    return <div className="min-h-screen flex items-center justify-center text-sm text-fg3" role="status">Loading...</div>;
  }

  return (
    <div className="min-h-screen flex flex-col bg-canvas pb-16 sm:pb-0">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-[200] focus:bg-brand focus:text-white focus:px-4 focus:py-2"
      >
        Skip to main content
      </a>

      <Navbar
        view={view}
        onNavigate={setView}
        searchQuery={filters.query}
        setSearchQuery={(query) => setFilters({ ...filters, query })}
        onOpenPoints={() => setIsPointsOpen(true)}
        onOpenWallet={() => setIsWalletOpen(true)}
      />

      <main id="main" className="flex-1 max-w-[88rem] w-full mx-auto px-3 sm:px-6 py-5 sm:py-6">
        {view === 'catalog' && <CatalogPage filters={filters} setFilters={setFilters} onOpenDetail={openDetail} />}
        {view === 'orders' && <OrdersPage />}
        {view === 'wishlist' && <WishlistPage onOpenDetail={openDetail} onBrowse={() => setView('catalog')} />}
        {view === 'writers' && <WritersPage onOpenDetail={openDetail} />}
      </main>

      <Footer />

      <BookDetailModal
        key={selectedBookId || 'none'}
        bookId={selectedBookId}
        onClose={() => setSelectedBookId(null)}
        onSelectBook={openDetail}
      />
      <CartDrawer onProceedToCheckout={() => setIsCheckoutOpen(true)} />
      <CheckoutModal
        isOpen={isCheckoutOpen}
        onClose={() => setIsCheckoutOpen(false)}
        onOrderSuccess={setOrderConfirmation}
        onOpenWallet={() => setIsWalletOpen(true)}
      />
      <OrderConfirmationModal
        confirmation={orderConfirmation}
        onClose={() => setOrderConfirmation(null)}
        onViewHistory={() => setView('orders')}
      />
      <GiftPointsModal isOpen={isPointsOpen} onClose={() => setIsPointsOpen(false)} />
      <WalletModal isOpen={isWalletOpen} onClose={() => setIsWalletOpen(false)} />
      <AuthModal />
    </div>
  );
}

export default function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <CartProvider>
          <WishlistProvider>
            <BookstoreMain />
          </WishlistProvider>
        </CartProvider>
      </AuthProvider>
    </ToastProvider>
  );
}
