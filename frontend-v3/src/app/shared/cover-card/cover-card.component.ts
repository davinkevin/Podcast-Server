import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatRippleModule } from '@angular/material/core';

export interface CoverCardAction {
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

  readonly play = output<void>();
  readonly action = output<CoverCardAction>();
  readonly open = output<void>();

  protected onPlay(event: Event) {
    event.stopPropagation();
    this.play.emit();
  }

  protected onAction(event: Event, action: CoverCardAction) {
    event.stopPropagation();
    this.action.emit(action);
  }

  protected onOpen() {
    this.open.emit();
  }
}
