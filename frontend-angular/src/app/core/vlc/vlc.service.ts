import { Injectable } from '@angular/core';

import type { CoverCardAction } from '../../shared/cover-card/cover-card.component';

/**
 * Hands an item's proxy URL off to the desktop / iOS VLC app via the
 * `vlc-x-callback://` URL scheme — same pattern as the V1 `vg-link-vlc`
 * directive, with one tweak: we URL-encode the inner stream URL so query
 * params and unicode survive the round-trip.
 *
 * We trigger the navigation via `location.assign` rather than rendering
 * the URL as an `<a href>` because Angular's DOM sanitizer rewrites
 * non-http(s) schemes to `unsafe:…`. Going through `location` keeps the
 * action a plain click handler with no `bypassSecurityTrustUrl` dance.
 */
@Injectable({ providedIn: 'root' })
export class VlcService {
  buildCallbackUrl(proxyUrl: string): string {
    const fullUrl = new URL(proxyUrl, window.location.origin).toString();
    return `vlc-x-callback://x-callback-url/stream?url=${encodeURIComponent(fullUrl)}`;
  }

  openInVlc(proxyUrl: string): void {
    window.location.assign(this.buildCallbackUrl(proxyUrl));
  }
}

/** Shared menu entry so all three list pages render the action identically.
 *  The `id` is what handlers match on; only present the action when the
 *  underlying item is actually playable (downloaded) — otherwise the proxy
 *  URL would 404 inside VLC. */
export const OPEN_IN_VLC_ACTION: CoverCardAction = {
  id: 'open-in-vlc',
  label: 'Open in VLC',
  icon: 'play_circle_outline',
};
