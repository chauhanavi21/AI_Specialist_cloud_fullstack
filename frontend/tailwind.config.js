/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        ibm: {
          blue: '#0f62fe',
          darkBlue: '#0043ce',
          lightBlue: '#edf5ff',
          gray: '#f4f4f4',
          text: '#161616',
          border: '#e0e0e0',
          accent: '#10b981'
        }
      }
    },
  },
  plugins: [],
}
