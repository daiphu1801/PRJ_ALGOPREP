import { Suspense } from "react";
import { OAuthCallbackView } from "@/views/shared/auth-callback";

// The provider redirects here with `code` and `state` (DD F1 section 0.1, route registered at the
// provider); the view reads the query on the client, hence the Suspense boundary.
export default async function OAuthCallbackPage({
  params,
}: {
  params: Promise<{ provider: string }>;
}) {
  const { provider } = await params;
  return (
    <Suspense>
      <OAuthCallbackView provider={provider} />
    </Suspense>
  );
}
