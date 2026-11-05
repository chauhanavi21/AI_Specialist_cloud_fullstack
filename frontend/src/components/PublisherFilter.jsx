import React from 'react';

export default function PublisherFilter({
  publishers,
  selectedPublisher,
  onSelectPublisher,
}) {
  const btn = (selected) =>
    `px-3 py-1.5 sm:py-1 rounded-md text-xs whitespace-nowrap shrink-0 ${
      selected
        ? 'bg-slate-800 text-white'
        : 'text-slate-600 hover:bg-brand-100'
    }`;

  return (
    <div className="flex items-center gap-1 overflow-x-auto pb-2 scrollbar-none mb-5 sm:mb-6 -mx-3 px-3 min-[420px]:-mx-4 min-[420px]:px-4 sm:mx-0 sm:px-0">
      <span className="text-xs text-slate-500 mr-2 shrink-0">Publisher</span>

      <button onClick={() => onSelectPublisher(null)} className={btn(selectedPublisher === null)}>
        All
      </button>

      {publishers.map((pub) => (
        <button
          key={pub.id}
          onClick={() => onSelectPublisher(pub.id)}
          className={btn(selectedPublisher === pub.id)}
        >
          {pub.name}
        </button>
      ))}
    </div>
  );
}
