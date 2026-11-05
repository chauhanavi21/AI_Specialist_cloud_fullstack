import React from 'react';

export default function Footer() {
  return (
    <footer className="bg-canvas border-t border-line mt-12 text-sm text-fg3">
      <div className="max-w-[88rem] mx-auto px-3 sm:px-6 py-6 flex flex-col sm:flex-row items-center justify-between gap-2 text-center sm:text-left">
        <span>
          <span className="font-semibold text-fg">Book Worm</span>
          {' '}· Free delivery from ₹499 · 48-hour order cancellation
        </span>
        <span className="text-xs">© 2026 Book Worm · Demo store, no real payments</span>
      </div>
    </footer>
  );
}
