/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Light blue + white theme
        brand: {
          50: '#f3f8fd',
          100: '#e6f1fa',
          200: '#cde3f5',
          300: '#a3cdee',
          400: '#6aaee0',
          500: '#3a8fd1',
          600: '#1f74b8',
          700: '#195d95',
          800: '#154b78',
          900: '#123d61',
        },
      },
      fontFamily: {
        sans: [
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
}
