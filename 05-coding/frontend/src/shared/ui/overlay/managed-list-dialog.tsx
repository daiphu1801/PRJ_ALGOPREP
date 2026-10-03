// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Quản lý ..." dialog for an ADMIN-managed category list (interview topics, problem topics —
// DEC-2026-1001-admin-configurable-settings): add, rename, delete, with no cap on how many. An item
// still in use cannot be deleted: its row shows the count and the delete control is disabled, which
// is what the BD's refusal looks like from the screen. Text arrives through `labels` so this stays
// free of any one screen's i18n namespace.
"use client";

import { useState, type ReactNode } from "react";
import { Check, Plus, Trash2 } from "lucide-react";
import type { ManagedItem, ManagedListError } from "@/shared/lib/managed-list-store";
import { toast } from "@/shared/lib/toast-store";
import { Button } from "../primitives/button";
import { IconAction } from "../primitives/icon-action";
import { Modal } from "./modal";
import { TextField } from "../form/text-field";

export type ManagedListLabels = {
  title: string;
  hint: string;
  nameLabel: string;
  newLabel: string;
  newPlaceholder: string;
  add: string;
  save: string;
  delete: string;
  close: string;
  usage: (count: number) => string;
  deleteBlocked: (count: number) => string;
  error: Record<ManagedListError, string>;
  /** Success toasts, one per action. */
  done: { add: string; rename: string; remove: string };
};

type Props<T extends ManagedItem> = {
  open: boolean;
  onClose: () => void;
  items: readonly T[];
  /** Records using each item, by key. */
  usage: Readonly<Record<string, number>>;
  labels: ManagedListLabels;
  onAdd: (label: string) => ManagedListError | null;
  onRename: (key: string, label: string) => ManagedListError | null;
  onRemove: (key: string) => void;
  /** Extra per-row control, e.g. a flag toggle. */
  renderExtra?: (item: T) => ReactNode;
};

export function ManagedListDialog<T extends ManagedItem>({
  open,
  onClose,
  items,
  usage,
  labels,
  onAdd,
  onRename,
  onRemove,
  renderExtra,
}: Props<T>) {
  const [draftNames, setDraftNames] = useState<Record<string, string>>({});
  const [newName, setNewName] = useState("");
  // Which input last failed: it keeps a red border, the message itself is a toast.
  const [invalidWhere, setInvalidWhere] = useState<string | null>(null);

  function report(where: string, result: ManagedListError | null, done: string) {
    setInvalidWhere(result ? where : null);
    if (result) toast.error(labels.error[result]);
    else toast.success(done);
    return result === null;
  }

  function add() {
    if (report("new", onAdd(newName), labels.done.add)) setNewName("");
  }

  function commitRename(key: string) {
    const draft = draftNames[key];
    if (draft === undefined) return;
    if (report(key, onRename(key, draft), labels.done.rename)) {
      setDraftNames(({ [key]: _discarded, ...rest }) => rest);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={labels.title}
      footer={
        <Button
          variant="ghost"
          size="sm"
          className="border border-[var(--color-border)]"
          onClick={onClose}
        >
          {labels.close}
        </Button>
      }
    >
      <p className="mb-3 text-[12.5px] text-[var(--color-text-muted)]">{labels.hint}</p>

      <ul className="mb-4 flex flex-col gap-2.5">
        {items.map((item) => {
          const used = usage[item.key] ?? 0;
          return (
            <li key={item.key}>
              <div className="flex items-start gap-2">
                <TextField
                  label={labels.nameLabel}
                  hideLabel
                  value={draftNames[item.key] ?? item.label}
                  onChange={(event) =>
                    setDraftNames((previous) => ({ ...previous, [item.key]: event.target.value }))
                  }
                  onKeyDown={(event) => {
                    if (event.key === "Enter") commitRename(item.key);
                  }}
                  invalid={invalidWhere === item.key}
                  wrapperClassName="flex-1"
                />
                {renderExtra ? <div className="mt-1.5 shrink-0">{renderExtra(item)}</div> : null}
                <span className="mt-2 w-20 shrink-0 text-right font-mono text-xs text-[var(--color-text-muted)]">
                  {labels.usage(used)}
                </span>
                <IconAction
                  icon={Check}
                  label={labels.save}
                  onClick={() => commitRename(item.key)}
                  className={draftNames[item.key] === undefined ? "invisible" : undefined}
                />
                <IconAction
                  icon={Trash2}
                  label={used > 0 ? labels.deleteBlocked(used) : labels.delete}
                  tone="danger"
                  disabled={used > 0}
                  onClick={() => {
                    onRemove(item.key);
                    toast.success(labels.done.remove);
                  }}
                />
              </div>
            </li>
          );
        })}
      </ul>

      <div className="flex items-start gap-2">
        <TextField
          label={labels.newLabel}
          placeholder={labels.newPlaceholder}
          value={newName}
          onChange={(event) => setNewName(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") add();
          }}
          invalid={invalidWhere === "new"}
          wrapperClassName="flex-1"
        />
        <Button variant="cta" size="sm" onClick={add} className="mt-6 gap-1">
          <Plus aria-hidden="true" className="h-4 w-4" />
          {labels.add}
        </Button>
      </div>
    </Modal>
  );
}
