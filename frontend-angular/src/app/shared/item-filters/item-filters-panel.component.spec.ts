import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { beforeEach, describe, expect, it } from 'vitest';

import { TagApi } from '../../core/api/tag.api';
import { TagsFieldComponent } from '../tags-field/tags-field.component';
import { ItemFilters, ItemFiltersPanelComponent } from './item-filters-panel.component';
import { StatusFilter } from './item-filters.model';

// This panel is a leaf, so unlike the rest of the app's specs it is worth
// rendering: what needs proving here — that the status control offers three
// choices and not the raw enum, and that every edit is reported immediately —
// lives in the template.

async function render(
  inputs: {
    tags?: readonly { name: string }[];
    status?: StatusFilter;
    withTags?: boolean;
    groupTitle?: string;
  } = {},
): Promise<{
  fixture: ComponentFixture<ItemFiltersPanelComponent>;
  changed: ItemFilters[];
}> {
  const fixture = TestBed.createComponent(ItemFiltersPanelComponent);
  if (inputs.tags) fixture.componentRef.setInput('tags', inputs.tags);
  if (inputs.status) fixture.componentRef.setInput('status', inputs.status);
  if (inputs.withTags !== undefined) fixture.componentRef.setInput('withTags', inputs.withTags);
  if (inputs.groupTitle) fixture.componentRef.setInput('groupTitle', inputs.groupTitle);

  const changed: ItemFilters[] = [];
  fixture.componentInstance.changed.subscribe((f) => changed.push(f));

  await fixture.whenStable();
  return { fixture, changed };
}

function root(fixture: ComponentFixture<unknown>): HTMLElement {
  return fixture.nativeElement as HTMLElement;
}

function buttonLabelled(fixture: ComponentFixture<unknown>, label: string): HTMLButtonElement {
  const el = Array.from(
    root(fixture).querySelectorAll<HTMLButtonElement>('.filters__actions button'),
  ).find((b) => b.textContent?.trim() === label);
  if (!el) throw new Error(`No "${label}" button in the panel actions`);
  return el;
}

function statusToggles(fixture: ComponentFixture<unknown>): HTMLElement[] {
  return Array.from(root(fixture).querySelectorAll<HTMLElement>('mat-button-toggle'));
}

describe('ItemFiltersPanelComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        // The nested tags field queries suggestions; it must never hit the network.
        { provide: TagApi, useValue: { search: () => ({ data: signal(undefined) }) } },
      ],
    });
  });

  it('offers exactly the three status choices, never the raw enum', async () => {
    const { fixture } = await render();

    // Regression guard: someone re-adding the six/seven backend statuses as
    // checkboxes fails here.
    expect(statusToggles(fixture).map((t) => t.textContent?.trim())).toEqual([
      'All',
      'Downloaded',
      'Not downloaded',
    ]);
    expect(root(fixture).querySelectorAll('mat-checkbox')).toHaveLength(0);
  });

  it('labels its status group for assistive technology', async () => {
    const { fixture } = await render({ status: 'not-downloaded' });

    const group = root(fixture).querySelector('mat-button-toggle-group')!;
    expect(group.getAttribute('aria-labelledby')).toBe('ps-status-filter-label');
  });

  it('reports a status edit straight away, carrying the tags along', async () => {
    const { fixture, changed } = await render({ tags: [{ name: 'tech' }] });

    fixture.componentInstance['onStatusChange']('downloaded');

    expect(changed).toEqual([{ tags: [{ name: 'tech' }], status: 'downloaded' }]);
  });

  it('reports a tag edit straight away, carrying the status along', async () => {
    const { fixture, changed } = await render({ status: 'not-downloaded' });

    fixture.componentInstance['onTagsChange']([{ name: 'dev' }]);

    expect(changed).toEqual([{ tags: [{ name: 'dev' }], status: 'not-downloaded' }]);
  });

  it('follows its inputs, holding no state that could drift from them', async () => {
    // There is no draft to go stale: what the panel shows is what it was given,
    // so a filter changed elsewhere — a summary chip removed, say — is reflected
    // here without the two ever disagreeing.
    const { fixture } = await render({ status: 'downloaded' });
    const checked = () =>
      statusToggles(fixture)
        .filter((t) => t.querySelector('button')?.getAttribute('aria-checked') === 'true')
        .map((t) => t.textContent?.trim());

    expect(checked()).toEqual(['Downloaded']);

    fixture.componentRef.setInput('status', 'not-downloaded');
    await fixture.whenStable();

    expect(checked()).toEqual(['Not downloaded']);
  });

  it('resets every filter in one go', async () => {
    const { fixture, changed } = await render({
      tags: [{ name: 'tech' }],
      status: 'downloaded',
    });

    buttonLabelled(fixture, 'Reset all filters').click();

    expect(changed).toEqual([{ tags: [], status: 'all' }]);
  });

  it('offers no reset when there is nothing to reset', async () => {
    const { fixture } = await render();

    expect(root(fixture).querySelector('.filters__actions')).toBeNull();
  });

  it('never submits a surrounding form', async () => {
    // Cheap insurance: the panel is rendered outside the host's search form
    // today, but a submit button would fire it if that ever changed back.
    const { fixture } = await render({ tags: [{ name: 'tech' }] });
    const buttons = root(fixture).querySelectorAll<HTMLButtonElement>('button');

    expect(buttons.length).toBeGreaterThan(0);
    for (const button of buttons) {
      expect(button.getAttribute('type')).toBe('button');
    }
  });

  describe('scoped to a single podcast', () => {
    // Tags belong to the podcast, not the item, so inside one podcast the
    // control would match everything or nothing.
    it('drops the tag filter and names the scope', async () => {
      const { fixture } = await render({ withTags: false, groupTitle: 'This podcast' });

      expect(root(fixture).querySelector('ps-tags-field')).toBeNull();
      expect(root(fixture).querySelector('.filters__group-title')?.textContent?.trim()).toBe(
        'This podcast',
      );
      // Status is still the whole point of the panel there.
      expect(statusToggles(fixture)).toHaveLength(3);
    });

    it('does not count or clear tags it never offered', async () => {
      const { fixture, changed } = await render({
        withTags: false,
        tags: [{ name: 'tech' }],
        status: 'downloaded',
      });

      buttonLabelled(fixture, 'Reset all filters').click();

      // The tag survives: this panel has no business clearing a filter it did
      // not show.
      expect(changed).toEqual([{ tags: [{ name: 'tech' }], status: 'all' }]);
    });

    it('offers no reset for a tag it never offered', async () => {
      const { fixture } = await render({ withTags: false, tags: [{ name: 'tech' }] });

      expect(root(fixture).querySelector('.filters__actions')).toBeNull();
    });
  });

  it('offers the tag field as a lookup, not a creator', async () => {
    // A tag name nothing carries can only ever return an empty result set, so
    // as a filter the field must not commit free text — unlike the podcast edit
    // dialog, where creating a tag is the whole point. The behaviour itself is
    // covered in the tags field's own spec; this pins the wiring.
    const { fixture } = await render();
    const field = fixture.debugElement.query(By.directive(TagsFieldComponent))
      .componentInstance as TagsFieldComponent;

    expect(field.allowNew()).toBe(false);
    expect(field.placeholder()).toBe('Search tags…');
  });

  it('traps focus while open', async () => {
    const { fixture } = await render();
    const panel = root(fixture).querySelector('.filters')!;

    expect(panel.getAttribute('role')).toBe('dialog');
    // cdkTrapFocus injects its anchors around the trapped region.
    expect(root(fixture).querySelectorAll('.cdk-focus-trap-anchor').length).toBeGreaterThan(0);
  });
});
