import React from 'react';
import BookCover from './BookCover';
import { inr } from '../lib/format';

/**
 * A short list of suggested books. `title` says why they are suggested (a better alternative, or books that go
 * with this one); `hint` is the one-line explanation under it.
 */
export default function SuggestionRail({ title, hint, books, onSelect }) {
  if (!books || books.length === 0) return null;
  return (
    <section aria-label={title} className="mt-6">
      <h3 className="text-sm font-semibold text-fg">{title}</h3>
      {hint && <p className="text-[11px] text-fg3 mb-3">{hint}</p>}
      <ul className="space-y-3">
        {books.map((b) => (
          <li key={b.id}>
            <button onClick={() => onSelect(b)} className="flex gap-3 text-left w-full group">
              <div className="w-12 shrink-0 aspect-[3/4]"><BookCover book={b} compact /></div>
              <div className="min-w-0">
                <p className="text-xs font-semibold text-fg line-clamp-2 group-hover:text-link">{b.title}</p>
                <p className="text-[11px] text-fg3 truncate">{b.categoryName}</p>
                <p className="text-xs font-semibold text-fg mt-0.5">{inr(b.price)}</p>
              </div>
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}
