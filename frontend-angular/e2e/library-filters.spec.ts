import { Page, expect, test } from '@playwright/test';

// #273: the filter panel's promises that only a real browser can prove —
// the overlay stays out of the layout flow, focus is trapped and comes back to
// the chevron, and the URL is the state.

const SEARCH_ROUTE = '**/api/v1/items/search*';

function item(index: number) {
  return {
    id: `item-${index}`,
    title: `Episode ${index}`,
    url: `https://example.com/ep${index}.mp3`,
    pubDate: '2024-01-01T00:00:00Z',
    downloadDate: null,
    creationDate: '2024-01-01T00:00:00Z',
    description: '',
    mimeType: 'audio/mpeg',
    length: null,
    fileName: null,
    status: 'NOT_DOWNLOADED',
    podcast: { id: 'pod-1', title: 'A podcast', url: 'https://example.com' },
    cover: {
      id: 'cover-1',
      width: 200,
      height: 200,
      url: 'https://example.com/cover.png',
      proxyURL: 'https://example.com/cover.png',
    },
    isDownloaded: false,
    podcastId: 'pod-1',
    proxyURL: `https://example.com/proxy/ep${index}.mp3`,
  };
}

/**
 * Serves a fixed page of episodes and records the query string of every search,
 * so a test can assert what the app actually asked the API for.
 */
async function stubSearch(page: Page): Promise<{ queries: string[] }> {
  const queries: string[] = [];

  await page.route(SEARCH_ROUTE, async (route) => {
    queries.push(new URL(route.request().url()).search);
    const content = [1, 2, 3, 4].map(item);
    await route.fulfill({
      json: {
        content,
        empty: false,
        first: true,
        last: true,
        number: 0,
        numberOfElements: content.length,
        size: 24,
        totalElements: content.length,
        totalPages: 1,
      },
    });
  });

  await page.route('**/api/v1/tags/search*', (route) => route.fulfill({ json: { content: [] } }));

  return { queries };
}

/**
 * Serves tag suggestions, matching on the name the way the real endpoint does —
 * so a query nothing carries comes back empty, which is the case that matters.
 */
async function stubTagSuggestions(page: Page, names: string[]) {
  await page.route('**/api/v1/tags/search*', (route) => {
    const q = (new URL(route.request().url()).searchParams.get('name') ?? '').toLowerCase();
    const matches = names.filter((n) => n.toLowerCase().includes(q));
    return route.fulfill({
      json: { content: matches.map((name, i) => ({ id: `tag-${i}`, name })) },
    });
  });
}

function chevron(page: Page) {
  return page.getByRole('button', { name: 'More filters' });
}

function panel(page: Page) {
  return page.getByRole('dialog', { name: 'More filters' });
}

/**
 * `mat-button-toggle-group` renders proper radiogroup semantics — a radiogroup
 * of radios with aria-checked — which is what a single choice between three is.
 */
function statusOption(page: Page, label: string) {
  return panel(page).getByRole('radio', { name: label, exact: true });
}

/**
 * The dev server compiles the lazy library chunk on first hit, which can take
 * far longer than the default expect timeout.
 */
const FIRST_PAINT = { timeout: 30_000 };

/**
 * The panel unrolls on open, so its box is still moving for a few frames.
 * Geometry assertions have to wait for the Web Animations to settle rather than
 * guess a delay.
 */
async function settled(page: Page) {
  await page
    .locator('.filters')
    .evaluate((el) =>
      Promise.all(el.getAnimations({ subtree: true }).map((a) => a.finished)).then(() => undefined),
    );
}

test('opening the panel does not move the episode grid', async ({ page }) => {
  await stubSearch(page);
  await page.goto('/library');

  const grid = page.locator('.library__grid');
  await expect(grid).toBeVisible(FIRST_PAINT);
  const before = await grid.boundingBox();

  await chevron(page).click();
  await expect(panel(page)).toBeVisible();

  // The overlay renders in the top layer / overlay container, so it takes no
  // room in the flow: the grid must not have budged by a single pixel.
  expect(await grid.boundingBox()).toEqual(before);

  // And the page behind stays live — no modal, no backdrop intercepting clicks.
  await expect(page.locator('.cdk-overlay-backdrop')).toHaveCount(0);
});

test('the panel reads as an extension of the search field', async ({ page }) => {
  await stubSearch(page);
  await page.goto('/library');
  await expect(page.locator('.library__grid')).toBeVisible(FIRST_PAINT);

  await chevron(page).click();
  await expect(panel(page)).toBeVisible();
  await settled(page);

  const field = await page.locator('.library__search > mat-form-field').boundingBox();
  const box = await page.locator('.filters').boundingBox();

  // Same width and same left edge as the field, and hanging straight off its
  // bottom edge: the panel is an extension of the field, not a card near it.
  expect(box!.width).toBeCloseTo(field!.width, 0);
  expect(box!.x).toBeCloseTo(field!.x, 0);
  expect(box!.y).toBeCloseTo(field!.y + field!.height, 0);

  // Only the bottom corners are rounded, and the field's own bottom corners go
  // flat to meet them — that is what sells the illusion of one shape.
  const shape = await page.locator('.filters').evaluate((el) => {
    const s = getComputedStyle(el);
    return {
      topLeft: s.borderTopLeftRadius,
      topRight: s.borderTopRightRadius,
      bottomLeft: s.borderBottomLeftRadius,
      borderTop: s.borderTopWidth,
    };
  });
  expect(shape.topLeft).toBe('0px');
  expect(shape.topRight).toBe('0px');
  expect(shape.borderTop).toBe('0px');
  expect(shape.bottomLeft).not.toBe('0px');

  const outline = page
    .locator('.library__search > mat-form-field .mdc-notched-outline__leading')
    .first();
  await expect(outline).toHaveCSS('border-bottom-left-radius', '0px');

  // Focusing the field must not change its outline: Material would draw it in
  // `primary` at 2px, leaving one half of a single shape a different colour and
  // weight from the other.
  await page.getByPlaceholder('Filter episodes…').click();
  const border = await page.evaluate(() => {
    const side = (el: Element, s: 'Left' | 'Top') =>
      `${getComputedStyle(el)[`border${s}Color`]} ${getComputedStyle(el)[`border${s}Width`]}`;
    return {
      field: side(
        document.querySelector('.library__search > mat-form-field .mdc-notched-outline__leading')!,
        'Top',
      ),
      panel: side(document.querySelector('.filters')!, 'Left'),
    };
  });
  expect(border.field).toBe(border.panel);
  // No line across the join, and no step in tone either: the field takes the
  // panel's fill so the two read as one module rather than one bolted onto the
  // other.
  await expect(outline).toHaveCSS('border-bottom-color', 'rgba(0, 0, 0, 0)');

  // Read both fills in one go: comparing them across two locators is what
  // matters, and the theme's own resolved colour is irrelevant.
  const fills = await page.evaluate(() => {
    const bg = (sel: string) =>
      getComputedStyle(document.querySelector(sel) as Element).backgroundColor;
    return {
      field: bg('.library__search > mat-form-field .mat-mdc-text-field-wrapper'),
      panel: bg('.filters'),
    };
  });
  expect(fills.field).toBe(fills.panel);
});

test('the panel stays stuck to the field when the page reflows under it', async ({ page }) => {
  await stubSearch(page);
  await page.goto('/library');
  await expect(page.locator('.library__grid')).toBeVisible(FIRST_PAINT);

  await chevron(page).click();
  await expect(panel(page)).toBeVisible();
  await settled(page);

  const offset = () =>
    page.evaluate(() => {
      const f = document
        .querySelector('.library__search > mat-form-field')!
        .getBoundingClientRect();
      const p = document.querySelector('.filters')!.getBoundingClientRect();
      return Math.round(p.x - f.x);
    });

  expect(await offset()).toBe(0);

  // The scrollbar appearing or disappearing moves the centred column sideways
  // without firing a resize, and the CDK places an anchored overlay once. Filters
  // applying live cross the scrolling threshold constantly, so the panel came
  // visibly unstuck from its field. `scrollbar-gutter` removes that cause —
  // asserted below — and a reposition on every filter change covers the rest.
  await page.evaluate(() => {
    (document.querySelector('.library') as HTMLElement).style.paddingLeft = '60px';
  });
  expect(await offset()).not.toBe(0);

  await statusOption(page, 'Downloaded').click();
  await expect(page).toHaveURL(/status=downloaded/);
  expect(await offset()).toBe(0);

  // On the element that actually scrolls: `html` never does, so a gutter there
  // reserves space for a scrollbar that never appears and fixes nothing.
  await expect(page.locator('.shell__outlet')).toHaveCSS('scrollbar-gutter', 'stable');
});

test('a filter change skips the view transition, so nothing flies over the panel', async ({
  page,
}) => {
  await stubSearch(page);

  // Angular always calls startViewTransition and then skips the ones it does
  // not want, so what matters is that this one was skipped. Left running, the
  // covers are lifted into the transition's own pseudo-element tree — which
  // paints above the top layer — and fly over the open panel on their way back.
  await page.addInitScript(() => {
    const w = window as unknown as { __started: number; __skipped: number };
    w.__started = 0;
    w.__skipped = 0;
    const real = document.startViewTransition?.bind(document);
    if (real) {
      document.startViewTransition = ((cb: never) => {
        w.__started++;
        const t = real(cb);
        const skip = t.skipTransition.bind(t);
        t.skipTransition = () => {
          w.__skipped++;
          skip();
        };
        return t;
      }) as typeof document.startViewTransition;
    }
  });

  await page.goto('/library');
  await expect(page.locator('.library__grid')).toBeVisible(FIRST_PAINT);

  await chevron(page).click();
  await expect(panel(page)).toBeVisible();

  await statusOption(page, 'Downloaded').click();
  await expect(page).toHaveURL(/status=downloaded/);

  await expect
    .poll(() => page.evaluate(() => (window as unknown as { __skipped: number }).__skipped))
    .toBeGreaterThan(0);
  const counts = await page.evaluate(() => ({
    started: (window as unknown as { __started: number }).__started,
    skipped: (window as unknown as { __skipped: number }).__skipped,
  }));
  // Every transition this page started was skipped: none was left to animate.
  expect(counts.skipped).toBe(counts.started);
});

test('setting a filter does not move the grid either', async ({ page }) => {
  await stubSearch(page);

  const gridY = () =>
    page.evaluate(() =>
      Math.round(document.querySelector('.library__grid')!.getBoundingClientRect().y),
    );

  await page.goto('/library');
  await expect(page.locator('.library__grid')).toBeVisible(FIRST_PAINT);
  const before = await gridY();

  // The summary row's space is reserved whether or not it holds a chip, so the
  // header's height is constant. Without that the page jumped 48px — behind the
  // open panel, on every toggle, which is exactly when it is least wanted.
  await chevron(page).click();
  await statusOption(page, 'Downloaded').click();
  await expect(page.locator('.library__active-filters')).toContainText('Downloaded');
  // The grid is unmounted while the filtered page loads; wait for it back
  // before measuring, or there is nothing to measure.
  await expect(page.locator('.library__grid')).toBeVisible();

  expect(await gridY()).toBe(before);
});

test('the whole journey is reachable from the keyboard alone', async ({ page }) => {
  await stubSearch(page);
  await page.goto('/library');
  await expect(page.locator('.library__grid')).toBeVisible(FIRST_PAINT);

  // Reach the chevron by tabbing, never by clicking.
  await page.keyboard.press('Tab');
  for (
    let i = 0;
    i < 20 && !(await chevron(page).evaluate((el) => el === document.activeElement));
    i++
  ) {
    await page.keyboard.press('Tab');
  }
  await expect(chevron(page)).toBeFocused();

  await page.keyboard.press('Enter');
  await expect(panel(page)).toBeVisible();

  // Focus is captured inside the panel and trapped there: tabbing round never
  // escapes it.
  await expect(panel(page).locator(':focus')).toHaveCount(1);
  for (let i = 0; i < 8; i++) {
    await page.keyboard.press('Tab');
    expect(await panel(page).locator(':focus').count()).toBe(1);
  }

  // Set a filter from the keyboard alone — arrow within the status group, which
  // is a radiogroup, and it applies on the spot.
  await statusOption(page, 'All').focus();
  await page.keyboard.press('ArrowRight');
  await expect(page).toHaveURL(/status=downloaded/);

  await page.keyboard.press('Escape');
  await expect(panel(page)).toBeHidden();
  // Back where we started, so a keyboard user never loses their place.
  await expect(chevron(page)).toBeFocused();
});

test('Escape closes the panel, keeping what was already applied', async ({ page }) => {
  await stubSearch(page);
  await page.goto('/library');
  await expect(page.locator('.library__grid')).toBeVisible(FIRST_PAINT);

  await chevron(page).click();
  await statusOption(page, 'Not downloaded').click();
  // Filters apply as they are set, so this is already in force before Escape.
  await expect(page).toHaveURL(/status=not-downloaded/);

  await page.keyboard.press('Escape');

  await expect(panel(page)).toBeHidden();
  await expect(chevron(page)).toBeFocused();
  // Escape closes, it does not undo — and the summary row reports what stands.
  await expect(page).toHaveURL(/status=not-downloaded/);
  await expect(page.locator('.library__active-filters')).toBeVisible();

  // Reopening shows the filter in force, since the panel only ever shows that.
  await chevron(page).click();
  await expect(statusOption(page, 'Not downloaded')).toBeChecked();
});

test('a filter reaches the URL as it is set, and a reload keeps it', async ({ page }) => {
  const { queries } = await stubSearch(page);
  await page.goto('/library');
  await expect(page.locator('.library__grid')).toBeVisible(FIRST_PAINT);

  await chevron(page).click();
  await statusOption(page, 'Downloaded').click();

  // No Apply to press, and the panel stays open so the grid can be watched
  // answering while filters are tuned.
  await expect(panel(page)).toBeVisible();
  await expect(page).toHaveURL(/status=downloaded/);
  await expect(page).toHaveURL(/page=0/);
  // The three-way vocabulary in the URL, the API's status list on the wire.
  await expect.poll(() => queries.at(-1)).toContain('status=FINISH');

  // A summary chip makes the active filter visible with the panel closed.
  const summary = page.locator('.library__active-filters');
  await expect(summary).toBeVisible();
  await expect(summary.getByText('Downloaded')).toBeVisible();

  await page.reload();
  await expect(page.locator('.library__active-filters')).toBeVisible();
  await expect(page).toHaveURL(/status=downloaded/);
});

test('removing a chip applies immediately', async ({ page }) => {
  await stubSearch(page);
  await page.goto('/library?status=downloaded&tags=tech');
  await expect(page.locator('.library__active-filters')).toBeVisible(FIRST_PAINT);

  await page.getByRole('button', { name: 'Remove tag tech' }).click();

  // The other criterion survives, and the tags parameter is gone rather than empty.
  await expect(page).toHaveURL(/status=downloaded/);
  await expect(page).not.toHaveURL(/tags=/);
});

test('an empty result blames the filters rather than the library', async ({ page }) => {
  await page.route('**/api/v1/tags/search*', (route) => route.fulfill({ json: { content: [] } }));
  await page.route(SEARCH_ROUTE, (route) =>
    route.fulfill({
      json: {
        content: [],
        empty: true,
        first: true,
        last: true,
        number: 0,
        numberOfElements: 0,
        size: 24,
        totalElements: 0,
        totalPages: 0,
      },
    }),
  );

  await page.goto('/library');
  await expect(page.getByRole('heading', { name: 'No episodes yet' })).toBeVisible(FIRST_PAINT);

  await page.goto('/library?status=downloaded');
  await expect(
    page.getByRole('heading', { name: 'No episodes match these filters' }),
  ).toBeVisible();

  await page.getByRole('button', { name: 'Clear filters' }).click();
  await expect(page).not.toHaveURL(/status=/);
  await expect(page.getByRole('heading', { name: 'No episodes yet' })).toBeVisible();
});

test('Enter in the search field submits, panel open or closed', async ({ page }) => {
  const { queries } = await stubSearch(page);
  await page.goto('/library');
  await expect(page.locator('.library__grid')).toBeVisible(FIRST_PAINT);

  const search = page.getByPlaceholder('Filter episodes…');

  await search.fill('kotlin');
  await search.press('Enter');
  await expect(page).toHaveURL(/q=kotlin/);

  // With the panel open the form holds a second text input — the tag field.
  // A form with no submit button stops submitting implicitly past one such
  // field, so Enter used to do nothing at all here.
  await chevron(page).click();
  await expect(panel(page)).toBeVisible();
  await search.fill('gradle');
  await search.press('Enter');

  await expect(page).toHaveURL(/q=gradle/);
  await expect.poll(() => queries.at(-1)).toContain('q=gradle');
});

test('picking a highlighted suggestion adds that one tag, not two', async ({ page }) => {
  await stubSearch(page);
  await stubTagSuggestions(page, ['dev', 'design']);
  await page.goto('/library');
  await expect(page.locator('.library__grid')).toBeVisible(FIRST_PAINT);

  await chevron(page).click();
  await expect(panel(page)).toBeVisible();

  // Type a prefix, arrow onto the first suggestion, take it with Enter. Enter is
  // also the chip separator, so this used to commit the prefix as well and leave
  // two tags behind — "d" and "dev".
  const tagInput = panel(page).locator('input[placeholder="Search tags…"]');
  await tagInput.fill('d');
  await expect(page.getByRole('option', { name: 'dev' })).toBeVisible();
  await tagInput.press('ArrowDown');
  await tagInput.press('Enter');

  const chips = panel(page).getByRole('row');
  await expect(chips).toHaveCount(1);
  await expect(chips.first()).toContainText('dev');
  // And the prefix must not linger in the field beside the tag it produced.
  await expect(tagInput).toHaveValue('');

  // And a name no tag carries commits nothing: as a filter it could only ever
  // return an empty result set, for what is usually a typo or the wrong case.
  await tagInput.fill('nosuchtag');
  await expect(page.getByRole('option', { name: 'No tag matches' })).toBeVisible();
  await tagInput.press('Enter');
  await expect(panel(page).getByRole('row')).toHaveCount(1);
  await expect(tagInput).toHaveValue('');
});

test('Tab takes the first match, so a lookup costs one key', async ({ page }) => {
  await stubSearch(page);
  await stubTagSuggestions(page, ['dev', 'design']);
  await page.goto('/library');
  await expect(page.locator('.library__grid')).toBeVisible(FIRST_PAINT);

  await chevron(page).click();
  await expect(panel(page)).toBeVisible();

  const tagInput = panel(page).locator('input[placeholder="Search tags…"]');

  // A prefix and Tab, rather than ArrowDown then Enter.
  await tagInput.fill('de');
  await expect(page.getByRole('option', { name: 'dev' })).toBeVisible();
  await tagInput.press('Tab');

  await expect(panel(page).getByRole('row')).toHaveCount(1);
  await expect(panel(page).getByRole('row').first()).toContainText('dev');
  await expect(page).toHaveURL(/tags=dev/);
  // Tab was consumed by the completion, so the field is still where it was.
  await expect(tagInput).toBeFocused();
  await expect(tagInput).toHaveValue('');

  // With nothing left to take, Tab means Tab again — focus is never trapped.
  await tagInput.press('Tab');
  await expect(tagInput).not.toBeFocused();
});

test('Tab still commits nothing when no tag matches', async ({ page }) => {
  await stubSearch(page);
  await stubTagSuggestions(page, ['dev']);
  await page.goto('/library');
  await expect(page.locator('.library__grid')).toBeVisible(FIRST_PAINT);

  await chevron(page).click();
  await expect(panel(page)).toBeVisible();

  // A name nothing carries: completing has nothing to offer, and committing the
  // text would filter on a tag that does not exist and empty the page.
  const tagInput = panel(page).locator('input[placeholder="Search tags…"]');
  await tagInput.fill('nosuchtag');
  await expect(page.getByRole('option', { name: 'No tag matches' })).toBeVisible();
  await tagInput.press('Tab');

  await expect(panel(page).getByRole('row')).toHaveCount(0);
  await expect(page).not.toHaveURL(/tags=/);
});

test('clicking a suggestion also clears the typed prefix', async ({ page }) => {
  await stubSearch(page);
  await stubTagSuggestions(page, ['dev', 'design']);
  await page.goto('/library');
  await expect(page.locator('.library__grid')).toBeVisible(FIRST_PAINT);

  await chevron(page).click();
  await expect(panel(page)).toBeVisible();

  const tagInput = panel(page).locator('input[placeholder="Search tags…"]');
  await tagInput.fill('d');
  await page.getByRole('option', { name: 'design' }).click();

  await expect(panel(page).getByRole('row')).toHaveCount(1);
  await expect(panel(page).getByRole('row').first()).toContainText('design');
  await expect(tagInput).toHaveValue('');
});
