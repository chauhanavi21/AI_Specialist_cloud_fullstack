import React from 'react';
import { BookOpen, ShoppingBag, Gift, History, User, LogOut, Search } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

export default function Navbar({
  searchQuery,
  setSearchQuery,
  onOpenHistory,
  onOpenPoints,
}) {
  const { user, isAuthenticated, logout, openAuthModal } = useAuth();
  const { cart, setIsCartOpen } = useCart();

  // text-base on small screens avoids iOS zoom-on-focus
  const searchInputClass =
    'w-full pl-9 pr-4 py-2 text-base sm:text-sm bg-brand-50 border border-brand-100 rounded-md focus:bg-white focus:border-brand-500 focus:outline-none';

  return (
    <header className="sticky top-0 z-30 bg-white border-b border-brand-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-14 sm:h-16 gap-2 sm:gap-4">
          {/* Logo */}
          <div
            className="flex items-center gap-2 cursor-pointer shrink-0"
            onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
          >
            <BookOpen className="size-6 text-brand-600" />
            <span className="hidden min-[420px]:inline text-lg font-semibold text-slate-900 whitespace-nowrap">
              Bob <span className="text-brand-600">Bookstore</span>
            </span>
          </div>

          {/* Search */}
          <div className="flex-1 max-w-xl mx-2 hidden sm:block">
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search by title, author or keyword"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className={searchInputClass}
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600"
                >
                  Clear
                </button>
              )}
            </div>
          </div>

          {/* Actions */}
          <div className="flex items-center gap-1 sm:gap-3 shrink-0">
            {isAuthenticated ? (
              <>
                <button
                  onClick={onOpenPoints}
                  className="flex items-center gap-1.5 px-2.5 sm:px-3 py-2 sm:py-1.5 border border-brand-200 text-brand-700 hover:bg-brand-50 rounded-md text-sm whitespace-nowrap"
                  title="Gift points"
                >
                  <Gift className="w-4 h-4" />
                  <span>{user.giftPointsBalance || 0} pts</span>
                </button>

                <button
                  onClick={onOpenHistory}
                  className="p-2.5 sm:p-2 text-slate-600 hover:text-brand-600 hover:bg-brand-50 rounded-md"
                  title="Orders"
                >
                  <History className="w-5 h-5" />
                </button>

                <div className="flex items-center gap-1 sm:gap-2 sm:pl-3 sm:border-l border-brand-100">
                  <span className="hidden lg:block text-sm text-slate-700 max-w-[10rem] truncate">{user.fullName}</span>
                  <button
                    onClick={logout}
                    className="p-2.5 sm:p-2 text-slate-400 hover:text-slate-700 hover:bg-brand-50 rounded-md"
                    title="Sign out"
                  >
                    <LogOut className="w-4 h-4" />
                  </button>
                </div>
              </>
            ) : (
              <button
                onClick={() => openAuthModal('login')}
                className="flex items-center gap-1.5 px-3 sm:px-4 py-2 text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 rounded-md whitespace-nowrap"
              >
                <User className="w-4 h-4" />
                <span>Sign in</span>
              </button>
            )}

            <button
              onClick={() => setIsCartOpen(true)}
              className="relative p-2.5 sm:p-2 text-slate-700 hover:bg-brand-50 rounded-md"
              title="Basket"
            >
              <ShoppingBag className="w-5 h-5" />
              {cart.itemCount > 0 && (
                <span className="absolute -top-1 -right-1 bg-brand-600 text-white text-[11px] font-medium w-5 h-5 rounded-full flex items-center justify-center">
                  {cart.itemCount}
                </span>
              )}
            </button>
          </div>
        </div>

        {/* Mobile search */}
        <div className="pb-3 sm:hidden">
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search books"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className={searchInputClass}
            />
          </div>
        </div>
      </div>
    </header>
  );
}
