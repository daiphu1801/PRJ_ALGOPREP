/**
 * Text-grade colour for an accent token that is also used as a fill (status dot, chart series).
 * The vivid accents sit at 2.5-4:1 on the light admin surfaces; pulling 40% toward --color-text
 * keeps the hue and reaches 4.8:1 or more on the page background in light, and lightens instead
 * in dark because --color-text flips there (admin UX review F3, 2026-10-14).
 * Accents that already have a hand-picked `-text` token (success, warn, negative) may use it directly.
 */
export function textTone(colorVar: string): string {
  return `color-mix(in srgb, var(${colorVar}) 60%, var(--color-text))`;
}
