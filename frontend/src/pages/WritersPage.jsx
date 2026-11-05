import React, { useEffect, useState, useCallback } from 'react';
import { ArrowLeft, UserCheck, UserPlus } from 'lucide-react';
import BookCard from '../components/BookCard';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { plural } from '../lib/format';

const initials = (name) => name.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join('');

export default function WritersPage({ onOpenDetail }) {
  const { user, openAuthModal } = useAuth();
  const toast = useToast();

  const [tab, setTab] = useState(user ? 'followed' : 'all');
  const [writers, setWriters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openId, setOpenId] = useState(null);
  const [detail, setDetail] = useState(null);

  const load = useCallback(() => {
    setError('');
    return api.getWriters(tab === 'followed' && !!user)
      .then((data) => setWriters(data || []))
      .catch((err) => setError(err.message || 'Could not load writers'));
  }, [tab, user?.id]);

  useEffect(() => {
    setLoading(true);
    load().finally(() => setLoading(false));
  }, [load]);

  useEffect(() => {
    if (!openId) { setDetail(null); return; }
    api.getWriter(openId).then(setDetail).catch((err) => toast.error(err.message || 'Could not load this writer'));
  }, [openId, user?.id]);

  const toggleFollow = async (writer) => {
    if (!user) return openAuthModal('login');
    const next = !writer.following;
    try {
      if (next) await api.followWriter(writer.id);
      else await api.unfollowWriter(writer.id);
      toast.success(next ? `Following ${writer.name}` : `Unfollowed ${writer.name}`);
      setDetail((d) => (d && d.id === writer.id ? { ...d, following: next } : d));
      await load();
    } catch (err) {
      toast.error(err.message || 'Could not update follow');
    }
    return undefined;
  };

  if (openId && detail) {
    return (
      <section aria-labelledby="writer-title">
        <button onClick={() => setOpenId(null)} className="flex items-center gap-1 text-sm text-link hover:underline mb-4">
          <ArrowLeft className="w-4 h-4" aria-hidden="true" /> All writers
        </button>
        <div className="flex gap-4 mb-6">
          <div className="size-20 shrink-0 rounded-full bg-layer2 text-link flex items-center justify-center text-2xl font-semibold" aria-hidden="true">
            {initials(detail.name)}
          </div>
          <div className="min-w-0">
            <h1 id="writer-title" className="text-xl font-semibold">{detail.name}</h1>
            <p className="text-sm text-fg2 mt-1 max-w-2xl">{detail.bio}</p>
            <button onClick={() => toggleFollow(detail)} aria-pressed={detail.following} className="btn-secondary mt-3">
              {detail.following ? <UserCheck className="w-4 h-4" aria-hidden="true" /> : <UserPlus className="w-4 h-4" aria-hidden="true" />}
              {detail.following ? 'Following' : 'Follow writer'}
            </button>
          </div>
        </div>
        <h2 className="text-base font-semibold mb-3">Books by {detail.name}</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
          {detail.books.map((b) => <BookCard key={b.id} book={b} onOpenDetail={onOpenDetail} />)}
        </div>
      </section>
    );
  }

  const tabClass = (active) =>
    `px-4 py-2 text-sm border-b-2 ${active ? 'border-brand text-fg' : 'border-transparent text-fg2 hover:text-fg'}`;

  return (
    <section aria-labelledby="writers-title">
      <h1 id="writers-title" className="text-xl font-semibold mb-1">My Writers</h1>
      <p className="text-xs text-fg3 mb-4">Follow the writers you love and find all their books in one place.</p>

      <div role="tablist" aria-label="Writers" className="flex border-b border-line mb-5">
        <button role="tab" aria-selected={tab === 'followed'} onClick={() => (user ? setTab('followed') : openAuthModal('login'))} className={tabClass(tab === 'followed')}>
          Following
        </button>
        <button role="tab" aria-selected={tab === 'all'} onClick={() => setTab('all')} className={tabClass(tab === 'all')}>
          All writers
        </button>
      </div>

      {loading ? (
        <p className="text-sm text-fg3 py-12 text-center" role="status">Loading writers...</p>
      ) : error ? (
        <div className="panel p-8 text-center space-y-3">
          <p className="text-sm text-danger" role="alert">{error}</p>
          <button onClick={() => { setLoading(true); load().finally(() => setLoading(false)); }} className="btn-secondary">Try again</button>
        </div>
      ) : writers.length === 0 ? (
        <div className="panel p-10 text-center">
          <p className="text-fg">You are not following any writers yet</p>
          <button onClick={() => setTab('all')} className="btn-primary mt-4">Find writers</button>
        </div>
      ) : (
        <ul className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
          {writers.map((w) => (
            <li key={w.id} className="panel p-4 flex items-center gap-3">
              <div className="size-12 shrink-0 rounded-full bg-layer2 text-link flex items-center justify-center font-semibold" aria-hidden="true">
                {initials(w.name)}
              </div>
              <button onClick={() => setOpenId(w.id)} className="flex-1 min-w-0 text-left">
                <span className="block text-sm font-semibold text-fg truncate hover:text-link">{w.name}</span>
                <span className="block text-xs text-fg3">{plural(w.bookCount, 'book')}</span>
              </button>
              <button
                onClick={() => toggleFollow(w)}
                aria-pressed={w.following}
                aria-label={`${w.following ? 'Unfollow' : 'Follow'} ${w.name}`}
                className="btn-secondary min-h-[2.25rem]"
              >
                {w.following ? 'Following' : 'Follow'}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
