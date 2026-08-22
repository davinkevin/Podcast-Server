import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import LibraryComponent from './library.component';
import { ItemApi } from '../../core/api/item.api';
import { TagApi } from '../../core/api/tag.api';
import { DownloadStreamService } from '../../core/downloads/download-stream.service';
import { CurrentUrlService } from '../../core/navigation/current-url.service';
import { NavigationOriginService } from '../../core/navigation/navigation-origin.service';
import { PlayerService } from '../../core/player/player.service';
import { VlcService } from '../../core/vlc/vlc.service';
import { ITEM_STATUSES } from '../../core/models/item.model';

// The #273 wiring: the URL is the source of truth for the two filters, and
// `searchInput` is the single place that turns it into an API request. We drive
// the inputs directly and read the computed state, in line with the project's
// lightweight test wiring.

describe('LibraryComponent filters (#273)', () => {
  let component: LibraryComponent;
  let navigate: ReturnType<typeof vi.fn>;
  let url: ReturnType<typeof signal<string>>;

  function setUrl(params: { q?: string; tags?: string; status?: string; page?: string }) {
    const fixture = TestBed.createComponent(LibraryComponent);
    for (const [key, value] of Object.entries(params)) {
      fixture.componentRef.setInput(key, value);
    }
    component = fixture.componentInstance;
  }

  beforeEach(() => {
    navigate = vi.fn();
    url = signal('/library');
    const noopMutation = { mutate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        {
          provide: ItemApi,
          useValue: {
            search: () => ({
              data: signal(undefined),
              isPending: signal(false),
              error: signal(undefined),
            }),
            resetMutation: () => noopMutation,
            deleteMutation: () => noopMutation,
            triggerDownload: () => ({ subscribe: vi.fn() }),
          },
        },
        {
          provide: DownloadStreamService,
          useValue: { downloading: signal([]), queue: signal([]) },
        },
        // Reached only when the panel is rendered, through its tags field.
        { provide: TagApi, useValue: { search: () => ({ data: signal(undefined) }) } },
        { provide: NavigationOriginService, useValue: { remember: vi.fn() } },
        { provide: CurrentUrlService, useValue: { url } },
        { provide: PlayerService, useValue: { currentItem: signal(undefined) } },
        { provide: VlcService, useValue: {} },
        { provide: Router, useValue: { navigate } },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
        { provide: MatDialog, useValue: { open: vi.fn() } },
      ],
    });
  });

  describe('searchInput', () => {
    it('sends no tags and no status by default', () => {
      setUrl({});

      expect(component['searchInput']()).toMatchObject({ tags: [], status: [] });
    });

    it('survives an absent query parameter, which arrives as undefined', () => {
      // `withComponentInputBinding` sets *every* input on each navigation and
      // passes `undefined` for a parameter that is not in the URL. A transform
      // that throws on `undefined` aborts the whole binding loop, so the inputs
      // declared after it are silently never set — which made `?status=` alone
      // do nothing at all until the transforms accepted `undefined`.
      const fixture = TestBed.createComponent(LibraryComponent);
      fixture.componentRef.setInput('tags', undefined);
      fixture.componentRef.setInput('status', undefined);
      component = fixture.componentInstance;

      expect(component['tags']()).toEqual([]);
      expect(component['status']()).toBe('all');
      expect(component['searchInput']()).toMatchObject({ tags: [], status: [] });
    });

    it('binds status even when no other filter parameter is present', () => {
      // The regression above showed up as "status only works alongside tags",
      // because `tags` was declared first and threw before `status` was set.
      setUrl({ status: 'downloaded' });

      expect(component['status']()).toBe('downloaded');
      expect(component['activeFilterCount']()).toBe(1);
    });

    it('forwards the tags from the URL', () => {
      setUrl({ tags: 'tech,dev' });

      expect(component['searchInput']().tags).toEqual(['tech', 'dev']);
    });

    it('ignores blanks and stray separators in the tags parameter', () => {
      setUrl({ tags: ' tech , , dev ,' });

      expect(component['searchInput']().tags).toEqual(['tech', 'dev']);
    });

    it('expands "downloaded" to FINISH alone', () => {
      setUrl({ status: 'downloaded' });

      expect(component['searchInput']().status).toEqual(['FINISH']);
    });

    it('expands "not-downloaded" to every status except FINISH', () => {
      setUrl({ status: 'not-downloaded' });

      expect(component['searchInput']().status).toEqual(
        ITEM_STATUSES.filter((s) => s !== 'FINISH'),
      );
    });

    it('sends an empty status list for "all", so the parameter is omitted', () => {
      // ItemApi.search only sets `status` for a non-empty list, so an empty one
      // means no `status=` on the wire at all.
      setUrl({ status: 'all' });

      expect(component['searchInput']().status).toEqual([]);
    });

    it('falls back to "all" on a status the app does not know', () => {
      // A hand-edited or stale link must not break the page — and must not leak
      // a raw backend status through either.
      setUrl({ status: 'FINISH' });

      expect(component['status']()).toBe('all');
      expect(component['searchInput']().status).toEqual([]);
    });
  });

  describe('applying filters', () => {
    it('navigates with the new parameters and resets the page', () => {
      setUrl({ page: '4' });

      component['onFiltersChanged']({
        tags: [{ name: 'tech' }, { name: 'dev' }],
        status: 'downloaded',
      });

      expect(navigate).toHaveBeenCalledWith([], {
        queryParams: { tags: 'tech,dev', status: 'downloaded', page: 0 },
        queryParamsHandling: 'merge',
      });
    });

    it('drops the parameters instead of sending them empty', () => {
      setUrl({ tags: 'tech', status: 'downloaded' });

      component['onFiltersChanged']({ tags: [], status: 'all' });

      expect(navigate).toHaveBeenCalledWith([], {
        queryParams: { tags: null, status: null, page: 0 },
        queryParamsHandling: 'merge',
      });
    });

    it('keeps the panel open, so the grid can be watched while filters are tuned', () => {
      setUrl({});
      component['panelOpen'].set(true);

      component['onFiltersChanged']({ tags: [{ name: 'tech' }], status: 'all' });

      expect(component['panelOpen']()).toBe(true);
    });

    it('closes the panel without navigating', () => {
      setUrl({});
      component['panelOpen'].set(true);

      component['onClosePanel']();

      expect(component['panelOpen']()).toBe(false);
      expect(navigate).not.toHaveBeenCalled();
    });

    it('survives the navigation a filter change causes', async () => {
      // The overlay is no longer disposed on navigation, since applying a filter
      // *is* one. A query-string change must leave the panel alone.
      const fixture = TestBed.createComponent(LibraryComponent);
      component = fixture.componentInstance;
      component['panelOpen'].set(true);

      url.set('/library?tags=tech');
      await fixture.whenStable();

      expect(component['panelOpen']()).toBe(true);
    });

    it('closes the panel once the URL leaves the library', async () => {
      // ListRouteReuseStrategy detaches this page rather than destroying it, so
      // without this the panel would still be sitting over the next page.
      const fixture = TestBed.createComponent(LibraryComponent);
      component = fixture.componentInstance;
      component['panelOpen'].set(true);

      url.set('/podcasts/abc');
      await fixture.whenStable();

      expect(component['panelOpen']()).toBe(false);
    });
  });

  describe('the active filter summary', () => {
    it('is absent when nothing is filtering', () => {
      setUrl({});

      expect(component['hasActiveFilters']()).toBe(false);
      expect(component['activeFilterCount']()).toBe(0);
    });

    it('counts one entry per criterion', () => {
      setUrl({ tags: 'tech,dev', status: 'downloaded' });

      expect(component['activeFilterCount']()).toBe(3);
      expect(component['statusFilterLabel']()).toBe('Downloaded');
    });

    it('does not count the text query, which has its own field', () => {
      setUrl({ q: 'kotlin' });

      expect(component['hasActiveFilters']()).toBe(false);
    });

    it('applies a tag removal immediately, keeping the other criteria', () => {
      setUrl({ tags: 'tech,dev', status: 'downloaded' });

      component['onRemoveTagFilter']('tech');

      expect(navigate).toHaveBeenCalledWith([], {
        queryParams: { tags: 'dev', status: 'downloaded', page: 0 },
        queryParamsHandling: 'merge',
      });
    });

    it('applies a status removal immediately, keeping the tags', () => {
      setUrl({ tags: 'tech', status: 'not-downloaded' });

      component['onRemoveStatusFilter']();

      expect(navigate).toHaveBeenCalledWith([], {
        queryParams: { tags: 'tech', status: null, page: 0 },
        queryParamsHandling: 'merge',
      });
    });

    it('clears every criterion at once', () => {
      setUrl({ tags: 'tech,dev', status: 'downloaded', q: 'kotlin' });

      component['onClearFilters']();

      // `q` is untouched: merge keeps it, and the text field owns it.
      expect(navigate).toHaveBeenCalledWith([], {
        queryParams: { tags: null, status: null, page: 0 },
        queryParamsHandling: 'merge',
      });
    });
  });

  describe('the panel input', () => {
    it('hands the panel the committed tags as tag objects', () => {
      setUrl({ tags: 'tech,dev' });

      expect(component['tagFilters']()).toEqual([{ name: 'tech' }, { name: 'dev' }]);
    });
  });

  describe('the chevron', () => {
    it('gets focus back once the overlay has detached', async () => {
      // Rendered here rather than driven through signals: the point is that the
      // view query resolves to the button element and not to MatIconButton, so
      // a keyboard user lands back where they opened from. That the detach event
      // is what calls this is covered end to end — focusing any earlier is undone
      // by the popover element leaving the DOM.
      const fixture = TestBed.createComponent(LibraryComponent);
      await fixture.whenStable();
      component = fixture.componentInstance;

      const chevron = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
        'button[aria-label="More filters"]',
      );
      expect(chevron).not.toBeNull();

      component['panelOpen'].set(true);
      await fixture.whenStable();
      component['onPanelDetached']();

      expect(component['panelOpen']()).toBe(false);
      expect(document.activeElement).toBe(chevron);
    });
  });
});
