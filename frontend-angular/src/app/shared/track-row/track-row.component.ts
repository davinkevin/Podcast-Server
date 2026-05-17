import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatRippleModule } from '@angular/material/core';

import {
  CoverCardAction,
  CoverCardActionGroup,
  CoverCardMenuEntry,
} from '../cover-card/cover-card.component';

/**
 * Spotify / Apple Music style horizontal row for an item. Built as a
 * companion to `ps-cover-card` — same API surface (title, subtitle, cover,
 * playable, downloadable, actions, viewTransitionName) but rendered as a
 * scannable line: 56 px thumb, meta in the middle, actions on the right.
 *
 * Used on PodcastDetail and PlaylistDetail item lists where the long lists
 * of episodes are easier to scan as rows than 220 px cards.
 */
@Component({
  selector: 'ps-track-row',
  standalone: true,
  imports: [MatIconModule, MatButtonModule, MatMenuModule, MatRippleModule],
  templateUrl: './track-row.component.html',
  styleUrl: './track-row.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'ps-track-row',
  },
})
export class TrackRowComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string | undefined>(undefined);
  readonly coverUrl = input.required<string>();
  /** CSS `aspect-ratio` value, e.g. `'16 / 9'` or `'1 / 1'`. When set, the
   *  thumb keeps the row's fixed height but its width follows the natural
   *  ratio of the artwork (capped by SCSS so panoramic covers don't push
   *  the title column off the start of the row). When omitted, the thumb
   *  falls back to a square. */
  readonly aspectRatio = input<string | null>(null);
  readonly actions = input<readonly CoverCardMenuEntry[]>([]);
  readonly playable = input<boolean>(true);
  readonly downloadable = input<boolean>(false);
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

  // See CoverCardComponent for the rationale — discriminator helpers that
  // give the template a clean `as` binding for the union variants.
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
