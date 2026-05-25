import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

import { PlayerService } from '../../core/player/player.service';
import { VlcService } from '../../core/vlc/vlc.service';
import { PlayerQueueButtonComponent } from '../../shared/player-queue-button/player-queue-button.component';

@Component({
  selector: 'ps-floating-player',
  standalone: true,
  imports: [
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    PlayerQueueButtonComponent,
  ],
  templateUrl: './floating-player.component.html',
  styleUrl: './floating-player.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FloatingPlayerComponent {
  protected readonly player = inject(PlayerService);
  private readonly vlc = inject(VlcService);

  protected onOpenInVlc(proxyUrl: string) {
    this.vlc.openInVlc(proxyUrl);
  }
}
