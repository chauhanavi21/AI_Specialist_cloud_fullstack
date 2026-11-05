import React, { useEffect, useRef } from 'react';
import { X } from 'lucide-react';

// Only the top-most open modal reacts to Escape.
const openModals = [];
let nextModalId = 1;

const SIZES = {
  sm: 'sm:max-w-sm',
  md: 'sm:max-w-lg',
  lg: 'sm:max-w-3xl',
  xl: 'sm:max-w-5xl',
};

/**
 * Accessible dialog shell: role="dialog", Escape and backdrop-click to close,
 * focus moves in on open and returns to the opener on close, page scroll is locked.
 * `placement="right"` renders a full-height side drawer.
 */
export default function Modal({
  open,
  onClose,
  title,
  size = 'md',
  placement = 'center',
  z = 50,
  panelClassName = '',
  backdropClassName = 'bg-black/70',
  children,
}) {
  const panelRef = useRef(null);
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    if (!open) return undefined;
    const id = nextModalId++;
    openModals.push(id);
    const opener = document.activeElement;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const onKey = (e) => {
      if (e.key === 'Escape' && openModals[openModals.length - 1] === id) {
        onCloseRef.current?.();
      }
    };
    document.addEventListener('keydown', onKey);
    panelRef.current?.focus();

    return () => {
      document.removeEventListener('keydown', onKey);
      const index = openModals.indexOf(id);
      if (index >= 0) openModals.splice(index, 1);
      document.body.style.overflow = previousOverflow;
      if (opener && typeof opener.focus === 'function') opener.focus();
    };
  }, [open]);

  if (!open) return null;

  const isDrawer = placement === 'right';

  return (
    <div
      className={`fixed inset-0 flex ${
        isDrawer ? 'justify-end' : 'items-end sm:items-center justify-center p-0 sm:p-4'
      } ${backdropClassName}`}
      style={{ zIndex: z }}
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose?.();
      }}
    >
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        tabIndex={-1}
        className={`relative w-full bg-layer text-fg border border-line shadow-2xl outline-none flex flex-col ${
          isDrawer
            ? 'h-dvh max-w-md border-y-0 border-r-0'
            : `${SIZES[size] || SIZES.md} rounded-t-lg sm:rounded max-h-[95dvh] sm:max-h-[92dvh]`
        } ${panelClassName}`}
      >
        {children}
      </div>
    </div>
  );
}

export function ModalHeader({ title, subtitle, onClose }) {
  return (
    <div className="flex items-start justify-between gap-4 px-5 sm:px-6 py-4 border-b border-line shrink-0">
      <div className="min-w-0">
        <h2 className="text-lg font-semibold text-fg">{title}</h2>
        {subtitle && <p className="text-xs text-fg3 mt-0.5">{subtitle}</p>}
      </div>
      {onClose && (
        <button
          onClick={onClose}
          aria-label="Close"
          className="p-2 -m-2 text-fg2 hover:text-fg hover:bg-layer2 shrink-0"
        >
          <X className="w-5 h-5" />
        </button>
      )}
    </div>
  );
}
