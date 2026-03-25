import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{js,ts,jsx,tsx,mdx}"],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: "#1E3A5F",
          light: "#2A4A73",
          dark: "#152A47",
          50: "#EEF2F7",
          100: "#D5DEE9",
        },
        accent: {
          teal: "#2A9D8F",
          gold: "#E9C46A",
        },
        surface: {
          light: "#FFFFFF",
          dark: "#1E293B",
        },
        success: {
          DEFAULT: "#22C55E",
          light: "#DCFCE7",
        },
        warning: "#F59E0B",
        danger: "#EF4444",
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};

export default config;
