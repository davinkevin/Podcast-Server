import { expect, test } from '@playwright/test';

test('the v3 shell loads under /v3/', async ({ page }) => {
  await page.goto('/v3/');

  await expect(page).toHaveTitle('Podcast Server');
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Podcast Server v3');
});
