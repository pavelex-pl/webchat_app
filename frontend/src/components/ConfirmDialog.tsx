import { ReactNode } from "react";
import Modal from "./Modal";

export default function ConfirmDialog({
  title,
  message,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  danger = false,
  onConfirm,
  onCancel,
}: {
  title: string;
  message: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <Modal title={title} onClose={onCancel}>
      <div className="text-sm text-slate-700 mb-4">{message}</div>
      <div className="flex justify-end gap-2">
        <button
          onClick={onCancel}
          className="px-3 py-1.5 border rounded text-sm text-slate-700 hover:bg-slate-50">
          {cancelLabel}
        </button>
        <button
          onClick={onConfirm}
          className={`px-3 py-1.5 rounded text-sm text-white ${
            danger ? "bg-red-600 hover:bg-red-700" : "bg-slate-800 hover:bg-slate-900"
          }`}>
          {confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
