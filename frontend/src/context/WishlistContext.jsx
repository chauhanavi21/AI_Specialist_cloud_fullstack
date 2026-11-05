import React, { createContext, useContext, useEffect, useState } from 'react';
import { api } from '../api/client';
import { useAuth } from './AuthContext';
import { useToast } from './ToastContext';

const WishlistContext = createContext(null);

/** Tracks which books the signed-in customer has wishlisted so every heart icon stays in sync. */
export function WishlistProvider({ children }) {
  const { user, openAuthModal } = useAuth();
  const toast = useToast();
  const [books, setBooks] = useState([]);

  const refresh = async () => {
    if (!user) {
      setBooks([]);
      return;
    }
    try {
      setBooks((await api.getWishlist()) || []);
    } catch (err) {
      console.error('Failed to load wishlist', err);
    }
  };

  useEffect(() => {
    refresh();
  }, [user]);

  const has = (bookId) => books.some((b) => b.id === bookId);

  const toggle = async (book) => {
    if (!user) {
      openAuthModal('login');
      return;
    }
    const wasWishlisted = has(book.id);
    // Optimistic update, rolled back if the request fails.
    setBooks((list) => (wasWishlisted ? list.filter((b) => b.id !== book.id) : [book, ...list]));
    try {
      if (wasWishlisted) {
        await api.removeFromWishlist(book.id);
        toast.success('Removed from your wishlist');
      } else {
        await api.addToWishlist(book.id);
        toast.success('Added to your wishlist');
      }
    } catch (err) {
      toast.error(err.message || 'Could not update your wishlist');
      refresh();
    }
  };

  return (
    <WishlistContext.Provider value={{ books, has, toggle, refresh }}>
      {children}
    </WishlistContext.Provider>
  );
}

export const useWishlist = () => useContext(WishlistContext);
