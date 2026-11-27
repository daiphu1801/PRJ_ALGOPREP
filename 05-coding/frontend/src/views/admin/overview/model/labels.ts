// Real mode sends codes (PYTHON, ACCEPTED, totalSubmissions); the label is a message under
// `adminOverview.labels`, so vi and en both read right. Mock data carries a ready label and no key.
export function translateLabel<T extends { label: string; labelKey?: string }>(
  t: (key: string) => string,
  item: T,
): T {
  return item.labelKey ? { ...item, label: t(item.labelKey) } : item;
}
