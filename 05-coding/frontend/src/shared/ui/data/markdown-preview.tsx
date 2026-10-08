// Read-only rendering of author-written Markdown (problem statements, interview questions) with
// GitHub-flavoured tables and LaTeX ($...$ and $$...$$) via KaTeX. Authoring screens keep the raw
// Markdown in a textarea; every read-only screen shows this instead.
//
// Safety: no `rehype-raw` is installed or enabled, so raw HTML typed into the Markdown is shown as
// text, never parsed into elements. Do not add it without a sanitizer (shared/lib/sanitize-html.ts
// still throws on purpose).
import ReactMarkdown from "react-markdown";
import rehypeKatex from "rehype-katex";
import remarkGfm from "remark-gfm";
import remarkMath from "remark-math";
import "katex/dist/katex.min.css";
import { cn } from "@/shared/lib";

type MarkdownPreviewProps = {
  children: string;
  /** "lg" is for a screen where this text IS the main content (the read-only detail pages). */
  size?: "base" | "lg";
  className?: string;
};

export function MarkdownPreview({
  children,
  size = "base",
  className,
}: MarkdownPreviewProps) {
  return (
    <div
      className={cn(
        "text-pretty break-words",
        size === "lg"
          ? "text-[15px] leading-7 [&>*+*]:mt-4"
          : "text-[13.5px] leading-relaxed [&>*+*]:mt-3",
        size === "lg"
          ? "[&_h1]:text-xl [&_h1]:font-bold [&_h2]:text-lg [&_h2]:font-bold [&_h3]:text-base [&_h3]:font-semibold"
          : "[&_h1]:text-lg [&_h1]:font-bold [&_h2]:text-base [&_h2]:font-bold [&_h3]:text-[14.5px] [&_h3]:font-semibold",
        "[&_ul]:list-disc [&_ul]:pl-5 [&_ol]:list-decimal [&_ol]:pl-5 [&_li+li]:mt-1",
        "[&_a]:text-[var(--color-accent-blue)] [&_a]:underline",
        "[&_blockquote]:border-l-2 [&_blockquote]:border-[var(--color-border)] [&_blockquote]:pl-3 [&_blockquote]:text-[var(--color-text-muted)]",
        "[&_:not(pre)>code]:rounded [&_:not(pre)>code]:bg-[var(--color-surface-hover)] [&_:not(pre)>code]:px-1 [&_:not(pre)>code]:py-0.5 [&_:not(pre)>code]:font-mono",
        "[&_pre]:overflow-x-auto [&_pre]:rounded-xl [&_pre]:bg-[var(--color-surface-hover)] [&_pre]:p-3 [&_pre]:font-mono",
        size === "lg"
          ? "[&_:not(pre)>code]:text-[14px] [&_pre]:text-[14px]"
          : "[&_:not(pre)>code]:text-[12.5px] [&_pre]:text-[12.5px]",
        "[&_table]:w-full [&_table]:border-collapse [&_td]:border [&_td]:border-[var(--color-border)] [&_td]:px-2 [&_td]:py-1 [&_th]:border [&_th]:border-[var(--color-border)] [&_th]:px-2 [&_th]:py-1 [&_th]:text-left",
        className,
      )}
    >
      <ReactMarkdown
        remarkPlugins={[remarkGfm, remarkMath]}
        rehypePlugins={[rehypeKatex]}
      >
        {children}
      </ReactMarkdown>
    </div>
  );
}
