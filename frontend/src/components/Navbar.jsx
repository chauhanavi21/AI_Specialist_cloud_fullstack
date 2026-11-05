import React from 'react';
import { BookOpen, ShoppingBag, Gift, User, LogOut, Search, Wallet, Package, Heart, PenLine } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { inr } from '../lib/format';

export default function Navbar({
  view,
  onNavigate,
  searchQuery,
  setSearchQuery,
  onOpenPoints,
  onOpenWallet,
}) {
  const { user, isAuthenticated, logout, openAuthModal } = useAuth();
  const { cart, setIsCartOpen } = useCart();

  const links = [
    { id: 'catalog', label: 'Book Worm', brand: true },
    { id: 'orders', label: 'My Orders' },
    { id: 'wishlist', label: 'My Wishlist' },
    { id: 'writers', label: 'My Writers' },
  ];

  // Signed-out visitors can open any page; account pages explain they need a sign-in.
  const linkClass = (active) =>
    `px-2 sm:px-3 h-full inline-flex items-center text-[13px] sm:text-sm border-b-2 whitespace-nowrap shrink-0 ${
      active ? 'border-brand text-fg' : 'border-transparent text-fg2 hover:text-fg hover:bg-layer2'
    }`;

  const tabs = links.filter((l) => !l.brand);

  return (
    <>
    <header className="sticky top-0 z-30 bg-canvas border-b border-line">
      <div className="max-w-[88rem] mx-auto px-3 sm:px-6">
        <div className="flex items-stretch justify-between h-14 gap-1">
          <div className="sm:hidden flex items-center gap-2 text-fg font-semibold">
            <BookOpen className="w-5 h-5 text-link" aria-hidden="true" />
            Book Worm
          </div>
          <nav aria-label="Main" className="hidden sm:flex items-stretch min-w-0">
            {links.map((link) =>
              link.brand ? (
                <button
                  key={link.id}
                  onClick={() => {
                    onNavigate('catalog');
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                  }}
                  className="flex items-center gap-2 pr-2 sm:pr-4 sm:mr-1 text-fg font-semibold whitespace-nowrap shrink-0"
                  aria-label="Book Worm home"
                >
                  <BookOpen className="w-5 h-5 text-link" aria-hidden="true" />
                  <span>Book Worm</span>
                </button>
              ) : (
                <button
                  key={link.id}
                  onClick={() => onNavigate(link.id)}
                  aria-current={view === link.id ? 'page' : undefined}
                  className={linkClass(view === link.id)}
                >
                  {link.label}
                </button>
              ),
            )}
          </nav>

          <div className="flex items-center sm:gap-2 shrink-0">
            {isAuthenticated ? (
              <>
                <button
                  onClick={onOpenWallet}
                  className="inline-flex items-center gap-1.5 p-2 sm:px-2.5 sm:py-1.5 text-sm text-fg2 hover:text-fg hover:bg-layer2"
                  title="Wallet"
                  aria-label={`Wallet, ${inr(user.walletBalance || 0)}`}
                >
                  <Wallet className="w-4 h-4" aria-hidden="true" />
                  <span className="hidden sm:inline">{inr(user.walletBalance || 0)}</span>
                </button>
                <button
                  onClick={onOpenPoints}
                  className="inline-flex items-center gap-1.5 p-2 sm:px-2.5 sm:py-1.5 text-sm text-fg2 hover:text-fg hover:bg-layer2"
                  title="Gift points"
                  aria-label={`Gift points, ${user.giftPointsBalance || 0}`}
                >
                  <Gift className="w-4 h-4" aria-hidden="true" />
                  <span className="hidden sm:inline">{user.giftPointsBalance || 0} pts</span>
                </button>
                <button
                  onClick={logout}
                  aria-label="Sign out"
                  title={`Sign out ${user.fullName}`}
                  className="p-2 sm:p-2.5 text-fg2 hover:text-fg hover:bg-layer2"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </>
            ) : (
              <button onClick={() => openAuthModal('login')} className="btn-primary min-h-[2.25rem]">
                <User className="w-4 h-4" aria-hidden="true" />
                <span>Sign in</span>
              </button>
            )}

            <button
              onClick={() => setIsCartOpen(true)}
              aria-label={`Basket, ${cart.itemCount} items`}
              className="relative p-2 sm:p-2.5 text-fg hover:bg-layer2"
            >
              <ShoppingBag className="w-5 h-5" />
              {cart.itemCount > 0 && (
                <span className="absolute top-0.5 right-0 bg-brand text-white text-[11px] font-medium min-w-5 h-5 px-1 rounded-full flex items-center justify-center">
                  {cart.itemCount}
                </span>
              )}
            </button>
          </div>
        </div>

        {view === 'catalog' && (
          <div className="pb-3 md:hidden">
            <label className="relative block">
              <span className="sr-only">Search books</span>
              <Search className="w-4 h-4 text-fg3 absolute left-3 top-1/2 -translate-y-1/2" aria-hidden="true" />
              <input
                type="search"
                placeholder="Search by title, author or keyword"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="field pl-9"
              />
            </label>
          </div>
        )}
      </div>
    </header>

    <nav
      aria-label="Account pages"
      className="sm:hidden fixed bottom-0 inset-x-0 z-30 bg-canvas border-t border-line grid grid-cols-4 pb-[env(safe-area-inset-bottom)]"
    >
      <button
        onClick={() => { onNavigate('catalog'); window.scrollTo({ top: 0 }); }}
        aria-current={view === 'catalog' ? 'page' : undefined}
        className={`py-2.5 text-xs flex flex-col items-center gap-1 ${view === 'catalog' ? 'text-fg border-t-2 border-brand -mt-px' : 'text-fg2'}`}
      >
        <BookOpen className="w-5 h-5" aria-hidden="true" />
        Books
      </button>
      {tabs.map((t) => (
        <button
          key={t.id}
          onClick={() => onNavigate(t.id)}
          aria-current={view === t.id ? 'page' : undefined}
          className={`py-2.5 text-xs flex flex-col items-center gap-1 ${view === t.id ? 'text-fg border-t-2 border-brand -mt-px' : 'text-fg2'}`}
        >
          {t.id === 'orders' && <Package className="w-5 h-5" aria-hidden="true" />}
          {t.id === 'wishlist' && <Heart className="w-5 h-5" aria-hidden="true" />}
          {t.id === 'writers' && <PenLine className="w-5 h-5" aria-hidden="true" />}
          {t.label}
        </button>
      ))}
    </nav>
    </>
  );
}
