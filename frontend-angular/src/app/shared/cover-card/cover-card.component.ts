import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatRippleModule } from '@angular/material/core';

export interface CoverCardAction {
  /** Optional stable identifier for action matching by callers. */
  readonly id?: string;
  readonly label: string;
  readonly icon: string;
  /** When set, renders the menu entry as an external link instead of a
   *  button. The host page's `action` callback is not invoked — the browser
   *  handles the navigation directly (opens in a new tab). */
  readonly url?: string;
}

/** Groups several actions under a nested Material submenu — the parent row
 *  keeps the standard `[icon] [text]` layout (plus a chevron added by mat-menu
 *  for free); the submenu opens on hover/click with each action rendered as
 *  a normal menu item. Useful when several actions share an intent (e.g.
 *  "open this item somewhere") and stacking them at the top level would feel
 *  verbose. At most one group per menu — the templates use a fixed submenu
 *  ref under the assumption. */
export interface CoverCardActionGroup {
  readonly kind: 'group';
  readonly label: string;
  readonly icon: string;
  readonly items: readonly CoverCardAction[];
}

export type CoverCardMenuEntry = CoverCardAction | CoverCardActionGroup;

@Component({
  selector: 'ps-cover-card',
  standalone: true,
  imports: [MatIconModule, MatButtonModule, MatMenuModule, MatRippleModule],
  templateUrl: './cover-card.component.html',
  styleUrl: './cover-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'ps-cover-card',
  },
})
export class CoverCardComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string | undefined>(undefined);
  readonly coverUrl = input.required<string>();
  readonly actions = input<readonly CoverCardMenuEntry[]>([]);
  readonly playable = input<boolean>(true);
  readonly downloadable = input<boolean>(false);
  /** When set, used as `view-transition-name` on the cover image so a
   *  matching element on the destination route (Apple-Music-style cover
   *  morph). Must be unique across all cards on screen at any time. */
  readonly viewTransitionName = input<string | undefined>(undefined);

  readonly play = output<void>();
  readonly download = output<void>();
  readonly action = output<CoverCardAction>();
  readonly open = output<void>();

  protected onPlay(event: Event) {
    event.stopPropagation();
    this.play.emit();
  }

  protected onDownload(event: Event) {
    event.stopPropagation();
    this.download.emit();
  }

  protected onAction(action: CoverCardAction) {
    this.action.emit(action);
  }

  protected onOpen() {
    this.open.emit();
  }

  // Template-side discriminators. Angular's template type checker can narrow
  // discriminated unions via `@if`, but only when the discriminator field
  // exists on every variant — `CoverCardAction` doesn't carry `kind` at all,
  // so we route through these helpers (with explicit type predicates) to
  // get a clean `as` binding.
  protected asGroup(entry: CoverCardMenuEntry): CoverCardActionGroup | null {
    return isGroup(entry) ? entry : null;
  }

  protected asAction(entry: CoverCardMenuEntry): CoverCardAction | null {
    return isGroup(entry) ? null : entry;
  }
}

function isGroup(entry: CoverCardMenuEntry): entry is CoverCardActionGroup {
  return 'kind' in entry && entry.kind === 'group';
}
