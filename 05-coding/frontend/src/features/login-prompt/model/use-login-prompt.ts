"use client";

import { useCallback, useState } from "react";

/**
 * Trạng thái mở/đóng của hộp thoại nhắc đăng nhập và nơi quay lại. `ask(returnTo)` mở hộp thoại
 * cho đường dẫn đó; component chỉ việc dựng `LoginPromptDialog` với `prompt.returnTo`.
 */
export function useLoginPrompt() {
  const [returnTo, setReturnTo] = useState<string | null>(null);
  const ask = useCallback((path: string) => setReturnTo(path), []);
  const close = useCallback(() => setReturnTo(null), []);
  return {
    ask,
    close,
    open: returnTo !== null,
    returnTo: returnTo ?? "/problems",
  };
}
