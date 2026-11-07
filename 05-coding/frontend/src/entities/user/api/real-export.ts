// Real calls for `GET /identity/me/export` (03-dd/api/F1_identity.md #16): `scope=profile` answers
// JSON, `scope=submissions` answers a CSV file. The interview transcripts (#17) need ai-review,
// which does not exist yet, so that one fails here with the same 501 the server would answer and
// the card shows its usual error toast instead of handing over an empty file.
import { api, ApiError } from "@/shared/api";

/** `truncated`: the server cut the submission file at its newest 2000 rows (header X-Export-Truncated). */
export type ExportedFile = { blob: Blob; truncated: boolean };

export async function realExportMyData(
  scope: "profile" | "submissions",
): Promise<ExportedFile> {
  if (scope === "submissions") {
    // The file comes straight from the server; errors (IDT-V06, IDT-508) reject as ApiError.
    const file = await api.downloadFile("/identity/me/export", {
      params: { scope },
    });
    return {
      blob: file.blob,
      truncated: file.header("x-export-truncated") === "true",
    };
  }
  const data = await api.get<unknown>("/identity/me/export", { scope });
  return {
    blob: new Blob([JSON.stringify(data, null, 2)], {
      type: "application/json",
    }),
    truncated: false,
  };
}

export function realExportInterviewTranscripts(): Promise<Blob> {
  return Promise.reject(notBuilt());
}

function notBuilt(): ApiError {
  return new ApiError("NOT_IMPLEMENTED", 501, "Arrives with F5");
}
