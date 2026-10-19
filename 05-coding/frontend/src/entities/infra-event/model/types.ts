/**
 * Infrastructure event (ADM0404): a log line emitted by a machine (go-judge worker, Outbox relay), NOT an administrative action taken by a person. Human actions belong to the system
 * log (`admin_system_log`, F1-14); the split is a hard boundary set by BD ADM0401.
 */
export const INFRA_LEVELS = ["error", "warn", "info"] as const;
export type InfraLevel = (typeof INFRA_LEVELS)[number];

export type InfraEvent = {
  id: string;
  /** "YYYY-MM-DD", compared as a string for the day-range filter. */
  date: string;
  /** "HH:mm:ss". */
  time: string;
  level: InfraLevel;
  message: string;
  service: string;
};
