// Real call for `GET /identity/me/export?scope=profile` (03-dd/api/F1_identity.md #16). The
// submissions export needs the judge module and the interview transcripts (#17) need ai-review;
// neither exists yet, so those fail here with the same 501 the server would answer, and the card
// shows its usual error toast instead of handing over an empty file.
import { api, ApiError } from "@/shared/api";

export async function realExportMyData(
  scope: "profile" | "submissions",
): Promise<Blob> {
  if (scope !== "profile") throw notBuilt();
  const data = await api.get<unknown>("/identity/me/export", { scope });
  return new Blob([JSON.stringify(data, null, 2)], {
    type: "application/json",
  });
}

export function realExportInterviewTranscripts(): Promise<Blob> {
  return Promise.reject(notBuilt());
}

function notBuilt(): ApiError {
  return new ApiError("NOT_IMPLEMENTED", 501, "Arrives with F4 and F5");
}
