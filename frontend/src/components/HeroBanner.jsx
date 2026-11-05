import React from 'react';

export default function HeroBanner({ onExploreClick }) {
  return (
    <div className="bg-brand-100 border border-brand-200 rounded-lg px-4 py-6 sm:px-10 sm:py-10 mb-6 sm:mb-8">
      <div className="max-w-2xl">
        <h1 className="text-xl min-[420px]:text-2xl md:text-3xl font-semibold text-slate-900 leading-tight mb-3">
          Books on cloud, AI and software engineering
        </h1>
        <p className="text-slate-600 text-sm sm:text-base leading-relaxed mb-5">
          OpenShift, Spring Boot, microservices, reliability engineering and more.
          Free shipping over $50, and you can cancel any order within 48 hours.
        </p>
        <button
          onClick={onExploreClick}
          className="px-5 py-2.5 bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium rounded-md"
        >
          Browse catalog
        </button>
      </div>
    </div>
  );
}
