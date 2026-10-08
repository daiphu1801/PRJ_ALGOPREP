import type { Locator, Page } from "@playwright/test";

// List filters are a button that opens a listbox of options (shared FilterMenu), not a row of pills.

/** The listbox of one filter, whether or not it is open right now. */
export function filterList(page: Page, label: string): Locator {
  return page.getByRole("listbox", { name: label });
}

export async function openFilter(page: Page, label: string): Promise<Locator> {
  await page.getByRole("button", { name: new RegExp(`^${label}:`) }).click();
  return filterList(page, label);
}

export async function pickFilter(
  page: Page,
  label: string,
  option: string,
): Promise<void> {
  const list = await openFilter(page, label);
  await list.getByRole("option", { name: option, exact: true }).click();
}
