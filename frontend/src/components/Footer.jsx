import React from 'react';

export default function Footer() {
  return (
    <footer className="bg-white border-t border-brand-100 mt-16 text-sm text-slate-500">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 flex flex-col sm:flex-row items-center justify-between gap-2 text-center sm:text-left">
        <span>
          <span className="font-semibold text-slate-800">Bob <span className="text-brand-600">Bookstore</span></span>
          {' '}· Free shipping over $50 · 48-hour order cancellation
        </span>
        <span className="text-xs">© 2026 Bob Bookstore</span>
      </div>
    </footer>
  );
}
