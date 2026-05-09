import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';

interface NavLink {
  readonly label: string;
  readonly icon: string;
  readonly path: string;
}

const NAV_LINKS: readonly NavLink[] = [
  { label: 'Library', icon: 'library_music', path: '/library' },
  { label: 'Podcasts', icon: 'podcasts', path: '/podcasts' },
  { label: 'Downloads', icon: 'downloading', path: '/downloads' },
  { label: 'Playlists', icon: 'playlist_play', path: '/playlists' },
  { label: 'Settings', icon: 'settings', path: '/settings' },
];

@Component({
  selector: 'ps-sidenav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, MatListModule, MatIconModule],
  templateUrl: './sidenav.component.html',
  styleUrl: './sidenav.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidenavComponent {
  protected readonly links = NAV_LINKS;
}
