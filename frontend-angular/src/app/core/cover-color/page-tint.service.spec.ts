import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { CurrentUrlService } from '../navigation/current-url.service';
import { SettingsService } from '../settings/settings.service';
import { CoverPalette } from './cover-color.service';
import { PageTintService } from './page-tint.service';

const PALETTE: CoverPalette = {
  vibrant: { hex: '#123456', titleText: '#ffffff', bodyText: '#eeeeee' },
  darkVibrant: null,
  lightVibrant: null,
  muted: null,
  darkMuted: null,
  lightMuted: null,
};

function tint(): string {
  return document.documentElement.style.getPropertyValue('--page-tint');
}

describe('PageTintService', () => {
  const url = signal('/podcasts/A');

  beforeEach(() => {
    url.set('/podcasts/A');
    TestBed.configureTestingModule({
      providers: [
        { provide: CurrentUrlService, useValue: { url } },
        { provide: SettingsService, useValue: { effectiveTheme: () => 'light' } },
      ],
    });
  });

  afterEach(() => {
    document.documentElement.removeAttribute('style');
  });

  it('applies the tint while the claimed url is the active one', () => {
    const service = TestBed.inject(PageTintService);

    service.claim('/podcasts/A', PALETTE);
    TestBed.flushEffects();

    expect(tint()).toBe('#123456');
  });

  it('clears the tint when navigation leaves the claimed url', () => {
    const service = TestBed.inject(PageTintService);
    service.claim('/podcasts/A', PALETTE);
    TestBed.flushEffects();

    url.set('/library');
    TestBed.flushEffects();

    expect(tint()).toBe('');
  });

  it('ignores a claim made for a url that is not the active one', () => {
    const service = TestBed.inject(PageTintService);

    service.claim('/podcasts/B', PALETTE);
    TestBed.flushEffects();

    expect(tint()).toBe('');
  });

  it('re-applies the tint when navigating back to the claimed url', () => {
    const service = TestBed.inject(PageTintService);
    service.claim('/podcasts/A', PALETTE);
    TestBed.flushEffects();

    url.set('/podcasts/A/items/i1');
    TestBed.flushEffects();
    expect(tint()).toBe('');

    url.set('/podcasts/A');
    TestBed.flushEffects();

    expect(tint()).toBe('#123456');
  });
});
