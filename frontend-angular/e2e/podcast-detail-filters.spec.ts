import { Page, expect, test } from '@playwright/test';

// #273, second host: the same panel on a podcast's own page, filtering that
// podcast's items. Tags are deliberately absent there — `ItemRepository.search`
// matches them through PODCAST_TAGS on ITEM.PODCAST_ID, so inside one podcast
// every item carries the same set and the control would return everything or
// nothing.

const PODCAST = {
  id: 'p1',
  title: 'A podcast',
  url: 'https://example.com/rss',
  hasToBeDeleted: false,
  lastUpdate: '2024-01-01T00:00:00Z',
  type: 'RSS',
  tags: [{ id: 't1', name: 'dev' }],
  cover: { id: 'c1', width: 200, height: 200, url: 'u', proxyURL: 'u' },
};

function item(i: number) {
  return {
    id: `i-${i}`,
    title: `Episode ${i}`,
    url: 'u',
    pubDate: '2024-01-01T00:00:00Z',
    downloadDate: null,
    creationDate: '2024-01-01T00:00:00Z',
    description: '',
    mimeType: 'audio/mpeg',
    length: null,
    fileName: null,
    status: 'NOT_DOWNLOADED',
    podcast: { id: 'p1', title: 'A podcast', url: 'u' },
    cover: { id: 'c1', width: 200, height: 200, url: 'u', proxyURL: 'u' },
    isDownloaded: false,
    podcastId: 'p1',
    proxyURL: 'u',
  };
}

const pageOf = (n: number) => ({
  content: Array.from({ length: n }, (_, i) => item(i)),
  empty: n === 0,
  first: true,
  last: true,
  number: 0,
  numberOfElements: n,
  size: 24,
  totalElements: n,
  totalPages: 1,
});

/** Serves the podcast and its items, recording what was asked of the API. */
async function stub(
  page: Page,
  counts: { unfiltered?: number; filtered?: number } = {},
): Promise<{ queries: string[] }> {
  const unfiltered = counts.unfiltered ?? 3;
  const filtered = counts.filtered ?? 0;
  const queries: string[] = [];
  await page.route('**/api/v1/podcasts/p1', (route) => route.fulfill({ json: PODCAST }));
  await page.route('**/api/v1/podcasts/p1/stats/**', (route) => route.fulfill({ json: [] }));
  await page.route('**/api/v1/podcasts/p1/items*', (route) => {
    const url = new URL(route.request().url());
    queries.push(url.search);
    return route.fulfill({
      json: pageOf(url.searchParams.get('status') ? filtered : unfiltered),
    });
  });
  return { queries };
}

const chevron = (page: Page) => page.getByRole('button', { name: 'More filters' });
const panel = (page: Page) => page.getByRole('dialog', { name: 'More filters' });
const FIRST_PAINT = { timeout: 30_000 };

test('offers status but not tags, and says which podcast it applies to', async ({ page }) => {
  await stub(page);
  await page.goto('/podcasts/p1');
  await expect(chevron(page)).toBeVisible(FIRST_PAINT);

  await chevron(page).click();
  await expect(panel(page)).toBeVisible();

  await expect(panel(page).locator('ps-tags-field')).toHaveCount(0);
  await expect(panel(page).getByRole('radio')).toHaveText(['All', 'Downloaded', 'Not downloaded']);
  // The library's heading claims every podcast; here that would be a lie.
  await expect(panel(page).locator('.filters__group-title')).toHaveText('This podcast');
});

test('a status filter reaches this podcast’s items and survives a reload', async ({ page }) => {
  const { queries } = await stub(page);
  await page.goto('/podcasts/p1');
  await expect(chevron(page)).toBeVisible(FIRST_PAINT);

  await chevron(page).click();
  await panel(page).getByRole('radio', { name: 'Downloaded', exact: true }).click();

  // Applied as it is set, and the panel stays open.
  await expect(panel(page)).toBeVisible();
  await expect(page).toHaveURL(/status=downloaded/);
  await expect(page).toHaveURL(/page=0/);
  // The three-way vocabulary in the URL, the API's status list on the wire —
  // and on the podcast's own endpoint, not the global search.
  await expect.poll(() => queries.at(-1)).toContain('status=FINISH');

  await expect(page.locator('.pod__active-filters')).toContainText('Downloaded');

  await page.reload();
  await expect(page.locator('.pod__active-filters')).toBeVisible(FIRST_PAINT);
});

test('the panel is an extension of the field here too', async ({ page }) => {
  await stub(page);
  await page.goto('/podcasts/p1');
  await expect(chevron(page)).toBeVisible(FIRST_PAINT);

  await chevron(page).click();
  await expect(panel(page)).toBeVisible();
  await page
    .locator('.filters')
    .evaluate((el) =>
      Promise.all(el.getAnimations({ subtree: true }).map((a) => a.finished)).then(() => undefined),
    );

  const box = await page.evaluate(() => {
    const f = document.querySelector('.pod__search > mat-form-field')!.getBoundingClientRect();
    const p = document.querySelector('.filters')!.getBoundingClientRect();
    return { dx: Math.round(p.x - f.x), dw: Math.round(p.width - f.width) };
  });
  expect(box).toEqual({ dx: 0, dw: 0 });

  await expect(
    page.locator('.pod__search > mat-form-field .mdc-notched-outline__leading').first(),
  ).toHaveCSS('border-bottom-color', 'rgba(0, 0, 0, 0)');

  // And focusing the field leaves its outline alone, so the two halves of the
  // shape keep the same colour and weight.
  await page.getByPlaceholder('Filter episodes…').click();
  const border = await page.evaluate(() => {
    const side = (el: Element, s: 'Left' | 'Top') =>
      `${getComputedStyle(el)[`border${s}Color`]} ${getComputedStyle(el)[`border${s}Width`]}`;
    return {
      field: side(
        document.querySelector('.pod__search > mat-form-field .mdc-notched-outline__leading')!,
        'Top',
      ),
      panel: side(document.querySelector('.filters')!, 'Left'),
    };
  });
  expect(border.field).toBe(border.panel);
});

test('an empty result blames the filter rather than an empty podcast', async ({ page }) => {
  // Empty either way, so the only thing separating the two messages is whether
  // a filter is active.
  await stub(page, { unfiltered: 0, filtered: 0 });
  await page.goto('/podcasts/p1');
  await expect(chevron(page)).toBeVisible(FIRST_PAINT);

  await chevron(page).click();
  await panel(page).getByRole('radio', { name: 'Downloaded', exact: true }).click();

  // Not "trigger an update": the podcast may be full, just not of what was asked.
  await expect(page.getByRole('heading', { name: 'No episodes match this filter' })).toBeVisible();

  await page.getByRole('button', { name: 'Clear filter' }).click();
  await expect(page).not.toHaveURL(/status=/);
  await expect(page.getByRole('heading', { name: 'No episodes yet' })).toBeVisible();
});

test('setting a filter does not move the episode list', async ({ page }) => {
  await stub(page, { unfiltered: 3, filtered: 3 });

  const listY = () =>
    page.evaluate(() =>
      Math.round(document.querySelector('.pod__list')!.getBoundingClientRect().y),
    );

  await page.goto('/podcasts/p1');
  await expect(page.locator('.pod__list')).toBeVisible(FIRST_PAINT);
  const before = await listY();

  await chevron(page).click();
  await panel(page).getByRole('radio', { name: 'Downloaded', exact: true }).click();
  await expect(page.locator('.pod__active-filters')).toContainText('Downloaded');
  // The list is unmounted while the filtered page loads; wait for it back
  // before measuring, or there is nothing to measure.
  await expect(page.locator('.pod__list')).toBeVisible();

  // The summary row keeps its space empty or not, so the header never changes
  // height and nothing below it shifts.
  expect(await listY()).toBe(before);
});
