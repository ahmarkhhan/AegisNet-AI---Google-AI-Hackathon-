/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        'aegis-dark': '#050B14',
        'aegis-panel': '#0A1128',
        'aegis-primary': '#00F0FF',
        'aegis-secondary': '#2563EB',
        'aegis-alert': '#FF003C',
        'aegis-warn': '#F59E0B',
        'aegis-success': '#00FF66'
      },
      fontFamily: {
        sans: ['Space Grotesk', 'sans-serif'],
        display: ['Rajdhani', 'sans-serif'],
        mono: ['Fira Code', 'monospace']
      },
      backgroundImage: {
        'grid-pattern': "linear-gradient(to right, #1f2937 1px, transparent 1px), linear-gradient(to bottom, #1f2937 1px, transparent 1px)",
        'hud-gradient': "linear-gradient(135deg, rgba(0,240,255,0.1) 0%, rgba(10,17,40,0.8) 100%)",
      }
    },
  },
  plugins: [],
}
