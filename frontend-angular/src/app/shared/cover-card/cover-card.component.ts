import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
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

/** Visual hairline rendered between two groups of menu entries. Carries
 *  no behaviour — purely a structural marker for the templates. */
export interface CoverCardDivider {
  readonly kind: 'divider';
}

/** Shared singleton for the structural divider — every component that
 *  builds an action list can splice this in without re-declaring a
 *  literal object each time. */
export const MENU_DIVIDER: CoverCardDivider = { kind: 'divider' };

/**
 * Concatenate menu sections into a single flat list, inserting
 * `MENU_DIVIDER` between consecutive non-empty sections. Empty sections
 * are skipped — they don't yield a divider on either side. The intent
 * is to keep callers declarative ("here are my three semantic groups")
 * without having to repeat the bookkeeping for dangling dividers.
 */
export function joinSections(
  ...sections: readonly (readonly CoverCardMenuEntry[])[]
): readonly CoverCardMenuEntry[] {
  const out: CoverCardMenuEntry[] = [];
  for (const section of sections) {
    if (section.length === 0) continue;
    if (out.length > 0) out.push(MENU_DIVIDER);
    out.push(...section);
  }
  return out;
}

export type CoverCardMenuEntry =
  | CoverCardAction
  | CoverCardActionGroup
  | CoverCardDivider;

@Component({
  selector: 'ps-cover-card',
  standalone: true,
  imports: [
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatMenuModule,
    MatRippleModule,
  ],
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
  /** Set when this card represents the floating player's current item.
   *  Replaces the play icon on the FAB with an animated "music playing"
   *  indicator so the user can spot the active card at a glance. */
  readonly playing = input<boolean>(false);
  /** When set, used as `view-transition-name` on the cover image so a
   *  matching element on the destination route (Apple-Music-style cover
   *  morph). Must be unique across all cards on screen at any time. */
  readonly viewTransitionName = input<string | undefined>(undefined);
  /** When set, a small artwork "chip" of the parent (e.g. the podcast a
   *  loose episode belongs to) is stamped on the bottom-right of the cover.
   *  Acts as a visual cue + a quick link — clicking it emits `openParent`
   *  instead of `open`. */
  readonly parentCoverUrl = input<string | undefined>(undefined);
  /** Accessible label target for the parent chip, e.g. the podcast title. */
  readonly parentTitle = input<string | undefined>(undefined);

  readonly play = output<void>();
  readonly download = output<void>();
  readonly action = output<CoverCardAction>();
  readonly open = output<void>();
  readonly openParent = output<void>();

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

  protected onOpenParent(event: Event) {
    event.stopPropagation();
    this.openParent.emit();
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
    return isGroup(entry) || isDivider(entry) ? null : entry;
  }

  protected isDivider(entry: CoverCardMenuEntry): boolean {
    return isDivider(entry);
  }
}

function isGroup(entry: CoverCardMenuEntry): entry is CoverCardActionGroup {
  return 'kind' in entry && entry.kind === 'group';
}

function isDivider(entry: CoverCardMenuEntry): entry is CoverCardDivider {
  return 'kind' in entry && entry.kind === 'divider';
}
