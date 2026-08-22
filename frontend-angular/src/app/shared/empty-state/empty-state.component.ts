import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'ps-empty-state',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="empty">
      <mat-icon class="empty__icon">{{ icon() }}</mat-icon>
      <h2 class="empty__title">{{ title() }}</h2>
      @if (subtitle(); as s) {
        <p class="empty__subtitle">{{ s }}</p>
      }
      <!-- Optional footer, for pages that can offer a way out of the empty
           state (e.g. clearing the active filters). Callers that project
           nothing render exactly as before. -->
      <div class="empty__actions"><ng-content /></div>
    </div>
  `,
  styles: `
    :host {
      display: block;
    }
    .empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      padding: 4rem 1rem;
      color: var(--mat-sys-on-surface-variant);
      text-align: center;
    }
    .empty__icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      opacity: 0.6;
    }
    .empty__title {
      font: var(--mat-sys-headline-small);
      margin: 0;
      color: var(--mat-sys-on-surface);
    }
    .empty__subtitle {
      font: var(--mat-sys-body-medium);
      margin: 0;
      max-width: 36ch;
    }
    /* Collapses to nothing when no content is projected, so the existing
       callers keep their current spacing. */
    .empty__actions:empty {
      display: none;
    }
    .empty__actions {
      margin-top: 0.5rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateComponent {
  readonly icon = input<string>('inbox');
  readonly title = input.required<string>();
  readonly subtitle = input<string | undefined>(undefined);
}
