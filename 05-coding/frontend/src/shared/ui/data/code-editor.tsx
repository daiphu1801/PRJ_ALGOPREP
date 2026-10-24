// PROTOTYPE - no DD yet. See 06-plan/PROTOTYPE_DEBT.md.
//
// Monaco-based code editor (README.md mục 5 locks Monaco). Styled like the LeetCode / NeetCode editors:
// no minimap, slim scrollbars, roomy line height, ligature mono font, a bracket-pair colouring and a
// status bar with cursor position. Follows the app light/dark theme.
//
// ponytail: Monaco's loader pulls the editor from the jsDelivr CDN at runtime, so the screen needs
// network. Self-host it (loader.config({ paths })) before an offline demo.
"use client";

import { useState } from "react";
import dynamic from "next/dynamic";
import type { BeforeMount, OnMount } from "@monaco-editor/react";
import { useTheme } from "next-themes";
import { Skeleton } from "../primitives/skeleton";

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
      <div className="min-h-0 flex-1">
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
            fontSize: 13.5,
            lineHeight: 22,
            fontFamily:
              "'JetBrains Mono', 'Fira Code', 'Cascadia Code', Consolas, monospace",
            fontLigatures: true,
            tabSize: TAB_SIZE,
            insertSpaces: true,
            padding: { top: 12, bottom: 12 },
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
              verticalScrollbarSize: 8,
              horizontalScrollbarSize: 8,
            },
            lineNumbersMinChars: 3,
            folding: true,
            wordWrap: "off",
            contextmenu: false,
          }}
        />
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
