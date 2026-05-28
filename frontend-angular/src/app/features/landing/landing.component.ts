import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  HostListener,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CoverCardComponent } from '../../shared/cover-card/cover-card.component';
import { ContentRowComponent } from '../../shared/content-row/content-row.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { DigestApi, DigestInput } from '../../core/api/digest.api';
import { ItemApi } from '../../core/api/item.api';
import { ItemHAL } from '../../core/models/item.model';
import { PlayerService } from '../../core/player/player.service';
import { CoverColorService } from '../../core/cover-color/cover-color.service';

/** The mixed grid's cell sizing — mirrored from the SCSS so the JS column
 *  count matches CSS `auto-fill` exactly (needed to drop the expansion panel
 *  at the end of the right row). */
const CELL_MIN_PX = 168;
const GRID_GAP_PX = 20; // 1.25rem

const ONE_DAY_MS = 24 * 60 * 60 * 1000;

/** UI period toggle → ISO-8601 duration sent to the backend. */
type Period = 'PT24H' | 'P7D' | 'P30D';
interface PeriodOption {
  readonly value: Period;
  readonly label: string;
}
const PERIODS: readonly PeriodOption[] = [
  { value: 'PT24H', label: 'Day' },
  { value: 'P7D', label: 'Week' },
  { value: 'P30D', label: 'Month' },
];

interface ShowEntry {
  readonly kind: 'show';
  readonly id: string;
  readonly podcastId: string;
  readonly title: string;
  readonly coverUrl: string;
  readonly itemCount: number;
  readonly items: readonly ItemHAL[];
}
interface EpisodeEntry {
  readonly kind: 'episode';
  readonly id: string;
  readonly item: ItemHAL;
  readonly podcastCoverUrl: string;
}
type Entry = ShowEntry | EpisodeEntry;

@Component({
  selector: 'ps-landing',
  standalone: true,
  imports: [
    RouterLink,
    MatIconModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatProgressSpinnerModule,
    CoverCardComponent,
    ContentRowComponent,
    EmptyStateComponent,
  ],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class LandingComponent {
  private readonly router = inject(Router);
  private readonly digestApi = inject(DigestApi);
  private readonly itemApi = inject(ItemApi);
  private readonly coverColor = inject(CoverColorService);
  protected readonly player = inject(PlayerService);

  protected readonly periods = PERIODS;
  protected readonly period = signal<Period>('PT24H');

  protected readonly digestQuery = this.digestApi.get(
    computed<DigestInput>(() => ({ within: this.period() })),
  );

  private readonly gridRef = viewChild<ElementRef<HTMLElement>>('grid');
  protected readonly columns = signal(2);
  protected readonly expandedId = signal<string | null>(null);

  // Hex tint extracted from the expanded show's cover, applied as the
  // expansion panel's background wash. Null = no tint.
  protected readonly expandedTint = signal<string | null>(null);

  constructor() {
    afterNextRender(() => this.measureColumns());

    // Collapse any open row when the period changes (its entry may be gone).
    effect(() => {
      this.period();
      this.expandedId.set(null);
    });

    // Pull a vibrant color off the open show's cover for the panel wash.
    effect(() => {
      const show = this.expandedShow();
      if (!show) {
        this.expandedTint.set(null);
        return;
      }
      this.coverColor.extract(show.coverUrl).then((palette) => {
        if (this.expandedShow()?.id !== show.id) return;
        this.expandedTint.set(
          palette?.vibrant?.hex ??
            palette?.darkVibrant?.hex ??
            palette?.muted?.hex ??
            null,
        );
      });
    });
  }

  @HostListener('window:resize')
  protected onResize() {
    this.measureColumns();
  }

  private measureColumns() {
    const el = this.gridRef()?.nativeElement;
    if (!el) return;
    const w = el.clientWidth;
    const cols = Math.floor((w + GRID_GAP_PX) / (CELL_MIN_PX + GRID_GAP_PX));
    this.columns.set(Math.max(1, cols));
  }

  // The digest payload is already grouped per podcast and freshness-ordered
  // by the backend. A podcast with 2+ items collapses to a show tile that
  // expands inline; a one-off drop shows the episode itself.
  protected readonly entries = computed<readonly Entry[]>(() => {
    const content = this.digestQuery.data()?.content ?? [];
    return content.map((p): Entry => {
      if (p.itemCount >= 2) {
        return {
          kind: 'show',
          id: 's-' + p.id,
          podcastId: p.id,
          title: p.title,
          coverUrl: p.cover.proxyURL,
          itemCount: p.itemCount,
          items: p.items,
        };
      }
      return {
        kind: 'episode',
        id: 'e-' + p.items[0].id,
        item: p.items[0],
        podcastCoverUrl: p.cover.proxyURL,
      };
    });
  });

  protected readonly isEmpty = computed(() => this.entries().length === 0);

  // Spotlight: the freshest standalone drops — the "something important".
  protected readonly spotlight = computed<readonly ItemHAL[]>(() =>
    this.entries()
      .filter((e): e is EpisodeEntry => e.kind === 'episode')
      .map((e) => e.item)
      .slice(0, 6),
  );

  protected readonly expansionIndex = computed<number>(() => {
    const id = this.expandedId();
    if (!id) return -1;
    const list = this.entries();
    const idx = list.findIndex((e) => e.id === id);
    if (idx < 0) return -1;
    const cols = this.columns();
    const rowEnd = (Math.floor(idx / cols) + 1) * cols - 1;
    return Math.min(rowEnd, list.length - 1);
  });

  protected readonly expandedShow = computed<ShowEntry | null>(() => {
    const id = this.expandedId();
    if (!id) return null;
    const e = this.entries().find((x) => x.id === id);
    return e && e.kind === 'show' ? e : null;
  });

  protected toggleShow(entry: ShowEntry) {
    this.expandedId.update((cur) => (cur === entry.id ? null : entry.id));
  }

  protected showLink(podcastId: string): unknown[] {
    return ['/podcasts', podcastId];
  }

  protected isNew(item: ItemHAL): boolean {
    if (!item.pubDate) return false;
    const ts = Date.parse(item.pubDate);
    return Number.isFinite(ts) && Date.now() - ts < ONE_DAY_MS;
  }

  protected coverUrl(item: ItemHAL): string {
    return item.cover.proxyURL;
  }

  protected onPlay(item: ItemHAL) {
    this.player.open(item);
  }

  protected onDownload(item: ItemHAL) {
    this.itemApi.triggerDownload(item.podcastId, item.id).subscribe();
  }

  protected onOpen(item: ItemHAL) {
    this.router.navigate(['/podcasts', item.podcastId, 'items', item.id]);
  }

  protected onOpenPodcast(podcastId: string) {
    this.router.navigate(['/podcasts', podcastId]);
  }

  protected isInProgress(item: ItemHAL): boolean {
    return item.status === 'STARTED' || item.status === 'PAUSED';
  }
}
