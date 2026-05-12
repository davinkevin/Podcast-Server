import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Compact bar that sits sticky at the top of a detail page (podcast,
 * playlist…) and fades in once the hero has scrolled out. The parent owns
 * the visibility signal — set via the `shown` input — typically driven by
 * an IntersectionObserver on a sentinel placed below the hero.
 */
@Component({
  selector: 'ps-detail-sticky-header',
  standalone: true,
  template: `
    <div class="dsh__inner">
      <img class="dsh__cover" [src]="coverUrl()" alt="" />
      <span class="dsh__title" [title]="title()">{{ title() }}</span>
    </div>
  `,
  styles: `
    :host {
      /* Fixed (not sticky) so the bar never participates in the page flow:
         no layout shift when hidden, and spans edge-to-edge from the sidenav
         to the right viewport edge regardless of the page's max-width. */
      position: fixed;
      top: 0;
      left: var(--sidenav-width, 0);
      right: 0;
      z-index: 5;
      /* 3 equal columns: first third holds the cover (right-aligned so its
         right edge sits exactly on the 1/3 mark), middle third holds the
         centered title, last third stays empty to keep the bar symmetric. */
      display: grid;
      grid-template-columns: 1fr 1fr 1fr;
      align-items: center;
      padding: 0.5rem clamp(1rem, 4vw, 2rem);
      /* Frosted glass: translucent surface + backdrop blur. The bar is an
         overlay, not a separate panel — content beneath shows through softly,
         removing the hard boundary that a solid background draws. */
      background: color-mix(in srgb, var(--mat-sys-surface) 65%, transparent);
      backdrop-filter: blur(20px) saturate(160%);
      -webkit-backdrop-filter: blur(20px) saturate(160%);
      opacity: 0;
      pointer-events: none;
      transition:
        opacity 200ms cubic-bezier(0.4, 0, 0.2, 1),
        left 200ms cubic-bezier(0.4, 0, 0.2, 1);
    }
    :host(.shown) {
      opacity: 1;
      pointer-events: auto;
    }
    /* The middle third is itself a flex row so the cover sits on the LEFT
       of that third while the title stays centered on the bar's true middle
       line — an invisible mirror of the cover on the right keeps the layout
       symmetric. */
    .dsh__inner {
      grid-column: 2;
      display: flex;
      align-items: center;
      gap: 0.75rem;
      min-width: 0;
    }
    .dsh__inner::after {
      content: '';
      width: 56px;
      height: 56px;
      flex: 0 0 auto;
      visibility: hidden;
    }
    .dsh__cover {
      width: 56px;
      height: 56px;
      border-radius: 8px;
      object-fit: cover;
      flex: 0 0 auto;
      box-shadow: var(--mat-sys-level1);
    }
    .dsh__title {
      flex: 1;
      font: var(--mat-sys-title-medium);
      color: var(--hero-title-color, var(--mat-sys-on-surface));
      text-align: center;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      min-width: 0;
    }
  `,
  host: { '[class.shown]': 'shown()' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetailStickyHeaderComponent {
  readonly coverUrl = input.required<string>();
  readonly title = input<string>('');
  readonly shown = input<boolean>(false);
}
