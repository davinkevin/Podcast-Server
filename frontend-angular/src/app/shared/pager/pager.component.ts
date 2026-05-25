import {
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  HostListener,
  inject,
  input,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'ps-pager',
  standalone: true,
  imports: [RouterLink, MatIconModule, MatButtonModule],
  templateUrl: './pager.component.html',
  styleUrl: './pager.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PagerComponent {
  readonly page = input.required<number>();
  readonly first = input<boolean>(true);
  readonly last = input<boolean>(true);
  // Optional: when supplied the pager renders « First and Last »
  // shortcuts. Skipped when callers can't compute it cheaply.
  readonly totalPages = input<number | undefined>(undefined);

  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly firstQuery = { page: 0 } as const;
  protected readonly prevQuery = computed(() => ({ page: Math.max(0, this.page() - 1) }));
  protected readonly nextQuery = computed(() => ({ page: this.page() + 1 }));
  protected readonly lastQuery = computed(() => {
    const total = this.totalPages();
    return total !== undefined ? { page: total - 1 } : undefined;
  });

  // ←/→ keyboard navigation. Bound at the document level so it fires
  // regardless of which element on the page has focus, with guards
  // mirroring what users expect from a "pager hotkey":
  //   - skip if the user is typing into an input, textarea, or any
  //     contentEditable element (a search field, a tag editor, …)
  //   - skip if a modifier is held (Cmd/Ctrl/Alt/Shift) so we don't
  //     hijack browser back/forward, history, or text-selection
  //   - only navigate if a corresponding prev/next page actually exists
  //     (`first/last` HAL flags)
  @HostListener('document:keydown', ['$event'])
  protected onDocumentKey(event: KeyboardEvent) {
    if (
      event.key !== 'ArrowLeft' &&
      event.key !== 'ArrowRight' &&
      event.key !== 'Home' &&
      event.key !== 'End'
    ) {
      return;
    }
    if (event.metaKey || event.ctrlKey || event.altKey || event.shiftKey) return;

    // ListRouteReuseStrategy detaches list pages (`library`/`podcasts`/
    // `playlists`) so their components stay alive in memory while the
    // user is on a different route. The HostListener stays bound the
    // whole time, so without this guard the retained library pager
    // would hijack ←/→ from podcast-detail and navigate the URL back
    // to `/library?page=N`. Checking `isConnected` filters out the
    // detached instance — its host element is no longer in the DOM.
    if (!this.host.nativeElement.isConnected) return;

    const t = event.target as HTMLElement | null;
    if (
      t &&
      (t.tagName === 'INPUT' ||
        t.tagName === 'TEXTAREA' ||
        t.tagName === 'SELECT' ||
        t.isContentEditable)
    ) {
      return;
    }

    if (event.key === 'ArrowLeft' && !this.first()) {
      event.preventDefault();
      this.navigateTo(this.prevQuery());
    } else if (event.key === 'ArrowRight' && !this.last()) {
      event.preventDefault();
      this.navigateTo(this.nextQuery());
    } else if (event.key === 'Home' && !this.first()) {
      event.preventDefault();
      this.navigateTo(this.firstQuery);
    } else if (event.key === 'End' && !this.last()) {
      // Skip when totalPages wasn't supplied — we can't know the index.
      const last = this.lastQuery();
      if (!last) return;
      event.preventDefault();
      this.navigateTo(last);
    }
  }

  // Horizontal swipe → page navigation (touch devices). Tracked at the
  // document level so the user can swipe anywhere on the page, not only
  // on the pager itself. Convention matches Instagram / Tinder: swipe
  // left = next page, swipe right = previous. Same `first()`/`last()`
  // guards as keyboard so we never wrap.
  //
  // Detection: a swipe is `|deltaX| > SWIPE_DISTANCE` AND `duration <
  // SWIPE_MAX_DURATION` AND `|deltaX| > |deltaY| * SWIPE_HV_RATIO` so a
  // mostly-vertical scroll never accidentally paginates.
  private touchStart?: { readonly x: number; readonly y: number; readonly t: number };

  @HostListener('document:touchstart', ['$event'])
  protected onTouchStart(event: TouchEvent) {
    // Reset on every event so multi-touch / pinch / cancelled drags
    // don't leak into the next single-finger swipe.
    this.touchStart = undefined;
    if (event.touches.length !== 1) return;
    if (!this.host.nativeElement.isConnected) return;
    if (this.isOverlayOpen()) return;
    if (this.isEditableTarget(event.target)) return;
    if (this.startedInHorizontalScroller(event.target as HTMLElement | null)) return;
    const t = event.touches[0];
    this.touchStart = { x: t.clientX, y: t.clientY, t: event.timeStamp };
  }

  @HostListener('document:touchcancel')
  protected onTouchCancel() {
    this.touchStart = undefined;
  }

  @HostListener('document:touchend', ['$event'])
  protected onTouchEnd(event: TouchEvent) {
    const start = this.touchStart;
    this.touchStart = undefined;
    if (!start) return;
    if (!this.host.nativeElement.isConnected) return;
    if (event.changedTouches.length === 0) return;
    const end = event.changedTouches[0];
    const dx = end.clientX - start.x;
    const dy = end.clientY - start.y;
    const duration = event.timeStamp - start.t;

    const SWIPE_DISTANCE = 80; // px — anything less reads as a tap or hesitant drag
    const SWIPE_MAX_DURATION = 500; // ms — beyond that the gesture isn't a swipe any more
    const SWIPE_HV_RATIO = 1.5; // horizontal must dominate vertical by this factor

    if (Math.abs(dx) < SWIPE_DISTANCE) return;
    if (duration > SWIPE_MAX_DURATION) return;
    if (Math.abs(dx) < Math.abs(dy) * SWIPE_HV_RATIO) return;

    if (dx < 0 && !this.last()) {
      this.navigateTo(this.nextQuery());
    } else if (dx > 0 && !this.first()) {
      this.navigateTo(this.prevQuery());
    }
  }

  private isEditableTarget(target: EventTarget | null): boolean {
    const el = target as HTMLElement | null;
    if (!el) return false;
    return (
      el.tagName === 'INPUT' ||
      el.tagName === 'TEXTAREA' ||
      el.tagName === 'SELECT' ||
      el.isContentEditable
    );
  }

  // Walks up from `target` looking for a horizontally-scrollable
  // ancestor (chip rows, embedded carousels). When one is found the
  // touch belongs to that element's scroll, not to pagination.
  private startedInHorizontalScroller(target: HTMLElement | null): boolean {
    let el = target;
    while (el && el !== document.body) {
      if (el.scrollWidth > el.clientWidth) {
        const overflowX = getComputedStyle(el).overflowX;
        if (overflowX === 'auto' || overflowX === 'scroll') return true;
      }
      el = el.parentElement;
    }
    return false;
  }

  // True when a CDK overlay is currently attached (mat-dialog,
  // mat-menu, mat-bottom-sheet…). Swipes inside those should belong
  // to the overlay's own logic — or simply not paginate the page
  // sitting behind.
  private isOverlayOpen(): boolean {
    return document.querySelector('.cdk-overlay-container .cdk-overlay-pane') !== null;
  }

  private navigateTo(query: { readonly page: number }) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: query,
      queryParamsHandling: 'merge',
    });
  }
}
