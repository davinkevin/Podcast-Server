import { A11yModule } from '@angular/cdk/a11y';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';

import { TagInput } from '../../core/models/tag.model';
import { TagsFieldComponent } from '../tags-field/tags-field.component';
import { STATUS_FILTER_OPTIONS, StatusFilter } from './item-filters.model';

export interface ItemFilters {
  readonly tags: readonly TagInput[];
  readonly status: StatusFilter;
}

/**
 * The advanced filter panel, meant to be rendered inside a CDK overlay anchored
 * to a search field — see `library.component.html`.
 *
 * Content is laid out as repeatable groups under a heading, the scope group
 * being the first, so a platform-scoped group can join it without a redesign.
 * Which controls that group offers is up to the host: the library takes both,
 * a single podcast's page takes status only, because tags belong to the podcast
 * rather than the item.
 *
 * Filters apply as they are set: every control edit emits `changed` and the host
 * reflects it straight away. The component holds no state of its own — the
 * inputs are the current filters and the outputs are edits to them — so there is
 * no draft to commit, nothing to discard, and no way for what is shown to drift
 * from what is being filtered on.
 *
 * Every button is `type="button"`. The panel is rendered outside the host's
 * search form today, but that costs nothing and keeps it safe to host anywhere.
 */
@Component({
  selector: 'ps-item-filters-panel',
  standalone: true,
  imports: [A11yModule, MatButtonModule, MatButtonToggleModule, TagsFieldComponent],
  template: `
    <div
      class="filters"
      role="dialog"
      aria-label="More filters"
      cdkTrapFocus
      [cdkTrapFocusAutoCapture]="true"
    >
      <section class="filters__group">
        <h3 class="filters__group-title">{{ groupTitle() }}</h3>

        @if (withTags()) {
          <!-- placeholder is bound rather than a static attribute: a static one
               also stays on the host element, leaving a stray HTML placeholder on
               ps-tags-field itself. -->
          <ps-tags-field
            [tags]="tags()"
            (tagsChange)="onTagsChange($event)"
            [allowNew]="false"
            [placeholder]="'Search tags…'"
            hint="Episodes must match every tag listed here."
          />
        }

        <div class="filters__row">
          <span class="filters__label" id="ps-status-filter-label">Status</span>
          <mat-button-toggle-group
            class="filters__toggles"
            aria-labelledby="ps-status-filter-label"
            [value]="status()"
            (change)="onStatusChange($event.value)"
            hideSingleSelectionIndicator
          >
            @for (option of statusOptions; track option.value) {
              <mat-button-toggle [value]="option.value">{{ option.label }}</mat-button-toggle>
            }
          </mat-button-toggle-group>
        </div>
      </section>

      <!-- Only rendered when there is something to reset: with filters applying
           as they are set there is no Apply to pair it with, and a permanently
           dead button is worse than no button. -->
      @if (hasFilters()) {
        <div class="filters__actions">
          <button type="button" mat-button (click)="onReset()">Reset all filters</button>
        </div>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
      /* The CDK sizes the overlay pane to the trigger's width, but the pane is a
         flex container and this host would otherwise shrink to its content —
         leaving the panel narrower than the field it hangs from. */
      width: 100%;
    }

    .filters {
      /* Read as a continuation of the search field, not as a card floating under
         it: square top corners, no top border (the field's own bottom edge is
         the seam), and the field's radius — extra-small, i.e. 8px in this theme
         — on the bottom only. */
      border: 1px solid var(--mat-sys-outline);
      border-top: none;
      border-bottom-left-radius: var(--mat-sys-corner-extra-small);
      border-bottom-right-radius: var(--mat-sys-corner-extra-small);
      /* A downward-only shadow: a token elevation spreads on all four sides,
         which would darken the join with the field. No highlight along the top
         edge either — it drew exactly the line this panel is trying not to
         have. The field hides its own bottom border to match. */
      box-shadow: 0 6px 16px -4px rgb(0 0 0 / 40%);
      background: var(--mat-sys-surface-container);
      color: var(--mat-sys-on-surface);
      padding: 1.25rem 1.25rem 1rem;

      /* Density -2 on the fields inside. Material's density API is a Sass mixin
         and these styles are plain CSS, so set the two tokens it would emit —
         they are custom properties, so they inherit into the nested tags field
         without piercing its encapsulation. Worth doing now rather than once
         #271 adds a third group and the panel is already too tall. */
      --mat-form-field-container-height: 48px;
      --mat-form-field-container-vertical-padding: 12px;
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      max-height: min(70vh, 32rem);
      overflow-y: auto;

      /* Unrolls from under the field instead of blinking into place. The reveal
         is a clip-path rather than a scaleY so no text is squashed on the way in,
         and the easing is the one the app already uses for its element morphs.
         The component is rebuilt on every open, so mounting is enough to trigger
         it — there is no animation state to manage. */
      animation: filters-unroll 260ms cubic-bezier(0.32, 0.72, 0, 1) both;
    }

    /* Deliberately no opacity here: fading the surface lets the page behind
       bleed through mid-flight, which looks like a glitch. The clip reveal
       alone carries the unrolling, and the surface stays opaque throughout. */
    @keyframes filters-unroll {
      from {
        clip-path: inset(0 0 100% 0);
        transform: translateY(-6px);
      }
      to {
        clip-path: inset(0 0 0 0);
        transform: translateY(0);
      }
    }

    /* The content settles just behind the surface, which is what stops the
       opening from reading as a single stiff jump. */
    .filters__group,
    .filters__actions {
      animation: filters-settle 260ms cubic-bezier(0.32, 0.72, 0, 1) both;
    }

    @keyframes filters-settle {
      from {
        transform: translateY(-6px);
        opacity: 0;
      }
      to {
        transform: translateY(0);
        opacity: 1;
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .filters,
      .filters__group,
      .filters__actions {
        animation: none;
      }
    }

    .filters__group {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      animation-delay: 60ms;
    }

    /* Titled section rather than a bare caption: the trailing hairline gives the
       group a visible extent, which is what makes a second group — #271 adds a
       platform-scoped one — read as a sibling instead of more loose content. */
    .filters__group-title {
      font: var(--mat-sys-label-small);
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: var(--mat-sys-on-surface-variant);
      margin: 0;
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .filters__group-title::after {
      content: '';
      flex: 1;
      height: 1px;
      background: var(--mat-sys-outline-variant);
    }

    /* The status label sits with its control, both centred, so the pair reads as
       one unit. Left-aligning the label under a centred group left a stray
       caption hanging off to the side. */
    .filters__row {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
    }

    .filters__label {
      font: var(--mat-sys-body-small);
      color: var(--mat-sys-on-surface-variant);
    }

    /* Material insets a form field's hint by 16px, which put it off the axis
       every other line in the panel shares. Pull it back so the section reads
       as one column. */
    .filters ::ng-deep .mat-mdc-form-field-hint-wrapper {
      padding-left: 0;
      padding-right: 0;
    }

    /* Sized to its three labels and centred, rather than stretched to the panel
       width — stretching hands all the slack to the last option, which then
       reads as the important one. Wrapping keeps it usable if the labels ever
       outgrow a field-width panel. */
    .filters__toggles {
      align-self: center;
      flex-wrap: wrap;
      justify-content: center;
    }

    .filters__actions {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      gap: 0.5rem;
      padding-top: 0.75rem;
      border-top: 1px solid var(--mat-sys-outline-variant);
      animation-delay: 110ms;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ItemFiltersPanelComponent {
  /** The filters currently in force, i.e. the ones the URL carries. */
  readonly tags = input<readonly TagInput[]>([]);
  readonly status = input<StatusFilter>('all');

  /**
   * Whether to offer the tag filter at all.
   *
   * Tags belong to the *podcast*, not the item — `ItemRepository.search` matches
   * them through `PODCAST_TAGS` on `ITEM.PODCAST_ID` — so inside a single
   * podcast every item carries the same set. Filtering there would return
   * either everything or nothing, so a host scoped to one podcast turns it off.
   */
  readonly withTags = input(true);

  /**
   * What this group of filters applies to. The default says "every podcast"
   * because the library searches across all of them; a host scoped to one
   * podcast must say so, or the heading claims a reach the query does not have.
   */
  readonly groupTitle = input('Every podcast');

  readonly changed = output<ItemFilters>();

  protected readonly statusOptions = STATUS_FILTER_OPTIONS;
  protected readonly hasFilters = computed(
    () => (this.withTags() && this.tags().length > 0) || this.status() !== 'all',
  );

  protected onTagsChange(tags: readonly TagInput[]) {
    this.changed.emit({ tags, status: this.status() });
  }

  protected onStatusChange(status: StatusFilter) {
    this.changed.emit({ tags: this.tags(), status });
  }

  protected onReset() {
    // Leave the tags alone when they are not on offer here: this panel has no
    // business clearing a filter it never showed.
    this.changed.emit({ tags: this.withTags() ? [] : this.tags(), status: 'all' });
  }
}
