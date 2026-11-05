import React from 'react';

export default function CategoryPills({
  categories,
  selectedCategory,
  onSelectCategory,
}) {
  const btn = (selected) =>
    `px-3.5 py-2 sm:py-1.5 rounded-md text-sm whitespace-nowrap shrink-0 ${
      selected
        ? 'bg-brand-600 text-white'
        : 'bg-white text-slate-700 border border-brand-100 hover:bg-brand-50'
    }`;

  return (
    <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none mb-3 -mx-3 px-3 min-[420px]:-mx-4 min-[420px]:px-4 sm:mx-0 sm:px-0">
      <button onClick={() => onSelectCategory(null)} className={btn(selectedCategory === null)}>
        All
      </button>

      {categories.map((cat) => (
        <button
          key={cat.id}
          onClick={() => onSelectCategory(cat.id)}
          className={btn(selectedCategory === cat.id)}
        >
          {cat.name}
          {cat.bookCount > 0 && (
            <span className="ml-1.5 text-xs opacity-70">{cat.bookCount}</span>
          )}
        </button>
      ))}
    </div>
  );
}
