// ADM0303 price table popup (F5 #23 to #26): the shared ManagedListDialog plus one price box per row.
// The box writes when the admin leaves it or presses Enter, not on every keystroke: each write is one
// PATCH, and the server answer (not the typed text) is what the row shows afterwards.
"use client";

import { useState } from "react";
import { useModelPriceActions, type ModelPrice } from "@/entities/ai-budget";
import { useT } from "@/shared/i18n";
import { ManagedListDialog, TextField } from "@/shared/ui";

type Actions = ReturnType<typeof useModelPriceActions>;

function PriceInput({ item, actions }: { item: ModelPrice; actions: Actions }) {
  const t = useT("adminAiLimits");
  // null while the box is not being edited, so it follows the server value.
  const [draft, setDraft] = useState<string | null>(null);

  async function commit() {
    if (draft === null) return;
    // BD Q4: a negative or non-numeric entry becomes 0 instead of an error.
    const parsed = Number(draft);
    const value = Number.isFinite(parsed) ? Math.max(0, parsed) : 0;
    if (value !== item.usdPerMillionTokens)
      await actions.setPrice(item.key, value);
    setDraft(null);
  }

  return (
    <TextField
      label={t("cost.dialog.priceLabel", { name: item.label })}
      hideLabel
      type="number"
      min={0}
      step="0.1"
      value={draft ?? String(item.usdPerMillionTokens)}
      onChange={(event) => setDraft(event.target.value)}
      onBlur={() => void commit()}
      onKeyDown={(event) => {
        if (event.key === "Enter") void commit();
      }}
      wrapperClassName="w-24"
    />
  );
}

export function ModelPriceDialog({
  open,
  onClose,
  items,
}: {
  open: boolean;
  onClose: () => void;
  items: readonly ModelPrice[];
}) {
  const t = useT("adminAiLimits");
  const actions = useModelPriceActions();

  return (
    <ManagedListDialog
      open={open}
      onClose={onClose}
      items={items}
      usage={{}}
      onAdd={actions.add}
      onRename={actions.rename}
      onRemove={actions.remove}
      renderExtra={(item) => <PriceInput item={item} actions={actions} />}
      labels={{
        title: t("cost.dialog.title"),
        hint: t("cost.dialog.hint"),
        nameLabel: t("cost.dialog.nameLabel"),
        newLabel: t("cost.dialog.newLabel"),
        newPlaceholder: t("cost.dialog.newPlaceholder"),
        add: t("cost.dialog.add"),
        save: t("cost.dialog.save"),
        delete: t("cost.dialog.delete"),
        close: t("cost.dialog.close"),
        usage: () => "",
        deleteBlocked: () => "",
        error: {
          empty: t("cost.dialog.errorEmpty"),
          duplicate: t("cost.dialog.errorDuplicate"),
        },
        done: {
          add: t("cost.dialog.doneAdd"),
          rename: t("cost.dialog.doneRename"),
          remove: t("cost.dialog.doneRemove"),
        },
      }}
    />
  );
}
