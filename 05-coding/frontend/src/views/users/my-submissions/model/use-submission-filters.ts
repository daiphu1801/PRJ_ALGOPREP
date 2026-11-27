"use client";
import { useEffect, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  normalizeSubmissionQuery,
  readSubmissionFilters,
  submissionFiltersUrl,
  type SubmissionFilters,
} from "./submission-url";

export function useSubmissionFilters() {
  const pathname = usePathname();
  const search = useSearchParams().toString();
  const router = useRouter();
  const filters = readSubmissionFilters(new URLSearchParams(search));
  const location = `${pathname}?${search}`;
  const [input, setInput] = useState({ location, text: filters.query });
  // A new URL (including Back/Forward) owns the input and invalidates any pending draft search.
  if (input.location !== location) setInput({ location, text: filters.query });
  const query = input.location === location ? input.text : filters.query;
  useEffect(() => {
    const committed = readSubmissionFilters(new URLSearchParams(search));
    if (normalizeSubmissionQuery(query) === committed.query) return;
    const timer = setTimeout(() => {
      const next = submissionFiltersUrl(pathname, search, {
        ...committed,
        query,
        page: 1,
      });
      router.replace(next, { scroll: false });
    }, 400);
    return () => clearTimeout(timer);
  }, [query, search, pathname, router]);

  function navigate(patch: Partial<SubmissionFilters>) {
    const pendingSearch = normalizeSubmissionQuery(query) !== filters.query;
    const next = submissionFiltersUrl(pathname, search, {
      ...filters,
      query,
      ...patch,
      ...(pendingSearch ? { page: 1 } : {}),
    });
    if (next !== (search ? `${pathname}?${search}` : pathname))
      router.push(next, { scroll: false });
  }
  return {
    filters,
    query,
    setQuery: (text: string) => setInput({ location, text }),
    navigate,
  };
}
