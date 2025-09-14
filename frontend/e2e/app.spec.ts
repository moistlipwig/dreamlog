import { test, expect } from '@playwright/test';

test('login button visible', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible();
});
