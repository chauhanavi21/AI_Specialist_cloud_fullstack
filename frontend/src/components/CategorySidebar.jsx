import React from 'react';

/** Left rail of genres, as in the wireframe. On small screens it becomes a scrolling strip. */
export default function CategorySidebar({ categories, selectedCategory, onSelectCategory }) {
  const item = (selected) =>
    `w-full text-left px-3 py-2 text-sm flex items-center justify-between gap-2 border-l-2 ${
      selected
        ? 'border-brand bg-layer2 text-fg font-medium'
        : 'border-transparent text-fg2 hover:bg-layer hover:text-fg'
    }`;

  const strip = (selected) =>
    `px-3.5 py-2 text-sm whitespace-nowrap shrink-0 ${
      selected ? 'bg-brand text-white' : 'bg-layer2 text-fg2 hover:text-fg'
    }`;

  return (
    <>
      <nav aria-label="Genres" className="hidden lg:block sticky top-[4.5rem] self-start">
        <h2 className="px-3 pb-2 text-xs uppercase tracking-wider text-fg3">Genres</h2>
        <ul>
          <li>
            <button
              onClick={() => onSelectCategory(null)}
              aria-current={selectedCategory === null ? 'true' : undefined}
              className={item(selectedCategory === null)}
            >
              All
            </button>
          </li>
          {categories.map((cat) => (
            <li key={cat.id}>
              <button
                onClick={() => onSelectCategory(cat.id)}
                aria-current={selectedCategory === cat.id ? 'true' : undefined}
                className={item(selectedCategory === cat.id)}
              >
                <span>{cat.name}</span>
                {cat.bookCount > 0 && <span className="text-xs text-fg3">{cat.bookCount}</span>}
              </button>
            </li>
          ))}
        </ul>
      </nav>

      <nav
        aria-label="Genres"
        className="lg:hidden flex gap-2 overflow-x-auto scrollbar-none pb-1 -mx-3 px-3 sm:mx-0 sm:px-0"
      >
        <button onClick={() => onSelectCategory(null)} className={strip(selectedCategory === null)}>
          All
        </button>
        {categories.map((cat) => (
          <button key={cat.id} onClick={() => onSelectCategory(cat.id)} className={strip(selectedCategory === cat.id)}>
            {cat.name}
          </button>
        ))}
      </nav>
    </>
  );
}
