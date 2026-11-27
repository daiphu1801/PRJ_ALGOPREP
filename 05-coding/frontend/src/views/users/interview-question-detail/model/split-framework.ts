// The reference answer is one text with sections separated by a blank line, each opening with a "Heading:" line
// (for example "Cách trả lời:", "Ví dụ áp dụng:"). Cut it into sections so each one can have its own card.
export type FrameworkSection = { heading: string | null; body: string };

export function splitFramework(text: string): FrameworkSection[] {
  return text
    .split(/\n\s*\n/)
    .map((block) => block.trim())
    .filter(Boolean)
    .map((block) => {
      const [first = "", ...rest] = block.split("\n");
      if (
        rest.length > 0 &&
        first.trim().length <= 60 &&
        first.trim().endsWith(":")
      )
        return {
          heading: first.trim().slice(0, -1).trim(),
          body: rest.join("\n").trim(),
        };
      return { heading: null, body: block };
    });
}
