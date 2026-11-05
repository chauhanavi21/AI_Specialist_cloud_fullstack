import React from 'react';

// Cover palettes echo the wireframe covers: bold colour blocks with a contrasting title.
// Every pair keeps title/background contrast above 4.5:1.
const PALETTES = [
  { bg: '#f1c21b', fg: '#161616', accent: '#161616' }, // yellow
  { bg: '#0b3d91', fg: '#ffffff', accent: '#f1c21b' }, // deep blue
  { bg: '#0e6027', fg: '#ffffff', accent: '#a7f0ba' }, // green
  { bg: '#ff832b', fg: '#161616', accent: '#161616' }, // orange
  { bg: '#6929c4', fg: '#ffffff', accent: '#d4bbff' }, // violet
  { bg: '#1c1c1c', fg: '#ffffff', accent: '#ff7eb6' }, // black
  { bg: '#00539a', fg: '#ffffff', accent: '#82cfff' }, // cobalt
  { bg: '#a2191f', fg: '#ffffff', accent: '#ffd7d9' }, // crimson
];

function paletteFor(id) {
  const n = Number(id) || 0;
  return PALETTES[Math.abs(n) % PALETTES.length];
}

/**
 * Book cover. Uses the real image when the API provides one; otherwise draws a
 * typographic cover from the title and author so the catalogue never looks empty.
 */
export default function BookCover({ book, className = '', compact = false }) {
  if (book.coverImageUrl) {
    return (
      <img
        src={book.coverImageUrl}
        alt={`Cover of ${book.title}`}
        loading="lazy"
        className={`w-full h-full object-cover ${className}`}
      />
    );
  }

  const { bg, fg, accent } = paletteFor(book.id);
  const words = book.title.toUpperCase().split(/\s+/);
  const author = (book.author || '').split('&')[0].trim().toUpperCase();

  return (
    <div
      role="img"
      aria-label={`Cover of ${book.title} by ${book.author}`}
      className={`w-full h-full flex flex-col justify-between overflow-hidden ${compact ? 'p-2' : 'p-3 sm:p-4'} ${className}`}
      style={{ background: bg, color: fg }}
    >
      <div
        className={`font-extrabold leading-[1.05] tracking-tight ${
          compact ? 'text-[8.5px]' : 'text-base sm:text-lg'
        }`}
        style={{
          display: '-webkit-box',
          WebkitLineClamp: compact ? 4 : 5,
          WebkitBoxOrient: 'vertical',
          overflow: 'hidden',
          overflowWrap: 'normal',
          wordBreak: 'normal',
          hyphens: 'none',
        }}
      >
        {words.join(' ')}
      </div>
      <div>
        <div className="h-1 w-8 mb-2" style={{ background: accent }} />
        <div className={`font-semibold tracking-wide ${compact ? 'text-[8px]' : 'text-[10px] sm:text-xs'}`}>
          {author}
        </div>
      </div>
    </div>
  );
}
