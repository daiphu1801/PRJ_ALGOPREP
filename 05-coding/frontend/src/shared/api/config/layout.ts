// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// One place to tune how wide each actor's shell is allowed to get. Before this, the cap was a
// magic number inlined per shell (`max-w-[1320px]` in the Admin branch of app-shell.tsx,
// `max-w-[1400px]` in the Student one), so the two drifted and neither was adjustable without
// editing JSX.
//
// Owner instruction 2026-09-27: all three actor prototypes share one responsive container with a
// configurable pixel cap. That OVERRIDES 02-bd/screens/users/_shell.md mục 4, which had said the
// Student area deliberately has no shared max-width and each screen declares its own — recorded in
// DEC-2026-0927-student-area-merge-and-shared-shell.
//
// The number is a CAP, not a width: below it the container is fluid, so this is the point where
// the layout stops growing, not where it starts being responsive.
//
// Keys are the three permission-gated areas of `entities/user`'s `AppArea`, spelled out rather
// than imported: `shared/` may not import `entities/` (FSD, eslint boundaries/element-types).
export const SHELL_MAX_WIDTH_PX = {
  // 1320 is what every Admin prototype used (09-layoutBase/Admin - Tổng quan.dc.html:104).
  admin: 1320,
  instructor: 1320,
  // 1400 is what every Student prototype used (09-layoutBase/Dashboard AlgoPrep.dc.html:50, 94).
  student: 1400,
} as const;

export type ShellArea = keyof typeof SHELL_MAX_WIDTH_PX;
