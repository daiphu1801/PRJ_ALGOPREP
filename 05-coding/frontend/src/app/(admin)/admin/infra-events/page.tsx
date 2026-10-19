import { INFRA_LEVELS } from "@/entities/infra-event";
import { AdminInfraEventsView } from "@/views/admin/infra-events";

export default async function Page({
  searchParams,
}: {
  searchParams: Promise<{ level?: string }>;
}) {
  const { level } = await searchParams;
  // Anything outside the three known levels is ignored rather than erroring.
  const initialLevel = INFRA_LEVELS.find((known) => known === level);
  return <AdminInfraEventsView initialLevel={initialLevel} />;
}
