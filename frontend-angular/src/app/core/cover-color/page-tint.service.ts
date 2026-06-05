import { Injectable, effect, inject, signal } from '@angular/core';

import { CurrentUrlService } from '../navigation/current-url.service';
import { SettingsService } from '../settings/settings.service';
import { CoverPalette, applyCoverTint } from './cover-color.service';

/**
 * Route-aware owner of the global cover tint (`--page-tint` & friends on
 * `<html>`). Detail pages *claim* a tint for the URL they are rendered at;
 * the tint is applied only while that URL is the active one and cleared as
 * soon as navigation moves to a page without a claim.
 *
 * This indirection exists because of ListRouteReuseStrategy: detail pages
 * are detached — not destroyed — on navigation, so a destroy-time cleanup
 * never runs, and a detached page must not be able to clobber the tint of
 * the page on screen. Matching claims against the current URL makes the
 * apply/clear ordering between outgoing and incoming pages irrelevant.
 */
@Injectable({ providedIn: 'root' })
export class PageTintService {
  /** Exposed so claiming pages can use it as their re-attach trigger. */
  readonly currentUrl = inject(CurrentUrlService).url;
  private readonly settings = inject(SettingsService);

  private readonly claimed = signal<{
    url: string;
    palette: CoverPalette | null;
  } | null>(null);

  constructor() {
    effect(() => {
      const claim = this.claimed();
      const dark = this.settings.effectiveTheme() === 'dark';
      const active = claim !== null && claim.url === this.currentUrl();
      applyCoverTint(active ? claim.palette : null, dark);
    });
  }

  claim(url: string, palette: CoverPalette | null): void {
    this.claimed.set({ url, palette });
  }
}
