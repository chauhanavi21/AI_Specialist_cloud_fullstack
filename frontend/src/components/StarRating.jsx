import React from 'react';
import { Star } from 'lucide-react';

/** Five-star display, or an accessible radio group when `onChange` is given. */
export default function StarRating({ value = 0, onChange, readOnly = false, size = 'md' }) {
  const dim = size === 'sm' ? 'w-3.5 h-3.5' : 'w-5 h-5';

  if (readOnly || !onChange) {
    return (
      <div className="flex gap-0.5 mt-1" role="img" aria-label={`${value} out of 5 stars`}>
        {[1, 2, 3, 4, 5].map((n) => (
          <Star key={n} className={`${dim} ${n <= value ? 'fill-warn text-warn' : 'text-fg3'}`} aria-hidden="true" />
        ))}
      </div>
    );
  }

  return (
    <div className="flex gap-0.5" role="radiogroup" aria-label="Your rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          role="radio"
          aria-checked={value === n}
          aria-label={`${n} star${n === 1 ? '' : 's'}`}
          onClick={() => onChange(n)}
          className="p-1"
        >
          <Star className={`${dim} ${n <= value ? 'fill-warn text-warn' : 'text-fg3'}`} aria-hidden="true" />
        </button>
      ))}
    </div>
  );
}
