import React from 'react';
import { Heart, Star } from 'lucide-react';
import BookCover from './BookCover';
import { useCart } from '../context/CartContext';
import { useWishlist } from '../context/WishlistContext';
import { deliveryBy, inr } from '../lib/format';

/** Horizontal card matching the wireframe: cover on the left, details on the right. */
export default function BookCard({ book, onOpenDetail }) {
  const { addToCart, cart } = useCart();
  const wishlist = useWishlist();

  const inCartQty = cart.items.find((i) => i.book.id === book.id)?.quantity || 0;
  const wished = wishlist.has(book.id);
  const outOfStock = book.inStock === false;

  return (
    <article className="group relative bg-layer border border-line hover:border-line2 flex gap-3 p-3">
      <button
        onClick={() => onOpenDetail(book)}
        className="w-24 sm:w-28 shrink-0 aspect-[3/4] overflow-hidden text-left"
        aria-label={`View details for ${book.title}`}
      >
        <BookCover book={book} compact />
      </button>

      <div className="flex-1 min-w-0 flex flex-col">
        <button onClick={() => onOpenDetail(book)} className="text-left">
          <h3 className="text-sm font-semibold text-fg leading-snug line-clamp-2 pr-8 group-hover:text-link">
            {book.title}
          </h3>
        </button>
        <p className="text-xs text-link mt-0.5 truncate">by {book.author}</p>
        <p className="text-xs text-fg3 mt-1.5 line-clamp-2">{book.blurb}</p>

        <p className="text-xs text-fg2 mt-2">
          {book.format}
          <span className="text-fg3"> · </span>
          <span className="text-link">{book.categoryName}</span>
        </p>

        <div className="flex items-center gap-1 mt-1.5 text-xs text-fg2">
          <Star className="w-3.5 h-3.5 fill-warn text-warn" aria-hidden="true" />
          <span>{Number(book.rating).toFixed(1)}</span>
          <span className="text-fg3">({book.reviewCount})</span>
        </div>

        <div className="mt-auto pt-3">
          <div className="flex items-baseline gap-2">
            <span className="text-base font-semibold text-fg">{inr(book.price)}</span>
            {book.originalPrice > book.price && (
              <span className="text-xs text-fg3 line-through">{inr(book.originalPrice)}</span>
            )}
          </div>
          <p className="text-[11px] text-fg3 mt-0.5">{outOfStock ? 'Out of stock' : deliveryBy(book)}</p>

          <button
            onClick={() => addToCart(book.id, 1)}
            disabled={outOfStock}
            className={`mt-2 w-full text-xs ${inCartQty > 0 ? 'btn-secondary' : 'btn-primary'} min-h-[2.25rem]`}
          >
            {inCartQty > 0 ? `${inCartQty} in basket · add another` : 'Add to basket'}
          </button>
        </div>
      </div>

      <button
        onClick={() => wishlist.toggle(book)}
        aria-pressed={wished}
        aria-label={wished ? `Remove ${book.title} from wishlist` : `Add ${book.title} to wishlist`}
        className="absolute top-2 right-2 p-1.5 text-fg2 hover:text-fg hover:bg-layer2"
      >
        <Heart className={`w-4 h-4 ${wished ? 'fill-danger text-danger' : ''}`} />
      </button>
    </article>
  );
}
