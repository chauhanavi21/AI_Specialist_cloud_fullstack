import React from 'react';
import { BookOpen, Terminal, Code2, ShieldCheck, Heart } from 'lucide-react';

export default function Footer() {
  return (
    <footer className="bg-slate-900 text-slate-300 pt-12 pb-8 border-t border-slate-800 mt-16 text-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand Info */}
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-white">
              <div className="w-8 h-8 rounded-lg bg-ibm-blue flex items-center justify-center text-white">
                <BookOpen className="w-5 h-5" />
              </div>
              <span className="text-base font-bold tracking-tight">
                Bob <span className="text-ibm-blue">Bookstore</span>
              </span>
            </div>
            <p className="text-slate-400 text-xs leading-relaxed">
              Full-stack AI Specialist Cloud Capstone application. Designed and vibe-coded with <strong>IBM Bob</strong> Agentic IDE workflows.
            </p>
          </div>

          {/* Architecture */}
          <div>
            <h4 className="text-white font-bold text-xs uppercase tracking-wider mb-3 flex items-center gap-1.5">
              <Code2 className="w-4 h-4 text-sky-400" />
              <span>Full-Stack Architecture</span>
            </h4>
            <ul className="space-y-2 text-slate-400">
              <li>• Spring Boot 3.3.5 (Java 21)</li>
              <li>• PostgreSQL & JPA Persistence</li>
              <li>• Stateless JWT Security Filter</li>
              <li>• React 18, Vite & Tailwind CSS</li>
              <li>• OpenAPI 3.0 / Swagger UI</li>
            </ul>
          </div>

          {/* Business Features */}
          <div>
            <h4 className="text-white font-bold text-xs uppercase tracking-wider mb-3 flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>Core Deliverables</span>
            </h4>
            <ul className="space-y-2 text-slate-400">
              <li>• 12 Customer Journey flows</li>
              <li>• 48-Hour Free Cancellation Engine</li>
              <li>• Loyalty Gift Points Ledger</li>
              <li>• 1-Click Buy It Again Reordering</li>
              <li>• Order-History Recommendations</li>
            </ul>
          </div>

          {/* Agentic Tools */}
          <div>
            <h4 className="text-white font-bold text-xs uppercase tracking-wider mb-3 flex items-center gap-1.5">
              <Terminal className="w-4 h-4 text-amber-400" />
              <span>Agentic IDE Stack</span>
            </h4>
            <p className="text-slate-400 text-xs leading-relaxed">
              Synthesized with IBM Bob Agentic tools, OpenAPI spec generation, JUnit test suites, and ROKS/ROSA cloud deployment templates.
            </p>
            <div className="mt-3">
              <span className="inline-block px-2.5 py-1 bg-blue-900/60 text-blue-300 rounded-md font-semibold text-[10px]">
                IBM Cloud & AI Specialist
              </span>
            </div>
          </div>
        </div>

        <div className="pt-8 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4 text-slate-500 text-[11px]">
          <div>
            © 2026 IBM Bob Bookstore Capstone Project. Built for AI Specialist Demonstration.
          </div>
          <div className="flex items-center gap-1 text-slate-400">
            <span>Powered by</span>
            <span className="font-bold text-white">IBM Bob Agentic IDE</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
