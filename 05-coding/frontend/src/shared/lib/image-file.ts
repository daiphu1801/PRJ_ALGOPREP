export const IMAGE_TYPES = ["image/png", "image/jpeg", "image/webp"] as const;

/** Pre-check of a picked image file before it is decoded: `null` when fine, else which rule failed. */
export function checkImageFile(
  file: Pick<File, "type" | "size">,
  maxBytes: number,
  types: readonly string[] = IMAGE_TYPES,
): "type" | "size" | null {
  if (!types.includes(file.type)) return "type";
  if (file.size > maxBytes) return "size";
  return null;
}
