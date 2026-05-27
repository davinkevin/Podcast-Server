import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  signal,
  ViewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PodcastApi } from '../api/podcast.api';
import { PlaylistApi } from '../api/playlist.api';
import { DownloadApi } from '../api/download.api';
import { SettingsService, ThemePreference } from '../settings/settings.service';
import { PodcastCreateDialogComponent } from '../../features/podcasts/podcast-create-dialog.component';
import { PlaylistCreateDialogComponent } from '../../features/playlists/playlist-create-dialog.component';
import { PodcastHAL } from '../models/podcast.model';
import { PlaylistWithItemsHAL } from '../models/playlist.model';

interface CommandItem {
  readonly section: 'Navigation' | 'Podcasts' | 'Playlists' | 'Actions';
  readonly label: string;
  readonly hint?: string;
  readonly icon: string;
  readonly keywords?: readonly string[];
  readonly run: () => void;
}

@Component({
  selector: 'ps-command-palette',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatIconModule],
  template: `
    <div class="palette">
      <div class="palette__search">
        <mat-icon class="palette__search-icon">search</mat-icon>
        <input
          #searchInput
          type="text"
          class="palette__input"
          placeholder="Type a command or page…"
          [ngModel]="query()"
          (ngModelChange)="onQueryChange($event)"
          (keydown)="onKey($event)"
          aria-label="Command palette search"
          autocomplete="off"
          spellcheck="false"
        />
      </div>

      @if (groups().length === 0) {
        <p class="palette__empty">No matches.</p>
      } @else {
        <ul class="palette__list" #list>
          @for (group of groups(); track group.title) {
            <li class="palette__section">{{ group.title }}</li>
            @for (item of group.items; track item.label; let idx = $index) {
              @let absIndex = group.startIndex + idx;
              <li>
                <button
                  type="button"
                  class="palette__item"
                  [class.palette__item--active]="absIndex === activeIndex()"
                  (click)="run(item)"
                  (mousemove)="setActive(absIndex)"
                  [attr.aria-selected]="absIndex === activeIndex()"
                >
                  <mat-icon class="palette__item-icon">{{ item.icon }}</mat-icon>
                  <span class="palette__item-label">{{ item.label }}</span>
                  @if (item.hint) {
                    <span class="palette__item-hint">{{ item.hint }}</span>
                  }
                </button>
              </li>
            }
          }
        </ul>
      }

      <div class="palette__footer">
        <span><kbd>↑</kbd><kbd>↓</kbd> Navigate</span>
        <span><kbd>↵</kbd> Select</span>
        <span><kbd>Esc</kbd> Close</span>
      </div>
    </div>
  `,
  styles: `
    .palette {
      display: flex;
      flex-direction: column;
      max-height: 70vh;
    }
    .palette__search {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1rem 1.25rem;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .palette__search-icon {
      color: var(--mat-sys-on-surface-variant);
      flex: 0 0 auto;
    }
    .palette__input {
      flex: 1 1 auto;
      min-width: 0;
      background: transparent;
      border: 0;
      outline: 0;
      color: inherit;
      font: var(--mat-sys-body-large);
    }
    .palette__list {
      list-style: none;
      margin: 0;
      padding: 0.5rem 0;
      overflow-y: auto;
      flex: 1 1 auto;
    }
    .palette__section {
      font: var(--mat-sys-label-small);
      color: var(--mat-sys-on-surface-variant);
      text-transform: uppercase;
      letter-spacing: 0.05em;
      padding: 0.5rem 1.25rem 0.25rem;
    }
    .palette__section:not(:first-child) {
      margin-top: 0.25rem;
    }
    .palette__item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      width: 100%;
      padding: 0.5rem 1.25rem;
      background: transparent;
      border: 0;
      cursor: pointer;
      color: inherit;
      text-align: left;
      font: var(--mat-sys-body-medium);
    }
    .palette__item--active {
      background: color-mix(in srgb, var(--mat-sys-primary) 14%, transparent);
    }
    .palette__item-icon {
      color: var(--mat-sys-on-surface-variant);
      flex: 0 0 auto;
    }
    .palette__item--active .palette__item-icon {
      color: var(--mat-sys-primary);
    }
    .palette__item-label {
      flex: 1 1 auto;
      min-width: 0;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .palette__item-hint {
      font: var(--mat-sys-body-small);
      color: var(--mat-sys-on-surface-variant);
      white-space: nowrap;
    }
    .palette__empty {
      padding: 2rem 1.25rem;
      text-align: center;
      color: var(--mat-sys-on-surface-variant);
      margin: 0;
    }
    .palette__footer {
      display: flex;
      gap: 1rem;
      padding: 0.5rem 1.25rem;
      border-top: 1px solid var(--mat-sys-outline-variant);
      font: var(--mat-sys-label-small);
      color: var(--mat-sys-on-surface-variant);
    }
    .palette__footer kbd {
      display: inline-block;
      padding: 0 0.35em;
      margin-right: 0.25em;
      border-radius: 4px;
      background: var(--mat-sys-surface-container-highest);
      color: var(--mat-sys-on-surface);
      font: var(--mat-sys-label-small);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommandPaletteComponent implements AfterViewInit {
  private readonly dialogRef = inject(MatDialogRef<CommandPaletteComponent>);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly podcastApi = inject(PodcastApi);
  private readonly playlistApi = inject(PlaylistApi);
  private readonly downloadApi = inject(DownloadApi);
  private readonly settings = inject(SettingsService);
  private readonly snackbar = inject(MatSnackBar);

  @ViewChild('searchInput') private searchInput?: ElementRef<HTMLInputElement>;
  @ViewChild('list') private listEl?: ElementRef<HTMLUListElement>;

  protected readonly query = signal('');
  // The raw "intent" index; the displayed active row is clamped via the
  // `activeIndex` computed below so it never points past the filtered list.
  private readonly intentIndex = signal(0);

  // TanStack Query: returns whatever is in the cache immediately (empty if
  // never visited), and refetches in the background. The palette is usable
  // straight away with static nav + actions even before lists land.
  protected readonly podcastsQuery = this.podcastApi.list();
  protected readonly playlistsQuery = this.playlistApi.list();

  // Cap on dynamic items (podcasts, playlists) when the search field is
  // empty — without it, libraries with dozens of feeds drown the Actions
  // section and the palette stops looking like a command list. Once the
  // user types anything, the cap is lifted so the filter searches the
  // full corpus.
  private static readonly EMPTY_SECTION_CAP = 5;

  // Flat list of every candidate (nav + dynamic + actions), pre-filter.
  private readonly allItems = computed<readonly CommandItem[]>(() => {
    const hasQuery = this.query().trim().length > 0;
    const cap = CommandPaletteComponent.EMPTY_SECTION_CAP;

    const items: CommandItem[] = [
      {
        section: 'Navigation',
        label: 'Spotlight',
        icon: 'auto_awesome',
        keywords: ['home', 'landing', 'digest', 'fresh', 'new', 'discover', 'recent'],
        run: () => this.go(['/']),
      },
      {
        section: 'Navigation',
        label: 'Library',
        icon: 'library_music',
        keywords: ['items', 'episodes', 'all'],
        run: () => this.go(['/library']),
      },
      {
        section: 'Navigation',
        label: 'Podcasts',
        icon: 'podcasts',
        keywords: ['feeds', 'shows'],
        run: () => this.go(['/podcasts']),
      },
      {
        section: 'Navigation',
        label: 'Playlists',
        icon: 'playlist_play',
        keywords: ['queues'],
        run: () => this.go(['/playlists']),
      },
      {
        section: 'Navigation',
        label: 'Settings',
        icon: 'settings',
        keywords: ['preferences', 'options', 'about'],
        run: () => this.go(['/settings']),
      },
    ];

    // Podcasts sorted by lastUpdate DESC (nulls last) to match the /podcasts
    // page convention — the most recently active feeds bubble up to the cap.
    const podcasts = [...(this.podcastsQuery.data()?.content ?? [])].sort(
      (a, b) => {
        const at = a.lastUpdate ? Date.parse(a.lastUpdate) : Number.NEGATIVE_INFINITY;
        const bt = b.lastUpdate ? Date.parse(b.lastUpdate) : Number.NEGATIVE_INFINITY;
        return bt - at;
      },
    );
    const podcastsToShow = hasQuery ? podcasts : podcasts.slice(0, cap);
    for (const p of podcastsToShow) {
      items.push({
        section: 'Podcasts',
        label: p.title,
        icon: 'podcasts',
        hint: 'Open podcast',
        run: () => this.go(['/podcasts', p.id]),
      });
    }

    const playlists = this.playlistsQuery.data()?.content ?? [];
    const playlistsToShow = hasQuery ? playlists : playlists.slice(0, cap);
    for (const p of playlistsToShow) {
      items.push({
        section: 'Playlists',
        label: p.name,
        icon: 'playlist_play',
        hint: 'Open playlist',
        run: () => this.go(['/playlists', p.id]),
      });
    }

    items.push(
      {
        section: 'Actions',
        label: 'Add podcast',
        icon: 'add',
        keywords: ['create', 'new', 'subscribe'],
        run: () => this.addPodcast(),
      },
      {
        section: 'Actions',
        label: 'Create playlist',
        icon: 'playlist_add',
        keywords: ['new'],
        run: () => this.createPlaylist(),
      },
      {
        section: 'Actions',
        label: 'Update all podcasts',
        icon: 'refresh',
        keywords: ['fetch', 'sync'],
        run: () => this.updateAll(false),
      },
      {
        section: 'Actions',
        label: 'Update all & download',
        icon: 'cloud_download',
        keywords: ['fetch', 'sync', 'grab'],
        run: () => this.updateAll(true),
      },
      {
        section: 'Actions',
        label: 'Clear download queue',
        icon: 'playlist_remove',
        keywords: ['empty', 'cancel'],
        run: () => this.clearQueue(),
      },
      {
        section: 'Actions',
        label: 'Stop all downloads',
        icon: 'stop_circle',
        keywords: ['abort', 'cancel'],
        run: () => this.stopAll(),
      },
      {
        section: 'Actions',
        label: 'Export OPML',
        icon: 'file_download',
        keywords: ['backup', 'subscriptions'],
        run: () => this.exportOpml(),
      },
      {
        section: 'Actions',
        label: this.themeLabel(),
        icon: 'palette',
        keywords: ['dark', 'light', 'system', 'appearance'],
        run: () => this.cycleTheme(),
      },
      {
        section: 'Actions',
        label: this.settings.sidenavMode() === 'rail' ? 'Expand sidebar' : 'Collapse sidebar',
        icon: 'view_sidebar',
        keywords: ['sidenav', 'rail', 'navigation'],
        run: () => this.toggleSidenav(),
      },
    );

    return items;
  });

  protected readonly filtered = computed<readonly CommandItem[]>(() => {
    const q = this.query().trim().toLowerCase();
    if (!q) return this.allItems();
    return this.allItems().filter((i) => {
      const haystack = [i.label, ...(i.keywords ?? [])].join(' ').toLowerCase();
      return haystack.includes(q);
    });
  });

  // Display structure: section -> items, with a precomputed `startIndex` so
  // the template can resolve each item's absolute index in `filtered()` for
  // keyboard highlighting.
  protected readonly groups = computed(() => {
    const result: {
      title: string;
      startIndex: number;
      items: CommandItem[];
    }[] = [];
    let i = 0;
    for (const item of this.filtered()) {
      const last = result[result.length - 1];
      if (last && last.title === item.section) {
        last.items.push(item);
      } else {
        result.push({ title: item.section, startIndex: i, items: [item] });
      }
      i++;
    }
    return result;
  });

  // The active index, clamped to the filtered length. Source of truth for
  // both keyboard highlight and `Enter` invocation.
  protected readonly activeIndex = computed(() => {
    const len = this.filtered().length;
    if (len === 0) return 0;
    return Math.min(Math.max(0, this.intentIndex()), len - 1);
  });

  constructor() {
    // Keep the active row in view as the user navigates with the keyboard.
    effect(() => {
      const idx = this.activeIndex();
      const list = this.listEl?.nativeElement;
      if (!list) return;
      const rows = list.querySelectorAll<HTMLElement>('.palette__item');
      rows[idx]?.scrollIntoView({ block: 'nearest' });
    });
  }

  ngAfterViewInit(): void {
    // Autofocus the search input. Using afterViewInit (not the input's
    // autofocus attribute) so MatDialog's own focus trap doesn't steal it.
    queueMicrotask(() => this.searchInput?.nativeElement.focus());
  }

  protected onQueryChange(v: string) {
    this.query.set(v);
    this.intentIndex.set(0);
  }

  protected setActive(index: number) {
    this.intentIndex.set(index);
  }

  protected onKey(event: KeyboardEvent) {
    const len = this.filtered().length;
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (len > 0)
        this.intentIndex.update(() => (this.activeIndex() + 1) % len);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      if (len > 0)
        this.intentIndex.update(() => (this.activeIndex() - 1 + len) % len);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      if (len === 0) return;
      const item = this.filtered()[this.activeIndex()];
      if (item) this.run(item);
    }
  }

  protected run(item: CommandItem) {
    this.dialogRef.close();
    // Defer to next microtask so the dialog close is committed before any
    // follow-up dialog/navigation runs — keeps focus restoration sane.
    queueMicrotask(() => item.run());
  }

  // -------- action handlers --------

  private go(commands: readonly string[]) {
    this.router.navigate([...commands]);
  }

  private addPodcast() {
    this.dialog
      .open(PodcastCreateDialogComponent, {
        autoFocus: 'first-tabbable',
        panelClass: 'ps-fitting-dialog',
      })
      .afterClosed()
      .subscribe((created: PodcastHAL | undefined) => {
        if (!created) return;
        this.router.navigate(['/podcasts', created.id]);
      });
  }

  private createPlaylist() {
    this.dialog
      .open(PlaylistCreateDialogComponent, {
        autoFocus: 'first-tabbable',
        panelClass: 'ps-fitting-dialog',
      })
      .afterClosed()
      .subscribe((created: PlaylistWithItemsHAL | undefined) => {
        if (!created) return;
        this.router.navigate(['/playlists', created.id]);
      });
  }

  private updateAll(withDownload: boolean) {
    this.podcastApi.updateAll({ download: withDownload }).subscribe({
      next: () =>
        this.snackbar.open(
          withDownload ? 'Update & download started' : 'Update started',
          undefined,
          { duration: 2500 },
        ),
      error: () =>
        this.snackbar.open('Could not start update', 'Dismiss', { duration: 4000 }),
    });
  }

  private clearQueue() {
    this.downloadApi.emptyQueue().subscribe({
      next: () =>
        this.snackbar.open('Download queue cleared', undefined, { duration: 2500 }),
      error: () =>
        this.snackbar.open('Could not clear queue', 'Dismiss', { duration: 4000 }),
    });
  }

  private stopAll() {
    this.downloadApi.stopAll().subscribe({
      next: () =>
        this.snackbar.open('All downloads stopped', undefined, { duration: 2500 }),
      error: () =>
        this.snackbar.open('Could not stop downloads', 'Dismiss', { duration: 4000 }),
    });
  }

  private exportOpml() {
    // Same pattern as SettingsComponent.onDownloadOpml — backend serves XML
    // without Content-Disposition, so we synthesize the download.
    fetch('/api/v1/podcasts/opml')
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.blob();
      })
      .then((blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'podcast-server.opml';
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
      })
      .catch(() =>
        this.snackbar.open('Could not export OPML', 'Dismiss', { duration: 4000 }),
      );
  }

  private themeLabel(): string {
    const next: Record<ThemePreference, string> = {
      system: 'Switch theme to light',
      light: 'Switch theme to dark',
      dark: 'Switch theme to system',
    };
    return next[this.settings.theme()];
  }

  private cycleTheme() {
    const order: ThemePreference[] = ['system', 'light', 'dark'];
    const current = this.settings.theme();
    const idx = order.indexOf(current);
    const next = order[(idx + 1) % order.length];
    this.settings.setTheme(next);
    this.snackbar.open(`Theme: ${next}`, undefined, { duration: 1500 });
  }

  private toggleSidenav() {
    this.settings.toggleSidenavMode();
  }
}
