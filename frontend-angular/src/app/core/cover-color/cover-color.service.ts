import { Injectable } from '@angular/core';
import { Vibrant } from 'node-vibrant/browser';
import type { Swatch } from '@vibrant/color';

export interface CoverSwatch {
  readonly hex: string;
  readonly titleText: string;
  readonly bodyText: string;
}

export interface CoverPalette {
  readonly vibrant: CoverSwatch | null;
  readonly darkVibrant: CoverSwatch | null;
  readonly lightVibrant: CoverSwatch | null;
  readonly muted: CoverSwatch | null;
  readonly darkMuted: CoverSwatch | null;
  readonly lightMuted: CoverSwatch | null;
}

function toCoverSwatch(s: Swatch | null | undefined): CoverSwatch | null {
  return s
    ? { hex: s.hex, titleText: s.titleTextColor, bodyText: s.bodyTextColor }
    : null;
}

// CSS variables driven by the cover palette. Used across the shell and the
// detail pages — kept here so route components stay declarative.
//
// `--hero-title-color` / `--hero-body-color` were intentionally dropped:
// Vibrant.titleTextColor is calibrated for text sitting on the *raw*
// swatch (e.g. white on solid bright pink). Our hero has
// `background: transparent` and the page tint is folded into the shell
// at 38 % opacity (see app.component.scss), so the actual background
// behind the title is pale-pink-over-surface, not the raw swatch — white
// text becomes invisible in light theme. The SCSS fallbacks
// `--mat-sys-on-surface` / `--mat-sys-on-surface-variant` give the right
// contrast in both themes.
const TINT_VARS = [
  '--page-tint',
  '--page-tint-bottom',
  '--page-accent',
  '--page-on-accent',
] as const;

export function applyCoverTint(
  palette: CoverPalette | null,
  isDark: boolean,
): void {
  const root = document.documentElement;
  if (!palette) {
    for (const v of TINT_VARS) root.style.removeProperty(v);
    return;
  }
  const top = isDark
    ? palette.darkVibrant ?? palette.vibrant
    : palette.vibrant ?? palette.lightVibrant;
  const bottom = isDark
    ? palette.darkMuted ?? palette.muted
    : palette.muted ?? palette.lightMuted;
  // Accents always lean on Vibrant when available — buttons should pop.
  const accent = palette.vibrant ?? top;
  set(root, '--page-tint', top?.hex);
  set(root, '--page-tint-bottom', bottom?.hex);
  set(root, '--page-accent', accent?.hex);
  set(root, '--page-on-accent', accent?.titleText);
}

export function clearCoverTint(): void {
  applyCoverTint(null, false);
}

function set(el: HTMLElement, name: string, value: string | undefined): void {
  if (value) el.style.setProperty(name, value);
  else el.style.removeProperty(name);
}

@Injectable({ providedIn: 'root' })
export class CoverColorService {
  // Palette cached per URL so navigating back to a cover does not rerun
  // extraction.
  private readonly cache = new Map<string, CoverPalette | null>();

  async extract(url: string): Promise<CoverPalette | null> {
    const cached = this.cache.get(url);
    if (cached !== undefined) return cached;

    try {
      const p = await Vibrant.from(url).getPalette();
      const result: CoverPalette = {
        vibrant: toCoverSwatch(p.Vibrant),
        darkVibrant: toCoverSwatch(p.DarkVibrant),
        lightVibrant: toCoverSwatch(p.LightVibrant),
        muted: toCoverSwatch(p.Muted),
        darkMuted: toCoverSwatch(p.DarkMuted),
        lightMuted: toCoverSwatch(p.LightMuted),
      };
      this.cache.set(url, result);
      return result;
    } catch {
      this.cache.set(url, null);
      return null;
    }
  }
}
