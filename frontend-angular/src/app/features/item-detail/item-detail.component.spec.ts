import { signal, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { beforeEach, describe, expect, it } from 'vitest';

import ItemDetailComponent from './item-detail.component';
import { ItemApi } from '../../core/api/item.api';
import { PlaylistApi } from '../../core/api/playlist.api';
import { DownloadStreamService } from '../../core/downloads/download-stream.service';
import { PlayerService } from '../../core/player/player.service';
import { VlcService } from '../../core/vlc/vlc.service';
import { CoverColorService } from '../../core/cover-color/cover-color.service';
import { PageTintService } from '../../core/cover-color/page-tint.service';
import { NavigationOriginService } from '../../core/navigation/navigation-origin.service';
import { ItemHAL } from '../../core/models/item.model';
import { DownloadingItemHAL } from '../../core/models/downloading-item.model';

// These specs cover the #260 logic in isolation: the FAB must turn into a
// loader (instead of vanishing) while a download is active, the loader label
// must reflect the downloading/queued state, and the cover progress badge
// must not duplicate the percentage already shown by the loader. We drive the
// component's source signals directly and read its computed state — no full
// template render, in line with the project's lightweight test wiring.

const ITEM_ID = 'item-1';

function makeItem(overrides: Partial<ItemHAL> = {}): ItemHAL {
  return {
    id: ITEM_ID,
    title: 'Episode 1',
    url: 'https://example.com/ep1.mp3',
    pubDate: null,
    downloadDate: null,
    creationDate: '2024-01-01T00:00:00Z',
    description: '',
    mimeType: 'audio/mpeg',
    length: null,
    fileName: null,
    status: 'NOT_DOWNLOADED',
    podcast: { id: 'pod-1', title: 'A podcast', url: 'https://example.com' },
    cover: { id: 'c1', width: 200, height: 200, url: 'u', proxyURL: 'p' },
    isDownloaded: false,
    podcastId: 'pod-1',
    proxyURL: 'https://example.com/proxy/ep1.mp3',
    ...overrides,
  };
}

function makeDownloading(progression: number): DownloadingItemHAL {
  return {
    id: ITEM_ID,
    title: 'Episode 1',
    status: 'STARTED',
    url: 'https://example.com/ep1.mp3',
    progression,
    isDownloaded: false,
    podcast: { id: 'pod-1', title: 'A podcast' },
    cover: { id: 'c1', url: 'u', proxyURL: 'p' },
  };
}

describe('ItemDetailComponent download loader (#260)', () => {
  let item: WritableSignal<ItemHAL | undefined>;
  let downloading: WritableSignal<readonly DownloadingItemHAL[]>;
  let queue: WritableSignal<readonly DownloadingItemHAL[]>;
  let component: any;

  beforeEach(() => {
    item = signal<ItemHAL | undefined>(makeItem());
    downloading = signal<readonly DownloadingItemHAL[]>([]);
    queue = signal<readonly DownloadingItemHAL[]>([]);

    const itemQuery = { data: item, error: signal(undefined), isSuccess: signal(true) };
    const noopMutation = { mutate: () => {} };

    TestBed.configureTestingModule({
      providers: [
        {
          provide: ItemApi,
          useValue: {
            getById: () => itemQuery,
            resetMutation: () => noopMutation,
            deleteMutation: () => noopMutation,
            triggerDownload: () => ({ subscribe: () => {} }),
          },
        },
        {
          provide: PlaylistApi,
          useValue: {
            getById: () => ({ data: signal(undefined), isSuccess: signal(false) }),
            removeItemMutation: () => noopMutation,
          },
        },
        { provide: DownloadStreamService, useValue: { downloading, queue } },
        { provide: NavigationOriginService, useValue: { consume: () => null } },
        { provide: CoverColorService, useValue: { extract: () => Promise.resolve(null) } },
        { provide: PageTintService, useValue: { currentUrl: () => '/', claim: () => {} } },
        { provide: PlayerService, useValue: {} },
        { provide: VlcService, useValue: {} },
        { provide: Title, useValue: { setTitle: () => {} } },
        { provide: Router, useValue: { navigate: () => {} } },
        { provide: MatSnackBar, useValue: { open: () => {} } },
        { provide: MatDialog, useValue: { open: () => {} } },
      ],
    });

    // No detectChanges: we read computed state only, so the constructor
    // effects (cover tint, title, settling bridge) stay dormant and we avoid
    // wiring the full template.
    const fixture = TestBed.createComponent(ItemDetailComponent);
    fixture.componentRef.setInput('id', ITEM_ID);
    fixture.componentRef.setInput('idPodcast', 'pod-1');
    component = fixture.componentInstance;
  });

  it('shows no loader and no badge when the item is not downloaded and idle', () => {
    expect(component.inProgress()).toBe(false);
    expect(component.showLoader()).toBe(false);
    expect(component.statusBadge()).toBeNull();
  });

  it('turns the FAB into a loader with the percentage while downloading', () => {
    downloading.set([makeDownloading(42)]);

    expect(component.inProgress()).toBe(true);
    expect(component.showLoader()).toBe(true);
    expect(component.loaderLabel()).toBe('Downloading… 42%');
    expect(component.statusBadge()).toEqual({ kind: 'downloading', progression: 42 });
  });

  it('shows a "Queued…" loader while waiting in the download queue', () => {
    queue.set([makeDownloading(0)]);

    expect(component.showLoader()).toBe(true);
    expect(component.loaderLabel()).toBe('Queued…');
    expect(component.statusBadge()).toEqual({ kind: 'queued', progression: null });
  });

  it('drops the loader once the item is downloaded', () => {
    item.set(makeItem({ isDownloaded: true, status: 'FINISH' }));

    expect(component.inProgress()).toBe(false);
    expect(component.showLoader()).toBe(false);
    expect(component.statusBadge()).toBeNull();
  });

  it('surfaces a failed badge but no loader so the Download button can retry', () => {
    item.set(makeItem({ status: 'FAILED' }));

    expect(component.showLoader()).toBe(false);
    expect(component.statusBadge()).toEqual({ kind: 'failed', progression: null });
  });
});
