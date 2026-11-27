// Real calls for `GET /identity/me/export` (03-dd/api/F1_identity.md #16): `scope=profile` answers
// JSON, `scope=submissions` answers a CSV file. Transcript export (#17) downloads the server's
// caller-scoped JSON file and preserves rate-limit/dependency errors for the settings card.
import { api } from "@/shared/api";

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

export async function realExportInterviewTranscripts(): Promise<Blob> {
  return (await api.downloadFile("/identity/me/export/interview-transcripts"))
    .blob;
}
