// A button that opens the native file chooser. The input is hidden, so the button is the only control
// a keyboard user meets. Picking the same file twice still fires `onFiles`, because the input is cleared
// after every pick. Used by the CSV import (SHR0301) and the bulk testcase upload (SHR0202).
"use client";

import { useRef, type ComponentPropsWithoutRef } from "react";
import { Button } from "../primitives/button";

type Props = Omit<ComponentPropsWithoutRef<typeof Button>, "onClick"> & {
  accept?: string;
  multiple?: boolean;
  /** Stable hook for tests: the hidden input has no accessible name of its own. */
  inputTestId?: string;
  onFiles: (files: File[]) => void;
};

export function FilePickerButton({
  accept,
  multiple,
  inputTestId,
  onFiles,
  children,
  ...buttonProps
}: Props) {
  const input = useRef<HTMLInputElement>(null);
  return (
    <>
      <input
        ref={input}
        type="file"
        accept={accept}
        multiple={multiple}
        hidden
        data-testid={inputTestId}
        onChange={(event) => {
          const files = Array.from(event.target.files ?? []);
          event.target.value = "";
          if (files.length > 0) onFiles(files);
        }}
      />
      <Button {...buttonProps} onClick={() => input.current?.click()}>
        {children}
      </Button>
    </>
  );
}
