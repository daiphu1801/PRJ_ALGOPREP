// Blocks leaving a screen while it holds unsaved changes (BD SHR0202 Q7, Q12, Q14).
//
// Next's App Router has no route-change blocker, so three exits are covered by hand:
// - a click on an in-app link (capture phase, so it runs before Next's own Link handler);
// - the browser Back button (a sentinel history entry is pushed, popstate re-pushes it);
// - closing or reloading the tab (`beforeunload`; the browser shows its own generic dialog, the
//   text cannot be customised).
// The first two open the screen's own dialog through `pending`; `stay` dismisses it, `leave`
// carries on to where the user was going.
//
// ponytail: the sentinel entry is not removed when `dirty` turns false again, so after a save the
// next Back press lands on a duplicate entry once. Fine for a prototype; replace with the Navigation
// API (`navigation.addEventListener("navigate")`) once the browser baseline allows it.
"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

type Pending = { kind: "link"; href: string } | { kind: "back" } | null;

export function useUnsavedChangesGuard(dirty: boolean) {
  const router = useRouter();
  const [pending, setPending] = useState<Pending>(null);
  const bypass = useRef(false);

  useEffect(() => {
    if (!dirty) return;

    const onBeforeUnload = (event: BeforeUnloadEvent) => {
      if (bypass.current) return;
      event.preventDefault();
      event.returnValue = "";
    };

    const onClick = (event: MouseEvent) => {
      if (bypass.current || event.defaultPrevented || event.button !== 0)
        return;
      if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey)
        return;
      const anchor = (event.target as Element | null)?.closest?.(
        "a[href]",
      ) as HTMLAnchorElement | null;
      if (
        !anchor ||
        (anchor.target && anchor.target !== "_self") ||
        anchor.hasAttribute("download")
      )
        return;
      const url = new URL(anchor.href, window.location.href);
      if (url.origin !== window.location.origin) return;
      if (
        url.pathname + url.search ===
        window.location.pathname + window.location.search
      )
        return;
      event.preventDefault();
      event.stopPropagation();
      setPending({ kind: "link", href: url.pathname + url.search + url.hash });
    };

    const onPopState = () => {
      if (bypass.current) return;
      // Undo the Back press, then ask.
      window.history.pushState(null, "", window.location.href);
      setPending({ kind: "back" });
    };

    window.history.pushState(null, "", window.location.href);
    window.addEventListener("beforeunload", onBeforeUnload);
    window.addEventListener("popstate", onPopState);
    document.addEventListener("click", onClick, true);
    return () => {
      window.removeEventListener("beforeunload", onBeforeUnload);
      window.removeEventListener("popstate", onPopState);
      document.removeEventListener("click", onClick, true);
    };
  }, [dirty]);

  const stay = useCallback(() => setPending(null), []);

  const leave = useCallback(() => {
    const target = pending;
    setPending(null);
    bypass.current = true;
    if (target?.kind === "link") router.push(target.href);
    // Two entries to skip: the sentinel and the entry Back would have gone to.
    else if (target?.kind === "back") window.history.go(-2);
  }, [pending, router]);

  return { pending: pending !== null, stay, leave };
}
