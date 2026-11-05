import React from 'react';
import BookCard from '../components/BookCard';
import { useAuth } from '../context/AuthContext';
import { useWishlist } from '../context/WishlistContext';

export default function WishlistPage({ onOpenDetail, onBrowse }) {
  const { user, openAuthModal } = useAuth();
  const wishlist = useWishlist();

  if (!user) {
    return (
      <div className="panel p-10 text-center">
        <p className="text-fg">Sign in to see your wishlist.</p>
        <button onClick={() => openAuthModal('login')} className="btn-primary mt-4">Sign in</button>
      </div>
    );
  }

  return (
    <section aria-labelledby="wishlist-title">
      <h1 id="wishlist-title" className="text-xl font-semibold mb-1">My Wishlist</h1>
      <p className="text-xs text-fg3 mb-5">Books you have saved for later.</p>

      {wishlist.books.length === 0 ? (
        <div className="panel p-10 text-center">
          <p className="text-fg">Your wishlist is empty</p>
          <p className="text-sm text-fg3 mt-1">Tap the heart on any book to save it here.</p>
          <button onClick={onBrowse} className="btn-primary mt-4">Browse books</button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
          {wishlist.books.map((book) => (
            <BookCard key={book.id} book={book} onOpenDetail={onOpenDetail} />
          ))}
        </div>
      )}
    </section>
  );
}
