import React, { useState, useEffect } from 'react';
import { Sparkles, ChevronRight, Star } from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function RecommendationsShelf({ onSelectBook }) {
  const { user } = useAuth();
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!user) {
      setRecommendations([]);
      return;
    }
    setLoading(true);
    api.getHistoryRecommendations(4)
      .then((data) => setRecommendations(data || []))
      .catch((err) => console.error('Failed to load recommendations', err))
      .finally(() => setLoading(false));
  }, [user]);

  if (!user || recommendations.length === 0) return null;

  return (
    <div className="bg-gradient-to-r from-blue-50 via-indigo-50 to-purple-50 rounded-3xl p-6 border border-blue-200/60 mb-8 shadow-xs">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-xl bg-ibm-blue text-white flex items-center justify-center shadow-xs">
            <Sparkles className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-base font-extrabold text-gray-900">
              Recommended For You
            </h3>
            <p className="text-xs text-gray-500">
              Tailored based on your order history & past tech topics
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {recommendations.map((book) => (
          <div
            key={book.id}
            onClick={() => onSelectBook(book)}
            className="bg-white rounded-2xl p-3 border border-gray-200 hover:border-ibm-blue hover:shadow-lg transition cursor-pointer flex flex-col justify-between group"
          >
            <div>
              <div className="aspect-[3/4] rounded-xl overflow-hidden bg-gray-100 mb-2">
                <img
                  src={book.coverImageUrl}
                  alt={book.title}
                  className="w-full h-full object-cover group-hover:scale-105 transition duration-300"
                />
              </div>
              <span className="text-[10px] font-bold text-ibm-blue block truncate">{book.categoryName}</span>
              <h4 className="text-xs font-bold text-gray-900 line-clamp-2 leading-tight group-hover:text-ibm-blue transition">
                {book.title}
              </h4>
              <p className="text-[11px] text-gray-500 line-clamp-1 mt-0.5">{book.author}</p>
            </div>

            <div className="flex items-center justify-between mt-3 pt-2 border-t border-gray-100">
              <span className="text-xs font-black text-gray-900">${book.price?.toFixed(2)}</span>
              <div className="flex items-center gap-1 text-[11px] font-bold text-amber-500">
                <Star className="w-3 h-3 fill-current" />
                <span>{book.rating?.toFixed(1)}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
