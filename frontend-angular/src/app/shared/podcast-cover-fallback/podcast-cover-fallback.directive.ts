import { Directive, ElementRef, inject, input } from '@angular/core';

/** Cover of a podcast, served from the bucket copy the cover cleanup never deletes. */
export function podcastCoverUrl(podcastId: string): string {
  return `/api/v1/podcasts/${podcastId}/cover.jpg`;
}

/**
 * Falls back to the podcast cover when an item cover can't be loaded.
 *
 * The item cover route redirects to the bucket copy, or to the remote URL once
 * the nightly cleanup has removed that copy — and for an old item that remote
 * URL is often dead. The swap happens once: if the podcast cover fails too, the
 * image is left as is instead of looping on `error`. A new `[src]` binding from
 * the host starts over, so reused elements recover on their next item.
 */
@Directive({
  selector: 'img[psPodcastCoverFallback]',
  standalone: true,
  host: { '(error)': 'onError()' },
})
export class PodcastCoverFallbackDirective {
  /** Podcast the item belongs to; no fallback when unset. */
  readonly psPodcastCoverFallback = input<string | undefined>(undefined);

  private readonly img = inject<ElementRef<HTMLImageElement>>(ElementRef).nativeElement;

  protected onError() {
    const podcastId = this.psPodcastCoverFallback();
    if (!podcastId) return;

    const fallback = new URL(podcastCoverUrl(podcastId), document.baseURI).href;
    if (this.img.src === fallback) return;

    this.img.src = fallback;
  }
}
