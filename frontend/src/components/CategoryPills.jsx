import React from 'react';
import { Layers } from 'lucide-react';

export default function CategoryPills({
  categories,
  selectedCategory,
  onSelectCategory,
}) {
  return (
    <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none mb-4">
      <button
        onClick={() => onSelectCategory(null)}
        className={`px-4 py-2 rounded-full text-xs font-semibold whitespace-nowrap transition flex items-center gap-1.5 ${
          selectedCategory === null
            ? 'bg-ibm-blue text-white shadow-sm'
            : 'bg-white text-gray-700 hover:bg-gray-100 border border-gray-200'
        }`}
      >
        <Layers className="w-3.5 h-3.5" />
        <span>All Categories</span>
      </button>

      {categories.map((cat) => {
        const isSelected = selectedCategory === cat.id;
        return (
          <button
            key={cat.id}
            onClick={() => onSelectCategory(cat.id)}
            className={`px-4 py-2 rounded-full text-xs font-semibold whitespace-nowrap transition ${
              isSelected
                ? 'bg-ibm-blue text-white shadow-sm'
                : 'bg-white text-gray-700 hover:bg-gray-100 border border-gray-200'
            }`}
          >
            {cat.name}
            {cat.bookCount > 0 && (
              <span className={`ml-1.5 px-1.5 py-0.2 rounded-full text-[10px] ${
                isSelected ? 'bg-blue-700 text-white' : 'bg-gray-100 text-gray-500'
              }`}>
                {cat.bookCount}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}
