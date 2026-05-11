import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'ps-pager',
  standalone: true,
  imports: [RouterLink, MatIconModule, MatButtonModule],
  templateUrl: './pager.component.html',
  styleUrl: './pager.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PagerComponent {
  readonly page = input.required<number>();
  readonly first = input<boolean>(true);
  readonly last = input<boolean>(true);

  protected readonly prevQuery = computed(() => ({ page: Math.max(0, this.page() - 1) }));
  protected readonly nextQuery = computed(() => ({ page: this.page() + 1 }));
}
