// PROTOTYPE - no DD yet. See 06-plan/PROTOTYPE_DEBT.md.
//
// Monaco-based code editor (README.md mục 5 locks Monaco). Styled like the LeetCode / NeetCode editors:
// no minimap, slim scrollbars, roomy line height, ligature mono font, a bracket-pair colouring and a
// status bar with cursor position. Follows the app light/dark theme.
//
// Monaco is self-hosted: `pnpm copy:monaco` (predev/prebuild) copies node_modules/monaco-editor/min/vs
// to public/monaco/vs, and the loader below points at it, so no CDN or network is needed.
"use client";

import { useEffect, useState } from "react";
import dynamic from "next/dynamic";
import { loader } from "@monaco-editor/react";
import type { BeforeMount, OnMount } from "@monaco-editor/react";
import { useTheme } from "next-themes";
import { Skeleton } from "../primitives/skeleton";

loader.config({ paths: { vs: "/monaco/vs" } });

const MonacoEditor = dynamic(() => import("@monaco-editor/react"), {
  ssr: false,
  loading: () => <Skeleton className="h-full w-full" />,
});

const THEME_DARK = "algoprep-dark";
const THEME_LIGHT = "algoprep-light";

const defineThemes: BeforeMount = (monaco) => {
  monaco.editor.defineTheme(THEME_DARK, {
    base: "vs-dark",
    inherit: true,
    rules: [
      { token: "comment", foreground: "6b7894", fontStyle: "italic" },
      { token: "keyword", foreground: "c792ea" },
      { token: "string", foreground: "a5e075" },
      { token: "number", foreground: "f7a85d" },
      { token: "type", foreground: "5ccfe6" },
    ],
    colors: {
      "editor.background": "#14161c",
      "editor.lineHighlightBackground": "#1d2029",
      "editorLineNumber.foreground": "#4a5265",
      "editorLineNumber.activeForeground": "#aab3c5",
      "editorCursor.foreground": "#ffa116",
      "editor.selectionBackground": "#2f3b57",
      "editorIndentGuide.background1": "#262a35",
      "editorIndentGuide.activeBackground1": "#3a4152",
      "editorGutter.background": "#14161c",
    },
  });
  monaco.editor.defineTheme(THEME_LIGHT, {
    base: "vs",
    inherit: true,
    rules: [
      { token: "comment", foreground: "8a94a6", fontStyle: "italic" },
      { token: "keyword", foreground: "7c3aed" },
      { token: "string", foreground: "15803d" },
      { token: "number", foreground: "c2410c" },
      { token: "type", foreground: "0e7490" },
    ],
    colors: {
      "editor.background": "#fbfbfd",
      "editor.lineHighlightBackground": "#f0f2f7",
      "editorLineNumber.foreground": "#a3acbd",
      "editorLineNumber.activeForeground": "#4b5563",
      "editorCursor.foreground": "#d97706",
      "editor.selectionBackground": "#cfe0ff",
      "editorIndentGuide.background1": "#e5e8ef",
      "editorIndentGuide.activeBackground1": "#c9cfdb",
      "editorGutter.background": "#fbfbfd",
    },
  });
};

export type CodeEditorLanguage = "java" | "cpp" | "python";

const TAB_SIZE = 4;

// The app scales big viewports with CSS `zoom` on <html> (globals.css `--zoom`). Monaco computes mouse
// positions from client coordinates and mis-hits lines when an ancestor is zoomed, so the editor lives in a
// counter-zoomed box (net zoom 1, same on-screen size: percentages are not scaled by zoom), and its
// font/padding are multiplied instead.
function useRootZoom(): number {
  const [zoom, setZoom] = useState(1);
  useEffect(() => {
    const read = () =>
      setZoom(
        Number.parseFloat(getComputedStyle(document.documentElement).zoom) || 1,
      );
    read();
    window.addEventListener("resize", read);
    return () => window.removeEventListener("resize", read);
  }, []);
  return zoom;
}

export function CodeEditor({
  value,
  onChange,
  language,
  label,
  className,
}: {
  value: string;
  onChange: (value: string) => void;
  language: CodeEditorLanguage;
  /** Accessible name of the editor. */
  label: string;
  className?: string;
}) {
  const { resolvedTheme } = useTheme();
  const [cursor, setCursor] = useState({ line: 1, column: 1 });
  const zoom = useRootZoom();

  const onMount: OnMount = (editor) => {
    editor.onDidChangeCursorPosition((event) =>
      setCursor({
        line: event.position.lineNumber,
        column: event.position.column,
      }),
    );
  };

  return (
    <div
      className={`flex min-h-0 flex-col overflow-hidden rounded-lg border border-[var(--color-border)] ${className ?? ""}`}
    >
      <div className="relative min-h-0 flex-1">
        <div
          className="absolute left-0 top-0"
          style={{ zoom: 1 / zoom, width: "100%", height: "100%" }}
        >
          <MonacoEditor
            value={value}
            language={language}
            theme={resolvedTheme === "dark" ? THEME_DARK : THEME_LIGHT}
            beforeMount={defineThemes}
            onMount={onMount}
            onChange={(next) => onChange(next ?? "")}
            options={{
              ariaLabel: label,
              minimap: { enabled: false },
              fontSize: 13.5 * zoom,
              lineHeight: 22 * zoom,
              fontFamily:
                "'JetBrains Mono', 'Fira Code', 'Cascadia Code', Consolas, monospace",
              fontLigatures: true,
              tabSize: TAB_SIZE,
              insertSpaces: true,
              padding: { top: 12 * zoom, bottom: 12 * zoom },
              scrollBeyondLastLine: false,
              smoothScrolling: true,
              cursorSmoothCaretAnimation: "on",
              cursorBlinking: "smooth",
              renderLineHighlight: "line",
              bracketPairColorization: { enabled: true },
              guides: { bracketPairs: true, indentation: true },
              automaticLayout: true,
              overviewRulerLanes: 0,
              hideCursorInOverviewRuler: true,
              overviewRulerBorder: false,
              scrollbar: {
                verticalScrollbarSize: 8 * zoom,
                horizontalScrollbarSize: 8 * zoom,
              },
              lineNumbersMinChars: 3,
              folding: true,
              wordWrap: "off",
              contextmenu: false,
            }}
          />
        </div>
      </div>
      <div className="flex shrink-0 items-center justify-between border-t border-[var(--color-border)] bg-[var(--color-surface-hover)] px-3 py-1 font-mono text-[11px] text-[var(--color-text-muted)]">
        <span>
          Ln {cursor.line}, Col {cursor.column}
        </span>
        <span>Spaces: {TAB_SIZE}</span>
      </div>
    </div>
  );
}
