import React, { useState, useEffect } from 'react';
import { X, Star } from 'lucide-react';
import { api } from '../api/client';
import { useCart } from '../context/CartContext';

export default function BookDetailModal({ bookId, onClose, onSelectBook }) {
  const [book, setBook] = useState(null);
  const [relatedBooks, setRelatedBooks] = useState([]);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const { addToCart } = useCart();

  useEffect(() => {
    if (!bookId) return;
    setLoading(true);
    setQuantity(1);

    Promise.all([
      api.getBookById(bookId),
      api.getRelatedBooks(bookId, 4)
    ])
      .then(([detail, related]) => {
        setBook(detail);
        setRelatedBooks(related || []);
      })
      .catch((err) => {
        console.error('Failed to load book detail', err);
      })
      .finally(() => setLoading(false));
  }, [bookId]);

  if (!bookId) return null;

  const handleAddToCart = () => {
    if (!book) return;
    addToCart(book.id, quantity);
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/40 flex items-end sm:items-center justify-center p-0 sm:p-4 overflow-y-auto">
      <div className="relative bg-white rounded-t-xl sm:rounded-lg max-w-3xl w-full overflow-hidden sm:my-8 max-h-[95dvh] sm:max-h-[90dvh] flex flex-col">
        <button
          onClick={onClose}
          className="absolute top-3 right-3 z-10 p-2 text-slate-500 hover:bg-brand-50 rounded-md"
        >
          <X className="w-5 h-5" />
        </button>

        {loading ? (
          <div className="p-16 text-center text-sm text-slate-500">Loading...</div>
        ) : book ? (
          <div className="overflow-y-auto p-4 sm:p-8 space-y-6 sm:space-y-8">
            <div className="grid grid-cols-1 sm:grid-cols-12 gap-5 sm:gap-6">
              <div className="sm:col-span-5">
                <div className="aspect-[3/4] max-w-[14rem] mx-auto sm:max-w-none rounded overflow-hidden bg-brand-50 border border-brand-100">
                  <img
                    src={book.coverImageUrl}
                    alt={book.title}
                    className="w-full h-full object-cover"
                  />
                </div>
              </div>

              <div className="sm:col-span-7 flex flex-col justify-between">
                <div>
                  <p className="text-xs text-brand-600 mb-1">
                    {book.category?.name || book.categoryName}
                    <span className="text-slate-400"> · </span>
                    <span className="text-slate-500">{book.publisher?.name || book.publisherName}</span>
                  </p>

                  <h2 className="text-xl sm:text-2xl font-semibold text-slate-900 leading-tight mb-1 pr-8">
                    {book.title}
                  </h2>
                  <p className="text-sm text-slate-600 mb-3">by {book.author}</p>

                  <div className="flex items-center gap-2 mb-4 text-sm text-slate-600">
                    <Star className="w-4 h-4 fill-amber-400 text-amber-400" />
                    <span>{book.rating?.toFixed(1)}</span>
                    <span className="text-slate-400">({book.reviewCount} reviews)</span>
                    <span className="text-slate-300">|</span>
                    <span>{book.stockQuantity > 0 ? `${book.stockQuantity} in stock` : 'Out of stock'}</span>
                  </div>

                  <div className="mb-4">
                    <div className="flex items-baseline gap-2">
                      <span className="text-2xl font-semibold text-slate-900">${book.price?.toFixed(2)}</span>
                      {book.originalPrice > book.price && (
                        <span className="text-sm text-slate-400 line-through">
                          ${book.originalPrice?.toFixed(2)}
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-slate-600 mt-1">
                      Estimated delivery: {book.tentativeDeliveryDate || `in ${book.tentativeDeliveryDays} days`}
                    </p>
                  </div>

                  <p className="text-xs text-slate-500 mb-6">
                    {book.pageCount} pages · Published {book.publishedYear}
                  </p>
                </div>

                <div className="flex flex-col min-[420px]:flex-row min-[420px]:items-center gap-3 pt-4 border-t border-brand-100">
                  <div className="flex items-center justify-between min-[420px]:justify-start border border-brand-200 rounded-md">
                    <button
                      onClick={() => setQuantity(Math.max(1, quantity - 1))}
                      className="size-10 sm:size-9 flex items-center justify-center text-slate-600 hover:bg-brand-50"
                    >
                      -
                    </button>
                    <span className="w-8 text-center text-sm text-slate-800">{quantity}</span>
                    <button
                      onClick={() => setQuantity(Math.min(book.stockQuantity || 10, quantity + 1))}
                      className="size-10 sm:size-9 flex items-center justify-center text-slate-600 hover:bg-brand-50"
                    >
                      +
                    </button>
                  </div>

                  <button
                    onClick={handleAddToCart}
                    className="min-[420px]:flex-1 py-3 sm:py-2.5 px-5 bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium rounded-md"
                  >
                    Add to basket · ${(book.price * quantity).toFixed(2)}
                  </button>
                </div>
              </div>
            </div>

            <div>
              <h4 className="text-sm font-semibold text-slate-900 mb-1">About this book</h4>
              <p className="text-sm text-slate-700 leading-relaxed">{book.description}</p>
              <p className="mt-3 text-xs text-slate-500">
                Orders can be cancelled for a full refund within 48 hours.
              </p>
            </div>

            {relatedBooks.length > 0 && (
              <div>
                <h4 className="text-sm font-semibold text-slate-900 mb-3">Related books</h4>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  {relatedBooks.map((rel) => (
                    <div
                      key={rel.id}
                      onClick={() => onSelectBook(rel)}
                      className="p-2 bg-white rounded-lg border border-brand-100 hover:border-brand-300 cursor-pointer group"
                    >
                      <div className="aspect-[3/4] rounded overflow-hidden bg-brand-50 mb-2">
                        <img src={rel.coverImageUrl} alt={rel.title} className="w-full h-full object-cover" />
                      </div>
                      <h5 className="text-xs font-medium text-slate-900 line-clamp-1 group-hover:text-brand-600">
                        {rel.title}
                      </h5>
                      <p className="text-[11px] text-slate-500 line-clamp-1">{rel.author}</p>
                      <span className="text-xs font-semibold text-slate-900">${rel.price.toFixed(2)}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        ) : null}
      </div>
    </div>
  );
}
