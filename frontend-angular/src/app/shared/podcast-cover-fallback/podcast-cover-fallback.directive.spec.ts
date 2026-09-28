import { Component, provideZonelessChangeDetection, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { PodcastCoverFallbackDirective } from './podcast-cover-fallback.directive';

// Once the nightly cleanup deletes an item cover from the bucket, the item
// cover route redirects to the remote URL, which is often dead for old items.
// The podcast cover is kept, so it is the one to show instead of a broken image.

@Component({
  standalone: true,
  imports: [PodcastCoverFallbackDirective],
  template: `<img [src]="src()" [psPodcastCoverFallback]="podcastId()" alt="" />`,
})
class HostComponent {
  readonly src = signal('/api/v1/podcasts/p1/items/i1/cover.png');
  readonly podcastId = signal<string | undefined>('p1');
}

describe('PodcastCoverFallbackDirective', () => {
  const podcastCover = new URL('/api/v1/podcasts/p1/cover.jpg', document.baseURI).href;

  async function render() {
    const fixture = TestBed.createComponent(HostComponent);
    await fixture.whenStable();
    const img = (fixture.nativeElement as HTMLElement).querySelector('img')!;
    return { fixture, img };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
  });

  it('keeps the item cover while it loads', async () => {
    const { img } = await render();

    expect(img.src).toContain('/api/v1/podcasts/p1/items/i1/cover.png');
  });

  it('switches to the podcast cover when the item cover fails', async () => {
    const { img } = await render();

    img.dispatchEvent(new Event('error'));

    expect(img.src).toBe(podcastCover);
  });

  it('stops there when the podcast cover fails too', async () => {
    const { img } = await render();
    img.dispatchEvent(new Event('error'));
    const swapped = img.src;

    img.dispatchEvent(new Event('error'));

    expect(img.src).toBe(swapped);
  });

  it('leaves the image alone without a podcast', async () => {
    const { fixture, img } = await render();
    fixture.componentInstance.podcastId.set(undefined);
    await fixture.whenStable();

    img.dispatchEvent(new Event('error'));

    expect(img.src).toContain('/items/i1/cover.png');
  });

  it('tries the next item cover again when the element is reused', async () => {
    const { fixture, img } = await render();
    img.dispatchEvent(new Event('error'));

    fixture.componentInstance.src.set('/api/v1/podcasts/p1/items/i2/cover.png');
    await fixture.whenStable();

    expect(img.src).toContain('/items/i2/cover.png');
  });
});
