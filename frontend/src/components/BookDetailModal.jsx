import React, { useState, useEffect, useCallback } from 'react';
import { Heart, Star, ShoppingCart, UserPlus, UserCheck } from 'lucide-react';
import Modal, { ModalHeader } from './Modal';
import BookCover from './BookCover';
import StarRating from './StarRating';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useWishlist } from '../context/WishlistContext';
import { useToast } from '../context/ToastContext';
import { deliveryBy, fullDate, inr, plural } from '../lib/format';

const REVIEW_LIMIT = 100;

function initials(name) {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join('');
}

export default function BookDetailModal({ bookId, onClose, onSelectBook }) {
  const { user, openAuthModal } = useAuth();
  const { addToCart } = useCart();
  const wishlist = useWishlist();
  const toast = useToast();

  const [book, setBook] = useState(null);
  const [related, setRelated] = useState([]);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [following, setFollowing] = useState(false);

  const [reviewRating, setReviewRating] = useState(0);
  const [reviewText, setReviewText] = useState('');
  const [reviewError, setReviewError] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);

  const load = useCallback(() => {
    if (!bookId) return Promise.resolve();
    return Promise.all([api.getBookById(bookId), api.getRelatedBooks(bookId, 3)])
      .then(([detail, rel]) => {
        setBook(detail);
        setRelated(rel || []);
        setFollowing(!!detail.writer?.following);
        setLoadError('');
      })
      .catch((err) => setLoadError(err.message || 'Could not load this book'));
  }, [bookId]);

  useEffect(() => {
    if (!bookId) return;
    setLoading(true);
    setQuantity(1);
    setReviewRating(0);
    setReviewText('');
    setReviewError('');
    load().finally(() => setLoading(false));
  }, [bookId, user, load]);

  if (!bookId) return null;

  const wished = book ? wishlist.has(book.id) : false;

  const toggleFollow = async () => {
    if (!user) return openAuthModal('login');
    if (!book?.writer?.id) return undefined;
    const next = !following;
    setFollowing(next);
    try {
      if (next) await api.followWriter(book.writer.id);
      else await api.unfollowWriter(book.writer.id);
      toast.success(next ? `Following ${book.writer.name}` : `Unfollowed ${book.writer.name}`);
    } catch (err) {
      setFollowing(!next);
      toast.error(err.message || 'Could not update follow');
    }
    return undefined;
  };

  const submitReview = async (e) => {
    e.preventDefault();
    if (!user) return openAuthModal('login');
    if (reviewRating < 1) return setReviewError('Choose a star rating');
    if (!reviewText.trim()) return setReviewError('Write a short review');
    setSubmittingReview(true);
    setReviewError('');
    try {
      await api.addReview(book.id, { rating: reviewRating, comment: reviewText.trim() });
      toast.success('Thanks for your review');
      setReviewRating(0);
      setReviewText('');
      await load();
    } catch (err) {
      setReviewError(err.message || 'Could not post your review');
    } finally {
      setSubmittingReview(false);
    }
    return undefined;
  };

  const alreadyReviewed = !!user && !!book?.reviews?.some((r) => r.userName === user.fullName);

  return (
    <Modal open onClose={onClose} title={book?.title || 'Book details'} size="xl">
      <ModalHeader
        title={book?.title || 'Book details'}
        subtitle={book ? `${book.categoryName} · ${book.publisherName}` : undefined}
        onClose={onClose}
      />

      {loading ? (
        <div className="p-16 text-center text-sm text-fg3" role="status">Loading...</div>
      ) : loadError ? (
        <div className="p-10 text-center space-y-3">
          <p className="text-sm text-danger">{loadError}</p>
          <button onClick={() => { setLoading(true); load().finally(() => setLoading(false)); }} className="btn-secondary">
            Try again
          </button>
        </div>
      ) : book ? (
        <div className="overflow-y-auto grid grid-cols-1 lg:grid-cols-[1fr_17rem]">
          <div className="p-5 sm:p-6 space-y-8 min-w-0">
            <div className="grid grid-cols-1 sm:grid-cols-[13rem_1fr] gap-6">
              <div className="aspect-[3/4] max-w-[13rem] w-full mx-auto sm:mx-0 border border-line">
                <BookCover book={book} />
              </div>

              <div>
                <p className="text-sm text-fg2">
                  by <span className="text-link">{book.writer?.name || book.author}</span>
                </p>
                <p className="text-sm text-fg3 mt-2">{book.description}</p>
                <p className="text-xs text-fg3 mt-2">
                  Published by: <span className="text-link">{book.publisherName}</span>
                </p>
                <p className="text-xs text-fg2 mt-3">
                  {book.format}
                  <span className="text-fg3"> · </span>
                  <span className="text-link">{book.categoryName}</span>
                </p>

                <div className="mt-4">
                  <div className="flex items-baseline gap-2">
                    <span className="text-2xl font-semibold text-fg">{inr(book.price)}</span>
                    {book.originalPrice > book.price && (
                      <span className="text-sm text-fg3 line-through">{inr(book.originalPrice)}</span>
                    )}
                  </div>
                  <p className="text-xs text-fg3 mt-1">
                    {book.inStock ? deliveryBy(book) : 'Out of stock'}
                    {book.inStock && book.stockQuantity <= 10 && (
                      <span className="text-warn"> · only {book.stockQuantity} left</span>
                    )}
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-2 mt-4">
                  <div className="flex items-center bg-layer2" role="group" aria-label="Quantity">
                    <button
                      onClick={() => setQuantity(Math.max(1, quantity - 1))}
                      aria-label="Decrease quantity"
                      className="size-10 hover:bg-[#4c4c4c]"
                    >
                      −
                    </button>
                    <span className="w-8 text-center text-sm" aria-live="polite">{quantity}</span>
                    <button
                      onClick={() => setQuantity(Math.min(book.stockQuantity || 1, quantity + 1))}
                      aria-label="Increase quantity"
                      className="size-10 hover:bg-[#4c4c4c]"
                    >
                      +
                    </button>
                  </div>

                  <button
                    onClick={() => addToCart(book.id, quantity)}
                    disabled={!book.inStock}
                    className="btn-primary"
                  >
                    Add to Cart
                    <ShoppingCart className="w-4 h-4" aria-hidden="true" />
                  </button>
                  <button
                    onClick={() => wishlist.toggle(book)}
                    aria-pressed={wished}
                    className="btn-secondary"
                  >
                    {wished ? 'In Wishlist' : 'Add to Wishlist'}
                    <Heart className={`w-4 h-4 ${wished ? 'fill-danger text-danger' : ''}`} aria-hidden="true" />
                  </button>
                </div>

                <dl className="grid grid-cols-3 gap-3 mt-6 text-xs">
                  <div>
                    <dt className="text-fg3">Language</dt>
                    <dd className="text-link mt-0.5">{book.language}</dd>
                  </div>
                  <div>
                    <dt className="text-fg3">Rating</dt>
                    <dd className="mt-0.5 flex items-center gap-1 text-fg">
                      <Star className="w-3.5 h-3.5 fill-warn text-warn" aria-hidden="true" />
                      {Number(book.rating).toFixed(1)}
                      <span className="text-fg3">({book.reviewCount})</span>
                    </dd>
                  </div>
                  <div>
                    <dt className="text-fg3">Sells</dt>
                    <dd className="text-fg mt-0.5">{book.copiesSold.toLocaleString('en-IN')} copies sold</dd>
                  </div>
                </dl>
                <p className="text-xs text-fg3 mt-3">
                  {book.pageCount} pages · launched {fullDate(book.launchDate)} · cancel within 48 hours for a full refund
                </p>
              </div>
            </div>

            <section aria-label="About the writer">
              <h3 className="text-sm font-semibold text-fg mb-3">About the writer</h3>
              <div className="flex gap-4">
                <div
                  className="size-16 shrink-0 rounded-full bg-layer2 text-link flex items-center justify-center text-lg font-semibold"
                  aria-hidden="true"
                >
                  {initials(book.writer?.name || book.author)}
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-fg">{book.writer?.name || book.author}</p>
                  {book.writer?.bio && <p className="text-xs text-fg3 mt-1">{book.writer.bio}</p>}
                  {book.writer?.id && (
                    <button onClick={toggleFollow} aria-pressed={following} className="btn-secondary mt-3 min-h-[2.25rem]">
                      {following ? <UserCheck className="w-4 h-4" aria-hidden="true" /> : <UserPlus className="w-4 h-4" aria-hidden="true" />}
                      {following ? 'Following' : 'Follow writer'}
                    </button>
                  )}
                </div>
              </div>
            </section>

            <section aria-label="Reviews">
              <h3 className="text-sm font-semibold text-fg mb-3">Reviews</h3>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <form onSubmit={submitReview} noValidate>
                  {alreadyReviewed ? (
                    <p className="text-xs text-fg3">You have already reviewed this book.</p>
                  ) : (
                    <>
                      <label htmlFor="review-text" className="field-label">Leave your review</label>
                      <textarea
                        id="review-text"
                        rows={4}
                        maxLength={REVIEW_LIMIT}
                        value={reviewText}
                        onChange={(e) => { setReviewText(e.target.value); setReviewError(''); }}
                        placeholder={user ? 'What did you think?' : 'Sign in to leave a review'}
                        aria-invalid={!!reviewError}
                        aria-describedby="review-error"
                        className="field resize-none"
                      />
                      <div className="flex justify-between text-[11px] text-fg3 mt-1">
                        <span id="review-error" className="field-error mt-0" role="alert">{reviewError}</span>
                        <span>{reviewText.length}/{REVIEW_LIMIT}</span>
                      </div>
                      <div className="flex items-center justify-between mt-2">
                        <StarRating value={reviewRating} onChange={setReviewRating} />
                        <button type="submit" disabled={submittingReview} className="btn-primary min-h-[2.25rem]">
                          {submittingReview ? 'Posting...' : 'Submit'}
                        </button>
                      </div>
                    </>
                  )}
                </form>

                <div className="space-y-4 max-h-64 overflow-y-auto pr-1">
                  {book.reviews.length === 0 ? (
                    <p className="text-xs text-fg3">No reviews yet. Be the first to share what you thought.</p>
                  ) : (
                    book.reviews.map((r) => (
                      <div key={r.id}>
                        <p className="text-xs font-semibold text-fg">{r.userName}</p>
                        <p className="text-xs text-fg2 mt-1">{r.comment}</p>
                        <StarRating value={r.rating} readOnly size="sm" />
                      </div>
                    ))
                  )}
                </div>
              </div>
            </section>
          </div>

          <aside className="border-t lg:border-t-0 lg:border-l border-line p-5 sm:p-6" aria-label="Related reads">
            <h3 className="text-sm font-semibold text-fg mb-3">Related Reads</h3>
            {related.length === 0 ? (
              <p className="text-xs text-fg3">No related books yet.</p>
            ) : (
              <ul className="space-y-4">
                {related.map((rel) => (
                  <li key={rel.id}>
                    <button onClick={() => onSelectBook(rel)} className="flex gap-3 text-left w-full group">
                      <div className="w-16 shrink-0 aspect-[3/4]"><BookCover book={rel} compact /></div>
                      <div className="min-w-0">
                        <p className="text-xs font-semibold text-fg line-clamp-2 group-hover:text-link">{rel.title}</p>
                        <p className="text-[11px] text-link truncate">by {rel.author}</p>
                        <p className="text-[11px] text-fg3 mt-1">{rel.format}</p>
                        <p className="text-xs font-semibold text-fg mt-1">{inr(rel.price)}</p>
                        <p className="text-[10px] text-fg3">{deliveryBy(rel)}</p>
                      </div>
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <p className="text-[11px] text-fg3 mt-6">{plural(book.reviewCount, 'review')} · {book.language}</p>
          </aside>
        </div>
      ) : null}
    </Modal>
  );
}
