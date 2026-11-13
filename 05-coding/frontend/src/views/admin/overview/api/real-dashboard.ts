// Real calls for the identity blocks of the admin overview: #41, #42 and #73
// (03-dd/api/F1_identity.md section 0.8.5). The seven judge-backed blocks are in real-submissions.ts.
import { api } from "@/shared/api";
import type { StatSummary, UserRetention } from "../model/types";

const BASE = "/identity/admin/dashboard";

type RetentionDto = {
  groups: { month: string; newUsers: number; returningUsers: number }[];
};

// The server sends the month as "yyyy-MM"; the chart axis reads "T<month>" like the prototype.
export function monthLabel(month: string): string {
  return `T${Number(month.slice(5, 7))}`;
}

export function realActiveUsersSummary(): Promise<StatSummary> {
  return api.get<StatSummary>(`${BASE}/summary`);
}

export function realPasswordResetsSummary(): Promise<StatSummary> {
  return api.get<StatSummary>(`${BASE}/password-resets`);
}

export async function realUserRetention(): Promise<UserRetention> {
  const dto = await api.get<RetentionDto>(`${BASE}/user-growth`);
  return {
    legend: [
      { label: "Người dùng mới", value: 0, colorVar: "--color-admin-teal" },
      { label: "Quay lại", value: 0, colorVar: "--color-admin-cyan" },
    ],
    groups: dto.groups.map((g) => ({
      label: monthLabel(g.month),
      bars: [
        { label: "Mới", value: g.newUsers, colorVar: "--color-admin-teal" },
        {
          label: "Quay lại",
          value: g.returningUsers,
          colorVar: "--color-admin-cyan",
        },
      ],
    })),
  };
}
