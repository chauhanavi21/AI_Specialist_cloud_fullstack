import React from 'react';
import BookCard from './BookCard';

/** A titled row of book cards: "Recommended for You", "Bestsellers this Month", "New Launches". */
export default function Shelf({ title, subtitle, books, onOpenDetail }) {
  if (!books || books.length === 0) return null;
  return (
    <section className="mb-8" aria-label={title}>
      <h2 className="text-base font-semibold text-fg">{title}</h2>
      {subtitle && <p className="text-xs text-fg3 mb-3">{subtitle}</p>}
      {!subtitle && <div className="mb-3" />}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
        {books.map((book) => (
          <BookCard key={book.id} book={book} onOpenDetail={onOpenDetail} />
        ))}
      </div>
    </section>
  );
}
