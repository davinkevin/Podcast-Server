import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CoverCardComponent } from '../../shared/cover-card/cover-card.component';
import { PagerComponent } from '../../shared/pager/pager.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import {
  StatusBadgeComponent,
  StatusBadgeKind,
} from '../../shared/status-badge/status-badge.component';
import { ItemApi, ItemSearchInput } from '../../core/api/item.api';
import { ItemHAL } from '../../core/models/item.model';
import { DownloadStreamService } from '../../core/downloads/download-stream.service';

const DEFAULT_PAGE_SIZE = 24;

@Component({
  selector: 'ps-library',
  standalone: true,
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    CoverCardComponent,
    PagerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './library.component.html',
  styleUrl: './library.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class LibraryComponent {
  // Bound to /library?q=&page=&sort= via withComponentInputBinding().
  readonly q = input<string>('');
  readonly page = input<number, string | number>(0, {
    transform: (v) => {
      const n = typeof v === 'number' ? v : Number.parseInt(v, 10);
      return Number.isFinite(n) && n >= 0 ? n : 0;
    },
  });
  readonly sort = input<string>('pubDate,DESC');

  private readonly router = inject(Router);
  private readonly itemApi = inject(ItemApi);
  private readonly stream = inject(DownloadStreamService);

  protected readonly searchDraft = signal('');

  protected readonly searchInput = computed<ItemSearchInput>(() => ({
    q: this.q(),
    page: this.page(),
    size: DEFAULT_PAGE_SIZE,
    sort: this.sort(),
  }));

  protected readonly itemsResource = this.itemApi.search(this.searchInput);

  protected readonly cardActions = [
    { label: 'Open podcast', icon: 'open_in_new' },
    { label: 'Add to playlist', icon: 'playlist_add' },
  ] as const;

  constructor() {
    // Mirror the URL into the search input on first load / direct nav.
    this.searchDraft.set(this.q());
  }

  protected onSubmitSearch() {
    const q = this.searchDraft().trim();
    this.router.navigate([], {
      queryParams: { q: q || null, page: 0 },
      queryParamsHandling: 'merge',
    });
  }

  protected onClearSearch() {
    this.searchDraft.set('');
    this.router.navigate([], {
      queryParams: { q: null, page: 0 },
      queryParamsHandling: 'merge',
    });
  }

  protected coverUrl(item: ItemHAL): string {
    return item.cover.url;
  }

  protected statusFor(item: ItemHAL): { kind: StatusBadgeKind; progression: number | null } | null {
    const downloading = this.stream.downloading().find((d) => d.id === item.id);
    if (downloading) {
      return { kind: 'downloading', progression: downloading.progression };
    }
    if (this.stream.queue().some((q) => q.id === item.id)) {
      return { kind: 'queued', progression: null };
    }
    if (item.status === 'FAILED') {
      return { kind: 'failed', progression: null };
    }
    return null;
  }

  protected isInProgress(item: ItemHAL): boolean {
    return (
      this.stream.downloading().some((d) => d.id === item.id) ||
      this.stream.queue().some((q) => q.id === item.id)
    );
  }

  protected onPlay(item: ItemHAL) {
    // PR 5: PlayerService.open(item).
    console.debug('[library] play', item.id, item.proxyURL);
  }

  protected onDownload(item: ItemHAL) {
    this.itemApi.triggerDownload(item.podcastId, item.id).subscribe();
  }

  protected onOpen(item: ItemHAL) {
    // PR 5: navigate to /podcasts/:id/items/:itemId.
    console.debug('[library] open', item.id);
  }
}
