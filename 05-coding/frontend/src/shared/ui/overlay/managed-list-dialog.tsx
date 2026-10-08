// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Quản lý ..." dialog for an ADMIN-managed category list (interview topics, problem topics —
// DEC-2026-1001-admin-configurable-settings): add, rename, delete, with no cap on how many. An item
// still in use cannot be deleted: its row shows the count and the delete control is disabled, which
// is what the BD's refusal looks like from the screen. Text arrives through `labels` so this stays
// free of any one screen's i18n namespace.
"use client";

import { useState, type ReactNode } from "react";
import { ArrowDown, ArrowUp, Check, Plus, Trash2 } from "lucide-react";
import type {
  ManagedItem,
  ManagedListError,
} from "@/shared/lib/managed-list-store";
import { cn } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import { Button } from "../primitives/button";
import { IconAction } from "../primitives/icon-action";
import { Modal } from "./modal";
import { TextField } from "../form/text-field";

// From this many items the list stops growing and scrolls. One row is h-9 (36px) with a 10px gap, so the
// cap shows SCROLL_FROM - 1 rows plus the 6px padding that keeps the focus ring from being clipped.
const SCROLL_FROM = 6;
const SCROLL_MAX_HEIGHT = `${((SCROLL_FROM - 1) * 36 + (SCROLL_FROM - 2) * 10 + 12) / 16}rem`;

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
  /** Tooltip of the delete control while the list is down to `keepAtLeast` items. */
  deleteLast?: string;
  /** Required when `onMove` is given. */
  moveUp?: string;
  moveDown?: string;
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
  /** Adds up/down controls to every row when given. */
  onMove?: (key: string, delta: -1 | 1) => void;
  /** The list may never shrink below this many items; delete is disabled at the floor. Default 0. */
  keepAtLeast?: number;
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
  onMove,
  keepAtLeast = 0,
  renderExtra,
}: Props<T>) {
  const [draftNames, setDraftNames] = useState<Record<string, string>>({});
  const [newName, setNewName] = useState("");
  // Which input last failed: it keeps a red border, the message itself is a toast.
  const [invalidWhere, setInvalidWhere] = useState<string | null>(null);

  function report(
    where: string,
    result: ManagedListError | null,
    done: string,
  ) {
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
      <p className="mb-3 text-[12.5px] text-[var(--color-text-muted)]">
        {labels.hint}
      </p>

      <ul
        className={cn(
          "mb-4 flex flex-col gap-2.5",
          // The padding and negative margin leave room for the focus ring, which overflow would clip.
          items.length >= SCROLL_FROM &&
            "-mx-1.5 mb-2.5 scrollbar-glass overflow-y-auto overscroll-contain px-1.5 py-1.5",
        )}
        style={
          items.length >= SCROLL_FROM
            ? { maxHeight: SCROLL_MAX_HEIGHT }
            : undefined
        }
      >
        {items.map((item, index) => {
          const used = usage[item.key] ?? 0;
          const atFloor = items.length <= keepAtLeast;
          return (
            <li key={item.key}>
              <div className="flex items-start gap-2">
                <TextField
                  label={labels.nameLabel}
                  hideLabel
                  value={draftNames[item.key] ?? item.label}
                  onChange={(event) =>
                    setDraftNames((previous) => ({
                      ...previous,
                      [item.key]: event.target.value,
                    }))
                  }
                  onKeyDown={(event) => {
                    if (event.key === "Enter") commitRename(item.key);
                  }}
                  invalid={invalidWhere === item.key}
                  wrapperClassName="flex-1"
                />
                {onMove ? (
                  <>
                    <IconAction
                      icon={ArrowUp}
                      label={labels.moveUp ?? ""}
                      disabled={index === 0}
                      onClick={() => onMove(item.key, -1)}
                    />
                    <IconAction
                      icon={ArrowDown}
                      label={labels.moveDown ?? ""}
                      disabled={index === items.length - 1}
                      onClick={() => onMove(item.key, 1)}
                    />
                  </>
                ) : null}
                {renderExtra ? (
                  <div className="mt-1.5 shrink-0">{renderExtra(item)}</div>
                ) : null}
                <span className="mt-2 w-20 shrink-0 text-right font-mono text-xs text-[var(--color-text-muted)]">
                  {labels.usage(used)}
                </span>
                <IconAction
                  icon={Check}
                  label={labels.save}
                  onClick={() => commitRename(item.key)}
                  className={
                    draftNames[item.key] === undefined ? "invisible" : undefined
                  }
                />
                <IconAction
                  icon={Trash2}
                  label={
                    used > 0
                      ? labels.deleteBlocked(used)
                      : atFloor
                        ? (labels.deleteLast ?? labels.delete)
                        : labels.delete
                  }
                  tone="danger"
                  disabled={used > 0 || atFloor}
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
