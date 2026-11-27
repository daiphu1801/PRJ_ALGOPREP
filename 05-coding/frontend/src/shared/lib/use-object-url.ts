"use client";

import { useEffect, useState } from "react";

/** Temporary `blob:` URL for a Blob, revoked when the blob changes or the component unmounts. */
export function useObjectUrl(
  blob: Blob | null | undefined,
): string | undefined {
  const [made, setMade] = useState<{ blob: Blob; url: string }>();
  useEffect(() => {
    if (!blob) return;
    const url = URL.createObjectURL(blob);
    // The URL only exists after this effect creates it, so state hands it to the next render.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMade({ blob, url });
    return () => URL.revokeObjectURL(url);
  }, [blob]);
  return blob && made?.blob === blob ? made.url : undefined;
}
