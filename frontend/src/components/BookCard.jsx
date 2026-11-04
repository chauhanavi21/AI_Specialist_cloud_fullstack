import React from 'react';
import { Star, Truck, Plus, Check } from 'lucide-react';
import { useCart } from '../context/CartContext';

export default function BookCard({ book, onOpenDetail }) {
  const { addToCart, cart } = useCart();

  const cartItem = cart.items.find(i => i.book.id === book.id);
  const inCartQty = cartItem ? cartItem.quantity : 0;

  const handleAdd = (e) => {
    e.stopPropagation();
    addToCart(book.id, 1);
  };

  return (
    <div
      onClick={() => onOpenDetail(book)}
      className="group bg-white rounded-2xl border border-gray-200 overflow-hidden shadow-xs hover:shadow-xl hover:border-ibm-blue/40 transition-all duration-300 flex flex-col cursor-pointer"
    >
      {/* Cover Image & Badges */}
      <div className="relative aspect-[3/4] overflow-hidden bg-gray-100">
        <img
          src={book.coverImageUrl}
          alt={book.title}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          loading="lazy"
        />

        {/* Tentative Delivery Badge (Slide 3 Node 7) */}
        <div className="absolute top-2.5 left-2.5 bg-slate-900/80 backdrop-blur-md text-white text-[10px] font-semibold px-2 py-0.5 rounded-md flex items-center gap-1 shadow">
          <Truck className="w-3 h-3 text-sky-400" />
          <span>Delivers in {book.tentativeDeliveryDays}d</span>
        </div>

        {/* Featured Badge */}
        {book.isFeatured && (
          <div className="absolute top-2.5 right-2.5 bg-amber-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-md shadow">
            Featured
          </div>
        )}
      </div>

      {/* Book Info */}
      <div className="p-4 flex-1 flex flex-col justify-between">
        <div>
          {/* Category & Publisher */}
          <div className="flex items-center justify-between text-[11px] text-gray-500 font-medium mb-1">
            <span className="text-ibm-blue font-semibold truncate max-w-[55%]">{book.categoryName}</span>
            <span className="truncate max-w-[40%] text-right">{book.publisherName}</span>
          </div>

          {/* Title */}
          <h3 className="font-bold text-gray-900 text-sm line-clamp-2 leading-snug group-hover:text-ibm-blue transition">
            {book.title}
          </h3>

          {/* Author */}
          <p className="text-xs text-gray-600 mt-1 mb-2 line-clamp-1">{book.author}</p>

          {/* Star Rating */}
          <div className="flex items-center gap-1.5 mb-3">
            <div className="flex items-center text-amber-500">
              <Star className="w-3.5 h-3.5 fill-current" />
            </div>
            <span className="text-xs font-bold text-gray-800">{book.rating.toFixed(1)}</span>
            <span className="text-[11px] text-gray-400">({book.reviewCount})</span>
          </div>
        </div>

        {/* Pricing and Action */}
        <div className="pt-3 border-t border-gray-100 flex items-center justify-between mt-auto">
          <div>
            <div className="text-base font-extrabold text-gray-900">
              ${book.price.toFixed(2)}
            </div>
            {book.originalPrice > book.price && (
              <div className="text-[11px] text-gray-400 line-through">
                ${book.originalPrice.toFixed(2)}
              </div>
            )}
          </div>

          <button
            onClick={handleAdd}
            className={`p-2 rounded-xl text-xs font-semibold transition flex items-center gap-1 shadow-xs ${
              inCartQty > 0
                ? 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200'
                : 'bg-ibm-blue text-white hover:bg-ibm-darkBlue'
            }`}
            title="Add to shopping basket"
          >
            {inCartQty > 0 ? (
              <>
                <Check className="w-4 h-4" />
                <span>{inCartQty} in cart</span>
              </>
            ) : (
              <>
                <Plus className="w-4 h-4" />
                <span>Add</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
