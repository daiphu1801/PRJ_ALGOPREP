// The AI returns the session summary (and each criterion comment) as ONE block of text, which reads as a wall.
// Cut it into short paragraphs: first at the line breaks the AI already wrote, then every `perParagraph`
// sentences. Pure display: the words are never changed.
const SENTENCE_END = /(?<=[.!?…])\s+(?=\S)/u;

export function splitIntoParagraphs(text: string, perParagraph = 2): string[] {
  const blocks = text
    .split(/\n+/)
    .map((block) => block.trim())
    .filter(Boolean);
  const paragraphs: string[] = [];
  for (const block of blocks) {
    const sentences = block
      .split(SENTENCE_END)
      .map((s) => s.trim())
      .filter(Boolean);
    for (let i = 0; i < sentences.length; i += perParagraph) {
      paragraphs.push(sentences.slice(i, i + perParagraph).join(" "));
    }
  }
  return paragraphs;
}
