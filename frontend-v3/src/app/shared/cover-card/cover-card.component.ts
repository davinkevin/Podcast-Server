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
}

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
  readonly actions = input<readonly CoverCardAction[]>([]);
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
}
