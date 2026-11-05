import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function RecommendationsShelf({ onSelectBook }) {
  const { user } = useAuth();
  const [recommendations, setRecommendations] = useState([]);

  useEffect(() => {
    if (!user) {
      setRecommendations([]);
      return;
    }
    api.getHistoryRecommendations(4)
      .then((data) => setRecommendations(data || []))
      .catch((err) => console.error('Failed to load recommendations', err));
  }, [user]);

  if (!user || recommendations.length === 0) return null;

  return (
    <section className="mb-8">
      <h2 className="text-lg font-semibold text-slate-900">Recommended for you</h2>
      <p className="text-xs text-slate-500 mb-3">Based on your previous orders</p>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 sm:gap-4">
        {recommendations.map((book) => (
          <div
            key={book.id}
            onClick={() => onSelectBook(book)}
            className="bg-white rounded-lg p-3 border border-brand-100 hover:border-brand-300 cursor-pointer group"
          >
            <div className="aspect-[3/4] rounded overflow-hidden bg-brand-50 mb-2">
              <img
                src={book.coverImageUrl}
                alt={book.title}
                className="w-full h-full object-cover"
              />
            </div>
            <h3 className="text-sm font-medium text-slate-900 line-clamp-2 leading-snug group-hover:text-brand-600">
              {book.title}
            </h3>
            <p className="text-xs text-slate-500 line-clamp-1 mt-0.5">{book.author}</p>
            <p className="text-sm font-semibold text-slate-900 mt-2">${book.price?.toFixed(2)}</p>
          </div>
        ))}
      </div>
    </section>
  );
}
