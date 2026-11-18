"use client";

import { Avatar, type AvatarSize } from "@/shared/ui";
import { useAvatarUrl } from "../api/avatar-queries";

/** A person's picture, or their initials when `avatarVersion` is empty (no request is made then). */
export function UserAvatar({
  userId,
  name,
  avatarVersion,
  size,
  className,
}: {
  userId?: string;
  name: string;
  avatarVersion?: number | null;
  size?: AvatarSize;
  className?: string;
}) {
  const src = useAvatarUrl(userId, avatarVersion);
  return <Avatar src={src} name={name} size={size} className={className} />;
}
