/**
 * BCrypt only reads the first 72 bytes of a password, so the backend rejects anything longer
 * instead of silently cutting it (DEC-2026-1030-bcrypt-password-byte-limit; backend error IDT-V02).
 * The limit is in UTF-8 BYTES, not characters: "ệ" costs 3 bytes.
 */
export const PASSWORD_MAX_BYTES = 72;

/** True when the password fits BCrypt's 72-byte limit once encoded as UTF-8. */
export function fitsBcrypt(password: string): boolean {
  return new TextEncoder().encode(password).length <= PASSWORD_MAX_BYTES;
}
