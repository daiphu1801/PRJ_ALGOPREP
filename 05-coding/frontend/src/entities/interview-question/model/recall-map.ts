// One place that maps the screen's recall words to the wire's (USR0401 DD 2, point 1): the URL and the
// UI say `new` and `known`, the API says `UNSEEN` and `KNOWN`.
import type { RecallStatusFilter } from "./learner-types";
import type { RecallLevel } from "./types";

export type ApiRating = "KNOWN" | "VAGUE" | "FORGOTTEN";
export type ApiStatus = "ALL" | "UNSEEN" | "REVIEWING" | "KNOWN";

export const toApiRating = (level: RecallLevel): ApiRating =>
  level.toUpperCase() as ApiRating;

export function fromApiRating(rating: string | null | undefined): RecallLevel | null {
  switch (rating) {
    case "KNOWN":
      return "known";
    case "VAGUE":
      return "vague";
    case "FORGOTTEN":
      return "forgotten";
    default:
      return null;
  }
}

const STATUS: Record<RecallStatusFilter, ApiStatus> = {
  all: "ALL",
  new: "UNSEEN",
  reviewing: "REVIEWING",
  known: "KNOWN",
};

export const toApiStatus = (status: RecallStatusFilter): ApiStatus =>
  STATUS[status];
