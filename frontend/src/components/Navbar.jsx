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

  return (
    <header className="sticky top-0 z-30 bg-white border-b border-gray-200 shadow-sm">
      {/* Top Notification Bar */}
      <div className="bg-ibm-darkBlue text-white text-xs py-1.5 px-4 text-center font-medium tracking-wide flex items-center justify-center gap-2">
        <span className="bg-ibm-accent text-slate-900 text-[10px] font-bold px-1.5 py-0.5 rounded">IBM BOB</span>
        <span>AI Specialist Capstone — Cloud Native E-Bookstore Platform | 48h Free Order Cancellation Guarantee</span>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 gap-4">
          {/* Logo & Brand */}
          <div className="flex items-center gap-3 cursor-pointer" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
            <div className="w-10 h-10 rounded-lg bg-ibm-blue flex items-center justify-center text-white shadow-md">
              <BookOpen className="w-6 h-6" />
            </div>
            <div>
              <span className="text-lg font-bold tracking-tight text-gray-900 block leading-tight">
                Bob <span className="text-ibm-blue">Bookstore</span>
              </span>
              <span className="text-[11px] text-gray-500 font-medium">Cloud & AI Engineering</span>
            </div>
          </div>

          {/* Search Bar */}
          <div className="flex-1 max-w-xl mx-2 hidden sm:block">
            <div className="relative">
              <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search books by title, author, or keywords..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-4 py-2 text-sm bg-gray-100 border border-transparent rounded-full focus:bg-white focus:border-ibm-blue focus:outline-none transition"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-gray-400 hover:text-gray-600"
                >
                  Clear
                </button>
              )}
            </div>
          </div>

          {/* Actions & Profile */}
          <div className="flex items-center gap-2 sm:gap-3">
            {isAuthenticated ? (
              <>
                {/* Gift Points Button */}
                <button
                  onClick={onOpenPoints}
                  className="flex items-center gap-1.5 px-3 py-1.5 bg-amber-50 hover:bg-amber-100 border border-amber-200 text-amber-900 rounded-full text-xs font-semibold transition shadow-sm"
                  title="View Loyalty Gift Points"
                >
                  <Gift className="w-3.5 h-3.5 text-amber-600" />
                  <span>{user.giftPointsBalance || 0} pts</span>
                </button>

                {/* Order History */}
                <button
                  onClick={onOpenHistory}
                  className="p-2 text-gray-600 hover:text-ibm-blue hover:bg-gray-100 rounded-lg transition"
                  title="Order History & Buy It Again"
                >
                  <History className="w-5 h-5" />
                </button>

                {/* User Info & Logout */}
                <div className="flex items-center gap-2 pl-2 border-l border-gray-200">
                  <div className="hidden md:block text-right">
                    <span className="text-xs font-semibold text-gray-800 block leading-tight">{user.fullName}</span>
                    <span className="text-[10px] text-gray-500">{user.email}</span>
                  </div>
                  <button
                    onClick={logout}
                    className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
                    title="Sign Out"
                  >
                    <LogOut className="w-4 h-4" />
                  </button>
                </div>
              </>
            ) : (
              <button
                onClick={() => openAuthModal('login')}
                className="flex items-center gap-1.5 px-4 py-2 text-sm font-semibold text-white bg-ibm-blue hover:bg-ibm-darkBlue rounded-lg transition shadow-sm"
              >
                <User className="w-4 h-4" />
                <span>Sign In</span>
              </button>
            )}

            {/* Shopping Cart Drawer Trigger */}
            <button
              onClick={() => setIsCartOpen(true)}
              className="relative p-2.5 bg-gray-100 hover:bg-gray-200 text-gray-800 rounded-lg transition flex items-center justify-center ml-1"
              title="View Shopping Basket"
            >
              <ShoppingBag className="w-5 h-5" />
              {cart.itemCount > 0 && (
                <span className="absolute -top-1.5 -right-1.5 bg-ibm-blue text-white text-[11px] font-bold w-5 h-5 rounded-full flex items-center justify-center shadow">
                  {cart.itemCount}
                </span>
              )}
            </button>
          </div>
        </div>

        {/* Mobile Search input */}
        <div className="pb-3 sm:hidden">
          <div className="relative">
            <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search books..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-4 py-2 text-sm bg-gray-100 border border-transparent rounded-full focus:bg-white focus:border-ibm-blue focus:outline-none transition"
            />
          </div>
        </div>
      </div>
    </header>
  );
}
