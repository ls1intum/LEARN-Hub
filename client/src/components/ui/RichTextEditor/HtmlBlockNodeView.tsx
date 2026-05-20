import React, { useState } from "react";
import { NodeViewWrapper } from "@tiptap/react";
import type { NodeViewProps } from "@tiptap/react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Code2, Edit3, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";

export const HtmlBlockNodeView: React.FC<NodeViewProps> = ({
  node,
  updateAttributes,
  deleteNode,
}) => {
  const { t } = useTranslation();
  const content = (node.attrs.content as string) ?? "";
  const [isEditing, setIsEditing] = useState(false);
  const [draft, setDraft] = useState("");

  const openEdit = () => {
    setDraft(content);
    setIsEditing(true);
  };

  const saveEdit = () => {
    updateAttributes({ content: draft });
    setIsEditing(false);
  };

  return (
    <NodeViewWrapper className="my-3">
      <div className="rounded-md border overflow-hidden">
        {/* Rendered HTML preview */}
        <div
          className="p-3 bg-white min-h-[40px] overflow-auto"
          // biome-ignore lint/security/noDangerouslySetInnerHtml: user-authored content in editor
          dangerouslySetInnerHTML={{ __html: content }}
        />

        {/* Bottom toolbar */}
        <div className="flex items-center gap-2 px-3 py-1.5 bg-muted/40 border-t text-xs">
          <Code2 className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
          <span className="text-muted-foreground flex-1 font-mono truncate text-[11px]">
            {content.length > 60
              ? content.slice(0, 60) + "…"
              : content || "HTML"}
          </span>
          <Button
            size="sm"
            variant="ghost"
            className="h-7 px-2 text-xs"
            onClick={openEdit}
            title={t("richTextEditor.editHtml")}
          >
            <Edit3 className="h-3.5 w-3.5" />
          </Button>
          <Button
            size="sm"
            variant="ghost"
            className="h-7 px-2 text-xs text-destructive hover:text-destructive"
            onClick={deleteNode}
            title={t("richTextEditor.deleteHtmlBlock")}
          >
            <Trash2 className="h-3.5 w-3.5" />
          </Button>
        </div>
      </div>

      {/* Edit dialog */}
      <Dialog open={isEditing} onOpenChange={setIsEditing}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{t("richTextEditor.editHtmlTitle")}</DialogTitle>
          </DialogHeader>
          <textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            className="w-full h-48 font-mono text-sm rounded-md border border-input bg-background px-3 py-2 resize-y focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            spellCheck={false}
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsEditing(false)}>
              {t("common.cancel")}
            </Button>
            <Button onClick={saveEdit}>{t("common.save")}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </NodeViewWrapper>
  );
};
