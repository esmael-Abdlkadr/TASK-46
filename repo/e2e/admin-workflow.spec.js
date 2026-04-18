import { test, expect } from '@playwright/test';

test('admin logs in and reaches dashboard', async ({ page }) => {
    const res = await page.goto('/login', { waitUntil: 'domcontentloaded' }).catch(() => null);
    if (!res || res.status() >= 500) {
        test.skip(true, 'Backend not running at E2E_BASE_URL (start docker compose)');
    }

    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.getByRole('button', { name: 'Sign In' }).click();

    await expect(page).toHaveURL(/\/admin\/dashboard/, { timeout: 20000 });
    await expect(page.getByRole('heading', { name: 'Administrator Dashboard' })).toBeVisible();
});

test('admin navigates to users management', async ({ page }) => {
    const res = await page.goto('/login', { waitUntil: 'domcontentloaded' }).catch(() => null);
    if (!res || res.status() >= 500) {
        test.skip(true, 'Backend not running at E2E_BASE_URL');
    }

    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await page.waitForURL(/\/admin\/dashboard/, { timeout: 20000 });

    await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
    await expect(page).toHaveURL(/\/admin\/users/);
    await expect(page.getByRole('heading', { name: 'User Management' })).toBeVisible();
});

test('recruiter cannot open admin dashboard', async ({ page }) => {
    const res = await page.goto('/login', { waitUntil: 'domcontentloaded' }).catch(() => null);
    if (!res || res.status() >= 500) {
        test.skip(true, 'Backend not running at E2E_BASE_URL');
    }

    await page.fill('#username', 'recruiter');
    await page.fill('#password', 'recruiter123');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await page.waitForURL(/\/recruiter\/dashboard/, { timeout: 20000 });

    const dash = await page.goto('/admin/dashboard', { waitUntil: 'domcontentloaded' });
    const code = dash?.status() ?? 0;
    if (code >= 400) {
        expect(code).toBe(403);
        return;
    }

    await expect(page.getByText(/Access Denied|403|Forbidden|not authorized/i).first()).toBeVisible({
        timeout: 5000,
    });
});
