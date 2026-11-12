/**
 * "Ý cần nói" arrives as one Markdown string (F6 #3, `suggestedApproach`). Each line starting with
 * `- `, `* ` or `n. ` opens an item; any other non-empty line is appended to the item before it, or
 * is an item of its own when it comes first [SoT: Suy luận, 03-dd/screens/users/USR0401 BR-SC-07].
 * Items are rendered as plain text by the callers, never as HTML.
 */
export function splitOutline(markdown: string | null | undefined): string[] {
  const items: string[] = [];
  for (const raw of (markdown ?? "").split(/\r?\n/)) {
    const line = raw.trim();
    if (!line) continue;
    const bullet = /^(?:[-*]|\d+\.)\s+(.*)$/.exec(line);
    if (bullet) items.push(bullet[1]!.trim());
    else if (items.length === 0) items.push(line);
    else items[items.length - 1] += ` ${line}`;
  }
  return items;
}
