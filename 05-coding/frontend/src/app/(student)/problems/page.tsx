import { Suspense } from "react";
import { ProblemListView } from "@/views/users/problem-list";

export default function Page() {
  // The list keeps its filters in the query string, and useSearchParams needs a Suspense boundary.
  return (
    <Suspense fallback={null}>
      <ProblemListView />
    </Suspense>
  );
}
