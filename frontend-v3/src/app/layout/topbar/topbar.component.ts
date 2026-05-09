import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'ps-topbar',
  standalone: true,
  imports: [
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatBadgeModule,
    MatMenuModule,
    MatTooltipModule,
  ],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopbarComponent {
  // Wired in PR 4 once DownloadStreamService lands.
  protected readonly downloadCount = 0;
}
