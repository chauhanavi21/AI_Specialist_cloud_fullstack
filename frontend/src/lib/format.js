const whole = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const full = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** Rupees: whole numbers print without decimals (₹899), anything else with two (₹956.88). */
export function inr(value) {
  const n = Number(value ?? 0);
  return Number.isInteger(n) ? whole.format(n) : full.format(n);
}

/** Rupees, always with two decimals. */
export function inr2(value) {
  return full.format(Number(value ?? 0));
}

/** Parses an ISO date ("2026-11-07") as a local date so it never shifts a day. */
export function parseDate(value) {
  if (!value) return null;
  const s = String(value);
  const d = new Date(s.length === 10 ? `${s}T00:00:00` : s);
  return Number.isNaN(d.getTime()) ? null : d;
}

/** "Mon, 21 Jul" */
export function shortDay(value) {
  const d = parseDate(value);
  return d ? d.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' }) : '';
}

/** "21 Jul 2026" */
export function fullDate(value) {
  const d = parseDate(value);
  return d ? d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '';
}

/** "Delivery by Mon, 21 Jul" with a days-based fallback. */
export function deliveryBy(book) {
  const label = shortDay(book.tentativeDeliveryDate);
  return label ? `Delivery by ${label}` : `Delivery in ${book.tentativeDeliveryDays} days`;
}

export const plural = (n, word) => `${n} ${word}${n === 1 ? '' : 's'}`;
