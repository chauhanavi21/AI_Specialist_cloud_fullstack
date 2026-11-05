/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // Dark "IBM Carbon" style palette, matching the dark wireframes in the brief.
        canvas: '#161616',   // page background
        layer: '#262626',    // cards, panels, modals
        layer2: '#393939',   // inputs and raised elements on a panel
        line: '#393939',     // dividers
        line2: '#8d8d8d',    // input borders (3:1 against layer2)
        fg: '#f4f4f4',       // primary text
        fg2: '#c6c6c6',      // secondary text
        fg3: '#a8a8a8',      // helper text (still above 4.5:1 on canvas and layer)
        link: '#78a9ff',     // links and active accents on dark
        brand: { DEFAULT: '#0f62fe', hover: '#0353e9', active: '#002d9c' },
        ok: '#42be65',
        danger: '#fa4d56',
        warn: '#f1c21b',
      },
      fontFamily: {
        sans: [
          '"IBM Plex Sans"',
          'Inter',
          '-apple-system',
          'BlinkMacSystemFont',
          '"Segoe UI"',
          'Roboto',
          'sans-serif',
        ],
      },
    },
  },
  plugins: [],
};
