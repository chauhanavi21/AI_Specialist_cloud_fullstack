import React from 'react';
import { Star, Plus, Check } from 'lucide-react';
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
      className="group bg-white rounded-lg border border-brand-100 overflow-hidden hover:border-brand-300 flex flex-col cursor-pointer"
    >
      <div className="aspect-[3/4] overflow-hidden bg-brand-50">
        <img
          src={book.coverImageUrl}
          alt={book.title}
          className="w-full h-full object-cover"
          loading="lazy"
        />
      </div>

      <div className="p-2.5 sm:p-3 flex-1 flex flex-col">
        <span className="text-[11px] text-brand-600 truncate">{book.categoryName}</span>
        <h3 className="font-medium text-slate-900 text-sm line-clamp-2 leading-snug mt-0.5 group-hover:text-brand-600">
          {book.title}
        </h3>
        <p className="text-xs text-slate-500 mt-1 line-clamp-1">{book.author}</p>

        <div className="flex items-center gap-1 mt-2 text-xs text-slate-600">
          <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
          <span>{book.rating.toFixed(1)}</span>
          <span className="text-slate-400">({book.reviewCount})</span>
        </div>

        <p className="text-[11px] text-slate-500 mt-1">
          Delivery in {book.tentativeDeliveryDays} days
        </p>

        <div className="pt-3 mt-auto flex flex-wrap items-center justify-between gap-x-2 gap-y-2">
          <div>
            <span className="text-base font-semibold text-slate-900">
              ${book.price.toFixed(2)}
            </span>
            {book.originalPrice > book.price && (
              <span className="ml-1.5 text-xs text-slate-400 line-through">
                ${book.originalPrice.toFixed(2)}
              </span>
            )}
          </div>

          <button
            onClick={handleAdd}
            className={`px-2.5 py-2 sm:py-1.5 rounded-md text-xs font-medium flex items-center gap-1 ${
              inCartQty > 0
                ? 'bg-brand-50 text-brand-700 border border-brand-200'
                : 'bg-brand-600 text-white hover:bg-brand-700'
            }`}
            title="Add to basket"
          >
            {inCartQty > 0 ? (
              <>
                <Check className="w-3.5 h-3.5" />
                <span>{inCartQty} in basket</span>
              </>
            ) : (
              <>
                <Plus className="w-3.5 h-3.5" />
                <span>Add</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
