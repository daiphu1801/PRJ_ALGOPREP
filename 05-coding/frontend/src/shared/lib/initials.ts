/**
 * Two-letter avatar initials: the first letters of the last two words of a display name, uppercase
 * (BD USR0502 Khu vực A NO 1; same rule as the class and admin tables). Empty name gives "".
 */
export function initialsOf(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((word) => word.charAt(0).toUpperCase())
    .join("");
}
