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

  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly prevQuery = computed(() => ({ page: Math.max(0, this.page() - 1) }));
  protected readonly nextQuery = computed(() => ({ page: this.page() + 1 }));

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
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
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
    }
  }

  private navigateTo(query: { readonly page: number }) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: query,
      queryParamsHandling: 'merge',
    });
  }
}
