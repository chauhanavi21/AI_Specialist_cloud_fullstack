import React from 'react';
import { Sparkles, ShieldCheck, Zap, Gift, RefreshCw } from 'lucide-react';

export default function HeroBanner({ onExploreClick }) {
  return (
    <div className="relative bg-gradient-to-br from-slate-900 via-blue-950 to-indigo-950 text-white rounded-3xl p-6 sm:p-10 mb-8 shadow-xl overflow-hidden">
      {/* Background Decorative Rings */}
      <div className="absolute -right-20 -bottom-20 w-80 h-80 bg-blue-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute left-1/3 -top-20 w-64 h-64 bg-indigo-500/15 rounded-full blur-2xl pointer-events-none" />

      <div className="relative z-10 max-w-3xl">
        <div className="inline-flex items-center gap-2 px-3 py-1 bg-white/10 backdrop-blur-md rounded-full text-xs font-semibold text-blue-200 mb-4 border border-white/10">
          <Sparkles className="w-3.5 h-3.5 text-ibm-accent" />
          <span>Vibe Coded & Built with IBM Bob Agentic IDE</span>
        </div>

        <h1 className="text-2xl sm:text-4xl md:text-5xl font-extrabold tracking-tight leading-tight mb-4">
          Master Hybrid Cloud, GenAI & Modern Software Engineering
        </h1>

        <p className="text-slate-300 text-sm sm:text-base leading-relaxed mb-6 max-w-2xl">
          Discover curated books on OpenShift, Agentic AI, Spring Boot 3 microservices, and Site Reliability Engineering with instant tentative delivery dates, gift loyalty points, and a 48-hour cancellation guarantee.
        </p>

        {/* Feature Highlights Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          <div className="flex items-center gap-2 bg-white/5 border border-white/10 rounded-xl p-2.5 backdrop-blur-sm">
            <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
            <div className="text-left">
              <span className="text-[11px] font-bold block text-white">48h Free Cancel</span>
              <span className="text-[9px] text-slate-400">100% instant refund</span>
            </div>
          </div>

          <div className="flex items-center gap-2 bg-white/5 border border-white/10 rounded-xl p-2.5 backdrop-blur-sm">
            <Gift className="w-4 h-4 text-amber-400 shrink-0" />
            <div className="text-left">
              <span className="text-[11px] font-bold block text-white">Gift Points</span>
              <span className="text-[9px] text-slate-400">10 pts = $1.00 USD</span>
            </div>
          </div>

          <div className="flex items-center gap-2 bg-white/5 border border-white/10 rounded-xl p-2.5 backdrop-blur-sm">
            <RefreshCw className="w-4 h-4 text-sky-400 shrink-0" />
            <div className="text-left">
              <span className="text-[11px] font-bold block text-white">Buy It Again</span>
              <span className="text-[9px] text-slate-400">1-click order re-add</span>
            </div>
          </div>

          <div className="flex items-center gap-2 bg-white/5 border border-white/10 rounded-xl p-2.5 backdrop-blur-sm">
            <Zap className="w-4 h-4 text-yellow-400 shrink-0" />
            <div className="text-left">
              <span className="text-[11px] font-bold block text-white">Fast Delivery</span>
              <span className="text-[9px] text-slate-400">2-3 days dispatch</span>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <button
            onClick={onExploreClick}
            className="px-6 py-2.5 bg-ibm-blue hover:bg-ibm-darkBlue text-white text-sm font-semibold rounded-xl shadow-lg transition active:scale-95"
          >
            Explore Catalog
          </button>
          <span className="text-xs text-slate-400">Free shipping on orders over $50</span>
        </div>
      </div>
    </div>
  );
}
