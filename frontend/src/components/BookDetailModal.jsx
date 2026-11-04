import React, { useState, useEffect } from 'react';
import { X, Star, Truck, ShieldCheck, ShoppingBag, BookOpen, Building2, Calendar, FileText, ChevronRight } from 'lucide-react';
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
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto animate-fadeIn">
      <div className="relative bg-white rounded-3xl max-w-3xl w-full shadow-2xl overflow-hidden my-8 max-h-[90vh] flex flex-col">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-10 p-2 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-full transition"
        >
          <X className="w-5 h-5" />
        </button>

        {loading ? (
          <div className="p-16 text-center">
            <div className="w-10 h-10 border-4 border-ibm-blue border-t-transparent rounded-full animate-spin mx-auto mb-4" />
            <p className="text-sm text-gray-500 font-medium">Loading book details...</p>
          </div>
        ) : book ? (
          <div className="overflow-y-auto p-6 sm:p-8 space-y-8">
            {/* Top Grid: Image & Core Info */}
            <div className="grid grid-cols-1 sm:grid-cols-12 gap-6">
              {/* Cover Image */}
              <div className="sm:col-span-5">
                <div className="aspect-[3/4] rounded-2xl overflow-hidden bg-gray-100 shadow-md border border-gray-200">
                  <img
                    src={book.coverImageUrl}
                    alt={book.title}
                    className="w-full h-full object-cover"
                  />
                </div>
              </div>

              {/* Details & Actions */}
              <div className="sm:col-span-7 flex flex-col justify-between">
                <div>
                  <div className="flex items-center gap-2 mb-2">
                    <span className="px-2.5 py-0.5 rounded-md text-xs font-bold bg-blue-100 text-ibm-blue">
                      {book.category?.name || book.categoryName}
                    </span>
                    <span className="text-xs text-gray-500 font-medium flex items-center gap-1">
                      <Building2 className="w-3.5 h-3.5" />
                      {book.publisher?.name || book.publisherName}
                    </span>
                  </div>

                  <h2 className="text-xl sm:text-2xl font-extrabold text-gray-900 leading-tight mb-2">
                    {book.title}
                  </h2>

                  <p className="text-sm text-gray-600 mb-3 font-medium">By {book.author}</p>

                  <div className="flex items-center gap-2 mb-4">
                    <div className="flex items-center text-amber-500">
                      <Star className="w-4 h-4 fill-current" />
                    </div>
                    <span className="text-sm font-bold text-gray-900">{book.rating?.toFixed(1)}</span>
                    <span className="text-xs text-gray-400">({book.reviewCount} customer reviews)</span>
                    <span className="text-gray-300">|</span>
                    <span className="text-xs font-semibold text-emerald-600">
                      {book.stockQuantity > 0 ? `${book.stockQuantity} in stock` : 'Out of stock'}
                    </span>
                  </div>

                  {/* Price Block */}
                  <div className="bg-gray-50 border border-gray-200 rounded-2xl p-4 mb-4 flex items-center justify-between">
                    <div>
                      <span className="text-xs text-gray-500 block">Special Price</span>
                      <div className="flex items-baseline gap-2">
                        <span className="text-2xl font-black text-gray-900">${book.price?.toFixed(2)}</span>
                        {book.originalPrice > book.price && (
                          <span className="text-xs text-gray-400 line-through">
                            ${book.originalPrice?.toFixed(2)}
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Tentative Delivery Date Callout */}
                    <div className="text-right">
                      <div className="inline-flex items-center gap-1 text-xs font-bold text-ibm-blue bg-blue-50 px-2.5 py-1 rounded-lg border border-blue-200">
                        <Truck className="w-3.5 h-3.5 text-ibm-blue" />
                        <span>Estimated Delivery: {book.tentativeDeliveryDate || `in ${book.tentativeDeliveryDays} days`}</span>
                      </div>
                    </div>
                  </div>

                  {/* Meta Pills */}
                  <div className="grid grid-cols-2 gap-2 text-xs text-gray-600 mb-6">
                    <div className="flex items-center gap-1.5 bg-gray-50 p-2 rounded-lg">
                      <FileText className="w-3.5 h-3.5 text-gray-400" />
                      <span>{book.pageCount} Pages</span>
                    </div>
                    <div className="flex items-center gap-1.5 bg-gray-50 p-2 rounded-lg">
                      <Calendar className="w-3.5 h-3.5 text-gray-400" />
                      <span>Published {book.publishedYear}</span>
                    </div>
                  </div>
                </div>

                {/* Add To Cart Controls */}
                <div className="flex items-center gap-3 pt-4 border-t border-gray-100">
                  <div className="flex items-center border border-gray-300 rounded-xl bg-gray-50 p-1">
                    <button
                      onClick={() => setQuantity(Math.max(1, quantity - 1))}
                      className="w-8 h-8 flex items-center justify-center font-bold text-gray-600 hover:bg-white rounded-lg transition"
                    >
                      -
                    </button>
                    <span className="w-8 text-center text-sm font-bold text-gray-800">{quantity}</span>
                    <button
                      onClick={() => setQuantity(Math.min(book.stockQuantity || 10, quantity + 1))}
                      className="w-8 h-8 flex items-center justify-center font-bold text-gray-600 hover:bg-white rounded-lg transition"
                    >
                      +
                    </button>
                  </div>

                  <button
                    onClick={handleAddToCart}
                    className="flex-1 py-3 px-6 bg-ibm-blue hover:bg-ibm-darkBlue text-white font-bold rounded-xl flex items-center justify-center gap-2 shadow-lg transition active:scale-98"
                  >
                    <ShoppingBag className="w-4 h-4" />
                    <span>Add to Basket · ${(book.price * quantity).toFixed(2)}</span>
                  </button>
                </div>
              </div>
            </div>

            {/* Synopsis Description */}
            <div className="bg-gray-50 rounded-2xl p-5 border border-gray-200">
              <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">Book Synopsis</h4>
              <p className="text-sm text-gray-700 leading-relaxed">{book.description}</p>
              <div className="mt-3 pt-3 border-t border-gray-200 flex items-center gap-2 text-xs text-gray-500">
                <ShieldCheck className="w-4 h-4 text-emerald-600" />
                <span>48-Hour Free Cancellation & 100% Refund Guarantee applied to this item.</span>
              </div>
            </div>

            {/* Related Products Section (Slide 3 Node 7, Slide 6) */}
            {relatedBooks.length > 0 && (
              <div>
                <div className="flex items-center justify-between mb-3">
                  <h4 className="text-sm font-bold text-gray-900 flex items-center gap-2">
                    <BookOpen className="w-4 h-4 text-ibm-blue" />
                    <span>Related Books in This Category</span>
                  </h4>
                  <span className="text-xs text-gray-500">Curated recommendations</span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  {relatedBooks.map((rel) => (
                    <div
                      key={rel.id}
                      onClick={() => onSelectBook(rel)}
                      className="p-3 bg-white rounded-xl border border-gray-200 hover:border-ibm-blue hover:shadow-md transition cursor-pointer group"
                    >
                      <div className="aspect-[3/4] rounded-lg overflow-hidden bg-gray-100 mb-2">
                        <img src={rel.coverImageUrl} alt={rel.title} className="w-full h-full object-cover group-hover:scale-105 transition" />
                      </div>
                      <h5 className="text-xs font-bold text-gray-900 line-clamp-1 group-hover:text-ibm-blue transition">
                        {rel.title}
                      </h5>
                      <p className="text-[11px] text-gray-500 line-clamp-1">{rel.author}</p>
                      <div className="flex items-center justify-between mt-2">
                        <span className="text-xs font-extrabold text-gray-900">${rel.price.toFixed(2)}</span>
                        <ChevronRight className="w-3.5 h-3.5 text-gray-400 group-hover:text-ibm-blue" />
                      </div>
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
