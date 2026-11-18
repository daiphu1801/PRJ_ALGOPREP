// Avatar picture loading (F1 #76). An `<img>` cannot send the bearer token, so the bytes come
// through the API client as a Blob and are shown through a temporary object URL.
"use client";

import { useQuery } from "@tanstack/react-query";
import { withMockData } from "@/shared/api";
import { useObjectUrl } from "@/shared/lib/use-object-url";
import { fakeGetUserAvatar } from "./__mock__/fake-user";
import { realGetUserAvatar } from "./real-avatar";

/** Object URL of the user's picture, `undefined` while loading, on error, or when there is none. */
export function useAvatarUrl(
  userId: string | undefined,
  avatarVersion: number | null | undefined,
): string | undefined {
  const enabled = Boolean(userId) && avatarVersion != null;
  const query = useQuery({
    queryKey: ["user", "avatar", userId, avatarVersion],
    queryFn: () =>
      withMockData(
        () => fakeGetUserAvatar(userId as string),
        () => realGetUserAvatar(userId as string, avatarVersion as number),
      ),
    enabled,
    retry: false,
    staleTime: Infinity,
  });
  return useObjectUrl(enabled ? query.data : undefined);
}
