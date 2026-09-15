import { redirect } from "next/navigation";

/**
 * The root route has no dedicated Landing screen — `01-rd/screens/` has no such screen among
 * its 25.
 *
 * Full behavior per `01-rd/screens/shared/auth.md:90` (Q3): not logged in goes to `/login`;
 * logged in goes to the role-based destination (`entities/user` → `HOME_PATH_BY_ROLE`), and if
 * the user arrived from a link that required login first, prefer returning to that exact URL.
 *
 * At this base-scaffold stage only the NOT-LOGGED-IN branch is implemented, since there is no
 * real session yet (`app/providers/auth-provider.tsx` is still a TODO, needs
 * `03-dd/api/identity.md`). The role-based branch and returnUrl belong to `middleware.ts` — to
 * be built alongside real auth, not guessed at ahead of time.
 */
export default function RootPage() {
  redirect("/login");
}
