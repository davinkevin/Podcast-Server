import { Injectable } from '@angular/core';
import { Vibrant } from 'node-vibrant/browser';

export interface CoverPalette {
  readonly vibrant: string | null;
  readonly darkVibrant: string | null;
  readonly lightVibrant: string | null;
  readonly muted: string | null;
  readonly darkMuted: string | null;
  readonly lightMuted: string | null;
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
        vibrant: p.Vibrant?.hex ?? null,
        darkVibrant: p.DarkVibrant?.hex ?? null,
        lightVibrant: p.LightVibrant?.hex ?? null,
        muted: p.Muted?.hex ?? null,
        darkMuted: p.DarkMuted?.hex ?? null,
        lightMuted: p.LightMuted?.hex ?? null,
      };
      this.cache.set(url, result);
      return result;
    } catch {
      this.cache.set(url, null);
      return null;
    }
  }
}
