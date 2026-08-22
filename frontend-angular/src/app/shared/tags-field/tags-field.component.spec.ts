import { WritableSignal, provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { TagApi } from '../../core/api/tag.api';
import { TagsContainerHAL } from '../../core/models/tag.model';
import { TagInput } from '../../core/models/tag.model';
import { TagsFieldComponent } from './tags-field.component';

// The field serves two opposite jobs. Attaching tags to a podcast, where typing
// a name nothing carries yet is the point. And filtering, where the same
// gesture can only ever ask for something no episode has — so a typo, or merely
// the wrong case, silently empties the page. `allowNew` is what separates them.

describe('TagsFieldComponent', () => {
  let suggestions: WritableSignal<TagsContainerHAL | undefined>;

  async function render(allowNew: boolean, tags: readonly TagInput[] = []) {
    const fixture = TestBed.createComponent(TagsFieldComponent);
    fixture.componentRef.setInput('allowNew', allowNew);
    fixture.componentRef.setInput('tags', tags);
    const emitted: (readonly TagInput[])[] = [];
    fixture.componentInstance.tags.subscribe((t) => emitted.push(t));
    await fixture.whenStable();
    return { fixture, emitted };
  }

  /** What the chip input hands over when a separator key or a blur commits. */
  function tokenEnd(fixture: ComponentFixture<TagsFieldComponent>, value: string) {
    const input = (fixture.nativeElement as HTMLElement).querySelector('input')!;
    input.value = value;
    fixture.componentInstance['query'].set(value);
    fixture.componentInstance['onChipInputEnd']({
      value,
      chipInput: fixture.componentInstance['chipInput'](),
      input,
    });
  }

  beforeEach(() => {
    suggestions = signal<TagsContainerHAL | undefined>(undefined);
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        { provide: TagApi, useValue: { search: () => ({ data: suggestions }) } },
      ],
    });
  });

  describe('when new tags are allowed', () => {
    it('commits what was typed', async () => {
      const { fixture, emitted } = await render(true);

      tokenEnd(fixture, 'brand new');

      expect(emitted.at(-1)).toEqual([{ name: 'brand new' }]);
    });

    it('leaves Tab alone, since it already commits what was typed', async () => {
      // Completing here would silently swap a name being created for an
      // existing one that merely shares its prefix.
      const { fixture, emitted } = await render(true);
      suggestions.set({ content: [{ id: 't1', name: 'development' }] });
      fixture.componentInstance['query'].set('dev');
      await fixture.whenStable();

      const event = {
        shiftKey: false,
        prevented: false,
        preventDefault() {
          this.prevented = true;
        },
      };
      fixture.componentInstance['onTabComplete'](event as unknown as Event);

      expect(emitted).toEqual([]);
      expect(event.prevented).toBe(false);
    });

    it('never says a name matched nothing, since any name is valid', async () => {
      const { fixture } = await render(true);
      suggestions.set({ content: [] });
      fixture.componentInstance['query'].set('nope');
      await fixture.whenStable();

      expect(fixture.componentInstance['noMatch']()).toBe(false);
    });
  });

  describe('when it is a lookup', () => {
    it('commits nothing that is not an existing tag', async () => {
      const { fixture, emitted } = await render(false);

      tokenEnd(fixture, 'Dev');

      // Right word, wrong case: as a filter this would have emptied the page.
      expect(emitted).toEqual([]);
    });

    it('clears the typed text rather than leave it beside the chips', async () => {
      const { fixture } = await render(false);

      tokenEnd(fixture, 'Dev');

      // Text left in the field would read as a filter that is not in force.
      expect(fixture.componentInstance['query']()).toBe('');
    });

    it('says so when a name matched nothing', async () => {
      const { fixture } = await render(false);
      suggestions.set({ content: [] });
      fixture.componentInstance['query'].set('nosuchtag');
      await fixture.whenStable();

      expect(fixture.componentInstance['noMatch']()).toBe(true);
    });

    it('stays quiet before the search has answered', async () => {
      const { fixture } = await render(false);
      fixture.componentInstance['query'].set('de');
      await fixture.whenStable();

      // No result yet is not the same as no match — saying so would flicker.
      expect(fixture.componentInstance['noMatch']()).toBe(false);
    });

    it('takes the first match on Tab, so it costs one key not two', async () => {
      const { fixture, emitted } = await render(false);
      suggestions.set({ content: [{ id: 't1', name: 'dev' }] });
      fixture.componentInstance['query'].set('de');
      await fixture.whenStable();

      const event = {
        shiftKey: false,
        prevented: false,
        preventDefault() {
          this.prevented = true;
        },
      };
      fixture.componentInstance['onTabComplete'](event as unknown as Event);

      expect(emitted.at(-1)).toEqual([{ id: 't1', name: 'dev' }]);
      // Tab is consumed, so focus stays put and more tags can be added.
      expect(event.prevented).toBe(true);
    });

    it('leaves Tab alone when there is nothing to take', async () => {
      const { fixture, emitted } = await render(false);
      suggestions.set({ content: [] });
      fixture.componentInstance['query'].set('nosuchtag');
      await fixture.whenStable();

      const event = {
        shiftKey: false,
        prevented: false,
        preventDefault() {
          this.prevented = true;
        },
      };
      fixture.componentInstance['onTabComplete'](event as unknown as Event);

      expect(emitted).toEqual([]);
      // Not consumed: focus must never be trapped in the field.
      expect(event.prevented).toBe(false);
    });

    it('leaves Shift+Tab alone, which means go back', async () => {
      const { fixture, emitted } = await render(false);
      suggestions.set({ content: [{ id: 't1', name: 'dev' }] });
      fixture.componentInstance['query'].set('de');
      await fixture.whenStable();

      const event = {
        shiftKey: true,
        prevented: false,
        preventDefault() {
          this.prevented = true;
        },
      };
      fixture.componentInstance['onTabComplete'](event as unknown as Event);

      expect(emitted).toEqual([]);
      expect(event.prevented).toBe(false);
    });

    it('still takes a suggestion, which is the only way in', async () => {
      const { fixture, emitted } = await render(false);

      fixture.componentInstance['onSelect']({
        option: { value: { id: 't1', name: 'dev' } },
      } as never);

      expect(emitted.at(-1)).toEqual([{ id: 't1', name: 'dev' }]);
    });
  });
});
