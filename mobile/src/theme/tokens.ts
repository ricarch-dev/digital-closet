type ColorToken = Readonly<{
  source: `oklch(${string})`;
  native: string;
}>;

const token = (source: ColorToken['source'], native: string): ColorToken => ({
  source,
  native,
});

// OKLCH is the authored palette. The paired sRGB values cross this single
// boundary because React Native's native color parser does not accept OKLCH.
export const sourcePalette = {
  seed: token('oklch(0.650 0.100 140.0)', '#6E9E63'),
  canvas: token('oklch(0.978 0.006 150.0)', '#F5F9F6'),
  surface: token('oklch(1.000 0.000 0.0)', '#FFFFFF'),
  ink: token('oklch(0.270 0.025 155.0)', '#1D2A21'),
  muted: token('oklch(0.470 0.020 155.0)', '#525E56'),
  line: token('oklch(0.900 0.012 150.0)', '#D9E0DA'),
  brand: token('oklch(0.430 0.090 153.0)', '#205E37'),
  brandSoft: token('oklch(0.940 0.030 145.0)', '#DFF1DF'),
  accent: token('oklch(0.550 0.150 35.0)', '#B8492E'),
  accentSoft: token('oklch(0.960 0.025 35.0)', '#FFECE7'),
  focus: token('oklch(0.550 0.160 250.0)', '#0074CA'),
  danger: token('oklch(0.480 0.160 28.0)', '#A52A24'),
} as const;

export const colors = {
  canvas: sourcePalette.canvas.native,
  surface: sourcePalette.surface.native,
  ink: sourcePalette.ink.native,
  muted: sourcePalette.muted.native,
  line: sourcePalette.line.native,
  brand: sourcePalette.brand.native,
  brandSoft: sourcePalette.brandSoft.native,
  accent: sourcePalette.accent.native,
  accentSoft: sourcePalette.accentSoft.native,
  focus: sourcePalette.focus.native,
  danger: sourcePalette.danger.native,
  white: '#FFFFFF',
};

export const radius = {
  small: 10,
  medium: 14,
  large: 16,
  pill: 999,
} as const;

export const spacing = {
  xs: 6,
  sm: 10,
  md: 16,
  lg: 24,
  xl: 32,
} as const;
