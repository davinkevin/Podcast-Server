import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';

import { PlayerService } from '../../core/player/player.service';

/**
 * Inline button revealing the current player queue in a popover. Mounted
 * inside the floating-player controls; same component for audio and video
 * PiP. Highlights the currently-playing item, dims the already-played
 * ones, and lets the user jump straight to any entry in one click.
 *
 * The host element renders only the trigger button; the mat-menu attaches
 * a panel positioned ABOVE the player bar (`yPosition="above"`) so the
 * list grows upward instead of clipping into the page chrome below.
 */
@Component({
  selector: 'ps-player-queue-button',
  standalone: true,
  imports: [MatIconModule, MatButtonModule, MatMenuModule, MatTooltipModule],
  template: `
    <button
      mat-icon-button
      type="button"
      aria-label="Show queue"
      matTooltip="Queue"
      [matMenuTriggerFor]="queueMenu"
    >
      <mat-icon>queue_music</mat-icon>
    </button>
    <mat-menu
      #queueMenu="matMenu"
      yPosition="above"
      xPosition="before"
      [overlapTrigger]="false"
      class="player-queue-menu"
    >
      @for (item of player.queue(); track item.id; let i = $index) {
        @let active = i === player.currentIndex();
        @let past = i < player.currentIndex();
        <button
          mat-menu-item
          type="button"
          class="queue__row"
          [class.queue__row--active]="active"
          [class.queue__row--past]="past"
          [disabled]="!item.isDownloaded && !active"
          (click)="player.jumpTo(i)"
        >
          <div class="queue__leader">
            @if (active) {
              <mat-icon class="queue__leader-icon">play_arrow</mat-icon>
            } @else {
              <img class="queue__cover" [src]="item.cover.url" alt="" />
            }
          </div>
          <div class="queue__meta">
            <div class="queue__title" [title]="item.title">{{ item.title }}</div>
            <div class="queue__sub" [title]="item.podcast.title">
              {{ item.podcast.title }}
            </div>
          </div>
        </button>
      }
    </mat-menu>
  `,
  styles: `
    :host {
      display: contents;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlayerQueueButtonComponent {
  protected readonly player = inject(PlayerService);
}
