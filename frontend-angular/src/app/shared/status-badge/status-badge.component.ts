import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

export type StatusBadgeKind = 'downloading' | 'queued' | 'failed';

const ICONS: Record<StatusBadgeKind, string> = {
  downloading: 'download',
  queued: 'schedule',
  failed: 'error',
};

const ARIA: Record<StatusBadgeKind, string> = {
  downloading: 'Downloading',
  queued: 'In queue',
  failed: 'Download failed',
};

@Component({
  selector: 'ps-status-badge',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <span
      class="badge"
      [class]="'badge--' + kind()"
      [attr.aria-label]="ariaLabel()"
      role="status"
    >
      <mat-icon class="badge__icon">{{ icon() }}</mat-icon>
      @if (showProgression()) {
        <span class="badge__pct">{{ progression() }}%</span>
      }
    </span>
  `,
  styles: `
    :host { display: inline-flex; }
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 4px 8px;
      border-radius: 999px;
      font: var(--mat-sys-label-small);
      line-height: 1;
      backdrop-filter: blur(8px);
      box-shadow: var(--mat-sys-level1);
    }
    .badge__icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
    }
    .badge--downloading {
      background: color-mix(in srgb, var(--mat-sys-tertiary-container) 92%, transparent);
      color: var(--mat-sys-on-tertiary-container);
    }
    .badge--queued {
      background: color-mix(in srgb, var(--mat-sys-surface-container-highest) 92%, transparent);
      color: var(--mat-sys-on-surface-variant);
    }
    .badge--failed {
      background: color-mix(in srgb, var(--mat-sys-error-container) 92%, transparent);
      color: var(--mat-sys-on-error-container);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBadgeComponent {
  readonly kind = input.required<StatusBadgeKind>();
  readonly progression = input<number | null>(null);

  protected readonly icon = computed(() => ICONS[this.kind()]);
  protected readonly ariaLabel = computed(() => {
    const base = ARIA[this.kind()];
    const p = this.progression();
    return p !== null && this.kind() === 'downloading' ? `${base} ${p}%` : base;
  });
  protected readonly showProgression = computed(
    () => this.kind() === 'downloading' && this.progression() !== null,
  );
}
