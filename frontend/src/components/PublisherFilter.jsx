import React from 'react';
import { Building2 } from 'lucide-react';

export default function PublisherFilter({
  publishers,
  selectedPublisher,
  onSelectPublisher,
}) {
  return (
    <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none mb-6">
      <div className="flex items-center gap-1.5 text-xs font-semibold text-gray-500 mr-1 shrink-0">
        <Building2 className="w-3.5 h-3.5" />
        <span>Publisher / Brand:</span>
      </div>

      <button
        onClick={() => onSelectPublisher(null)}
        className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition ${
          selectedPublisher === null
            ? 'bg-gray-900 text-white shadow-xs'
            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
        }`}
      >
        All Brands
      </button>

      {publishers.map((pub) => {
        const isSelected = selectedPublisher === pub.id;
        return (
          <button
            key={pub.id}
            onClick={() => onSelectPublisher(pub.id)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition ${
              isSelected
                ? 'bg-gray-900 text-white shadow-xs'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            {pub.name}
          </button>
        );
      })}
    </div>
  );
}
