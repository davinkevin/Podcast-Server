import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

import { PlayerService } from '../../core/player/player.service';

@Component({
  selector: 'ps-floating-player',
  standalone: true,
  imports: [MatIconModule, MatButtonModule, MatTooltipModule],
  templateUrl: './floating-player.component.html',
  styleUrl: './floating-player.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FloatingPlayerComponent {
  protected readonly player = inject(PlayerService);
}
