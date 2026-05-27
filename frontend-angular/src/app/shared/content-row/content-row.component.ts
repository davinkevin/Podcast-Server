import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

/**
 * Horizontal, scroll-snapping carousel of projected cards — the building
 * block for the landing's "one row per podcast" layout (Spotify / Apple
 * Music style). Purely presentational: callers project `ps-cover-card`s (or
 * anything) into the default slot and own the data.
 *
 * The track is a flex row with horizontal overflow; chevron buttons page it
 * left/right by ~90% of the visible width and disable themselves at each end.
 */
@Component({
  selector: 'ps-content-row',
  standalone: true,
  imports: [RouterLink, MatIconModule, MatButtonModule],
  templateUrl: './content-row.component.html',
  styleUrl: './content-row.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentRowComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string | undefined>(undefined);
  /** Optional small artwork shown to the left of the title (e.g. the
   *  podcast cover) to anchor the row visually. */
  readonly artworkUrl = input<string | undefined>(undefined);
  /** Optional pill rendered next to the title, e.g. "12 recent". */
  readonly badge = input<string | undefined>(undefined);
  /** Optional "see all" target — a router commands array. */
  readonly seeAllLink = input<unknown[] | undefined>(undefined);

  private readonly track = viewChild.required<ElementRef<HTMLElement>>('track');

  protected readonly canScrollLeft = signal(false);
  protected readonly canScrollRight = signal(false);

  constructor() {
    // Initial paging affordances once the track has laid out (the right
    // chevron only shows when content actually overflows).
    afterNextRender(() => this.refreshAffordances());
  }

  protected onScroll() {
    this.refreshAffordances();
  }

  protected page(direction: -1 | 1) {
    const el = this.track().nativeElement;
    el.scrollBy({ left: direction * el.clientWidth * 0.9, behavior: 'smooth' });
  }

  private refreshAffordances() {
    const el = this.track().nativeElement;
    const max = el.scrollWidth - el.clientWidth;
    this.canScrollLeft.set(el.scrollLeft > 1);
    this.canScrollRight.set(el.scrollLeft < max - 1);
  }
}
