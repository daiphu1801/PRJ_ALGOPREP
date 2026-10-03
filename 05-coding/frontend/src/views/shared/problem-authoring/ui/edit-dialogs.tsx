// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Add/edit forms for one testcase and one worked example (SHR0202, tab Testcase and tab Ví dụ mẫu).
// Mounted only while open, so each open starts from the row's own values — no reset effect needed.
"use client";

import { useState } from "react";
import type { Testcase, TestcaseVisibility, WorkedExample } from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { Button, Modal, SelectField, TextArea } from "@/shared/ui";

type FormShellProps = {
  title: string;
  onClose: () => void;
  onSave: () => void;
  children: React.ReactNode;
};

function FormShell({ title, onClose, onSave, children }: FormShellProps) {
  const t = useT("problemAuthoring");
  return (
    <Modal
      open
      onClose={onClose}
      title={title}
      className="max-w-lg"
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose}>
            {t("dialog.cancel")}
          </Button>
          <Button size="sm" onClick={onSave}>
            {t("dialog.save")}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        {children}
      </div>
    </Modal>
  );
}

type TestcaseDialogProps = {
  /** Undefined = adding a new row. */
  testcase?: Testcase;
  onClose: () => void;
  onSave: (values: Pick<Testcase, "input" | "expected" | "visibility">) => void;
};

export function TestcaseDialog({ testcase, onClose, onSave }: TestcaseDialogProps) {
  const t = useT("problemAuthoring");
  const [input, setInput] = useState(testcase?.input ?? "");
  const [expected, setExpected] = useState(testcase?.expected ?? "");
  const [visibility, setVisibility] = useState<TestcaseVisibility>(testcase?.visibility ?? "hidden");
  const [touched, setTouched] = useState(false);
  const invalid = !input.trim() || !expected.trim();

  function submit() {
    setTouched(true);
    if (invalid) {
      toast.error(t("dialog.required"));
      return;
    }
    onSave({ input: input.trim(), expected: expected.trim(), visibility });
  }

  return (
    <FormShell
      title={testcase ? t("dialog.editTestcase") : t("dialog.addTestcase")}
      onClose={onClose}
      onSave={submit}
    >
      <TextArea
        label={t("columnInput")}
        rows={3}
        value={input}
        invalid={touched && !input.trim()}
        onChange={(event) => setInput(event.target.value)}
        className="font-mono text-[12.5px]"
      />
      <TextArea
        label={t("columnExpected")}
        rows={3}
        value={expected}
        invalid={touched && !expected.trim()}
        onChange={(event) => setExpected(event.target.value)}
        className="font-mono text-[12.5px]"
      />
      <SelectField
        label={t("columnVisibility")}
        value={visibility}
        onChange={(event) => setVisibility(event.target.value as TestcaseVisibility)}
        options={[
          { value: "public", label: t("visibility.public") },
          { value: "hidden", label: t("visibility.hidden") },
        ]}
      />
    </FormShell>
  );
}

type ExampleDialogProps = {
  example?: WorkedExample;
  onClose: () => void;
  onSave: (values: Pick<WorkedExample, "input" | "output" | "explanation">) => void;
};

export function ExampleDialog({ example, onClose, onSave }: ExampleDialogProps) {
  const t = useT("problemAuthoring");
  const [input, setInput] = useState(example?.input ?? "");
  const [output, setOutput] = useState(example?.output ?? "");
  const [explanation, setExplanation] = useState(example?.explanation ?? "");
  const [touched, setTouched] = useState(false);
  // Explanation is optional; input and output are what make it an example.
  const invalid = !input.trim() || !output.trim();

  function submit() {
    setTouched(true);
    if (invalid) {
      toast.error(t("dialog.required"));
      return;
    }
    onSave({ input: input.trim(), output: output.trim(), explanation: explanation.trim() });
  }

  return (
    <FormShell
      title={example ? t("dialog.editExample") : t("dialog.addExample")}
      onClose={onClose}
      onSave={submit}
    >
      <TextArea
        label={t("exampleInput")}
        rows={2}
        value={input}
        invalid={touched && !input.trim()}
        onChange={(event) => setInput(event.target.value)}
        className="font-mono text-[12.5px]"
      />
      <TextArea
        label={t("exampleOutput")}
        rows={2}
        value={output}
        invalid={touched && !output.trim()}
        onChange={(event) => setOutput(event.target.value)}
        className="font-mono text-[12.5px]"
      />
      <TextArea
        label={t("exampleExplanation")}
        rows={3}
        value={explanation}
        onChange={(event) => setExplanation(event.target.value)}
      />
    </FormShell>
  );
}
