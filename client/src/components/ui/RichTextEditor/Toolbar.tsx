import React, { useRef, useState } from "react";
import type { Editor } from "@tiptap/react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Bold,
  Italic,
  Underline,
  Strikethrough,
  Heading1,
  Heading2,
  Heading3,
  List,
  ListOrdered,
  Quote,
  Code,
  Code2,
  Undo,
  Redo,
  Table,
  Link,
  Minus,
  AlignLeft,
  AlignCenter,
  AlignRight,
  ImageUp,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import type { UnifiedImageAttrs } from "./extensions/UnifiedImage";

interface ToolbarButtonProps {
  onClick: () => void;
  isActive?: boolean;
  disabled?: boolean;
  title: string;
  children: React.ReactNode;
}

const ToolbarButton: React.FC<ToolbarButtonProps> = ({
  onClick,
  isActive,
  disabled,
  title,
  children,
}) => (
  <Button
    type="button"
    size="sm"
    variant={isActive ? "secondary" : "ghost"}
    className="h-8 w-8 p-0"
    onClick={onClick}
    disabled={disabled}
    title={title}
  >
    {children}
  </Button>
);

const Divider = () => (
  <span className="w-px h-5 bg-border mx-0.5 shrink-0" aria-hidden />
);

interface ToolbarProps {
  editor: Editor;
}

export const Toolbar: React.FC<ToolbarProps> = ({ editor }) => {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [htmlDialogOpen, setHtmlDialogOpen] = useState(false);
  const [htmlDraft, setHtmlDraft] = useState("");

  const insertTable = () => {
    editor
      .chain()
      .focus()
      .insertTable({ rows: 3, cols: 3, withHeaderRow: true })
      .run();
  };

  const insertLink = () => {
    const previous = editor.getAttributes("link").href as string | undefined;
    const url = window.prompt(t("richTextEditor.enterUrl"), previous ?? "");
    if (url === null) return;
    if (!url) {
      editor.chain().focus().extendMarkRange("link").unsetLink().run();
      return;
    }
    editor.chain().focus().extendMarkRange("link").setLink({ href: url }).run();
  };

  const openHtmlDialog = () => {
    setHtmlDraft("");
    setHtmlDialogOpen(true);
  };

  const insertHtmlBlock = () => {
    if (htmlDraft.trim()) {
      editor.chain().focus().insertHtmlBlock(htmlDraft.trim()).run();
    }
    setHtmlDialogOpen(false);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (ev) => {
      const src = ev.target?.result as string;
      const attrs: UnifiedImageAttrs = {
        src,
        alt: file.name.replace(/\.[^.]+$/, ""),
        lhId: "",
        lhPrompt: "",
      };
      editor
        .chain()
        .focus()
        .insertContent({ type: "unifiedImage", attrs })
        .run();
    };
    reader.readAsDataURL(file);

    // Reset so the same file can be re-selected
    e.target.value = "";
  };

  return (
    <div className="flex flex-wrap items-center gap-0.5 p-1.5 border-b bg-muted/20">
      {/* History */}
      <ToolbarButton
        onClick={() => editor.chain().focus().undo().run()}
        disabled={!editor.can().undo()}
        title={t("richTextEditor.undo")}
      >
        <Undo className="h-3.5 w-3.5" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().redo().run()}
        disabled={!editor.can().redo()}
        title={t("richTextEditor.redo")}
      >
        <Redo className="h-3.5 w-3.5" />
      </ToolbarButton>

      <Divider />

      {/* Headings */}
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
        isActive={editor.isActive("heading", { level: 1 })}
        title={t("richTextEditor.heading1")}
      >
        <Heading1 className="h-3.5 w-3.5" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
        isActive={editor.isActive("heading", { level: 2 })}
        title={t("richTextEditor.heading2")}
      >
        <Heading2 className="h-3.5 w-3.5" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
        isActive={editor.isActive("heading", { level: 3 })}
        title={t("richTextEditor.heading3")}
      >
        <Heading3 className="h-3.5 w-3.5" />
      </ToolbarButton>

      <Divider />

      {/* Inline formatting */}
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleBold().run()}
        isActive={editor.isActive("bold")}
        title={t("richTextEditor.bold")}
      >
        <Bold className="h-3.5 w-3.5" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleItalic().run()}
        isActive={editor.isActive("italic")}
        title={t("richTextEditor.italic")}
      >
        <Italic className="h-3.5 w-3.5" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleUnderline().run()}
        isActive={editor.isActive("underline")}
        title={t("richTextEditor.underline")}
      >
        <Underline className="h-3.5 w-3.5" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleStrike().run()}
        isActive={editor.isActive("strike")}
        title={t("richTextEditor.strikethrough")}
      >
        <Strikethrough className="h-3.5 w-3.5" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleCode().run()}
        isActive={editor.isActive("code")}
        title={t("richTextEditor.inlineCode")}
      >
        <Code className="h-3.5 w-3.5" />
      </ToolbarButton>

      <Divider />

      {/* Alignment */}
      <ToolbarButton
        onClick={() => editor.chain().focus().setTextAlign("left").run()}
        isActive={editor.isActive({ textAlign: "left" })}
        title={t("richTextEditor.alignLeft")}
      >
        <AlignLeft className="h-3.5 w-3.5" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().setTextAlign("center").run()}
        isActive={editor.isActive({ textAlign: "center" })}
        title={t("richTextEditor.alignCenter")}
      >
        <AlignCenter className="h-3.5 w-3.5" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().setTextAlign("right").run()}
        isActive={editor.isActive({ textAlign: "right" })}
        title={t("richTextEditor.alignRight")}
      >
        <AlignRight className="h-3.5 w-3.5" />
      </ToolbarButton>

      <Divider />

      {/* Lists */}
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleBulletList().run()}
        isActive={editor.isActive("bulletList")}
        title={t("richTextEditor.bulletList")}
      >
        <List className="h-3.5 w-3.5" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleOrderedList().run()}
        isActive={editor.isActive("orderedList")}
        title={t("richTextEditor.orderedList")}
      >
        <ListOrdered className="h-3.5 w-3.5" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleBlockquote().run()}
        isActive={editor.isActive("blockquote")}
        title={t("richTextEditor.blockquote")}
      >
        <Quote className="h-3.5 w-3.5" />
      </ToolbarButton>

      <Divider />

      {/* Insert */}
      <ToolbarButton
        onClick={insertTable}
        title={t("richTextEditor.insertTable")}
      >
        <Table className="h-3.5 w-3.5" />
      </ToolbarButton>
      <ToolbarButton
        onClick={insertLink}
        isActive={editor.isActive("link")}
        title={t("richTextEditor.insertLink")}
      >
        <Link className="h-3.5 w-3.5" />
      </ToolbarButton>
      <ToolbarButton
        onClick={() => editor.chain().focus().setHorizontalRule().run()}
        title={t("richTextEditor.horizontalRule")}
      >
        <Minus className="h-3.5 w-3.5" />
      </ToolbarButton>

      {/* Image upload */}
      <ToolbarButton
        onClick={() => fileInputRef.current?.click()}
        title={t("richTextEditor.uploadImage")}
      >
        <ImageUp className="h-3.5 w-3.5" />
      </ToolbarButton>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={handleFileChange}
      />

      {/* HTML embed */}
      <ToolbarButton
        onClick={openHtmlDialog}
        title={t("richTextEditor.insertHtml")}
      >
        <Code2 className="h-3.5 w-3.5" />
      </ToolbarButton>

      {/* Insert HTML dialog */}
      <Dialog open={htmlDialogOpen} onOpenChange={setHtmlDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{t("richTextEditor.insertHtmlTitle")}</DialogTitle>
          </DialogHeader>
          <textarea
            value={htmlDraft}
            onChange={(e) => setHtmlDraft(e.target.value)}
            placeholder='<iframe src="..." width="560" height="315"></iframe>'
            className="w-full h-48 font-mono text-sm rounded-md border border-input bg-background px-3 py-2 resize-y focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            spellCheck={false}
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setHtmlDialogOpen(false)}>
              {t("common.cancel")}
            </Button>
            <Button onClick={insertHtmlBlock} disabled={!htmlDraft.trim()}>
              {t("richTextEditor.insertHtmlConfirm")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Table context controls (only visible when cursor is inside a table) */}
      {editor.isActive("table") && (
        <>
          <Divider />
          <Button
            type="button"
            size="sm"
            variant="ghost"
            className="h-8 px-2 text-xs"
            onClick={() => editor.chain().focus().addRowAfter().run()}
            title={t("richTextEditor.addRowAfter")}
          >
            +{t("richTextEditor.row")}
          </Button>
          <Button
            type="button"
            size="sm"
            variant="ghost"
            className="h-8 px-2 text-xs"
            onClick={() => editor.chain().focus().deleteRow().run()}
            title={t("richTextEditor.deleteRow")}
          >
            -{t("richTextEditor.row")}
          </Button>
          <Button
            type="button"
            size="sm"
            variant="ghost"
            className="h-8 px-2 text-xs"
            onClick={() => editor.chain().focus().addColumnAfter().run()}
            title={t("richTextEditor.addColAfter")}
          >
            +{t("richTextEditor.col")}
          </Button>
          <Button
            type="button"
            size="sm"
            variant="ghost"
            className="h-8 px-2 text-xs"
            onClick={() => editor.chain().focus().deleteColumn().run()}
            title={t("richTextEditor.deleteCol")}
          >
            -{t("richTextEditor.col")}
          </Button>
          <Button
            type="button"
            size="sm"
            variant="ghost"
            className="h-8 px-2 text-xs text-destructive hover:text-destructive"
            onClick={() => editor.chain().focus().deleteTable().run()}
            title={t("richTextEditor.deleteTable")}
          >
            ✕{t("richTextEditor.table")}
          </Button>
        </>
      )}
    </div>
  );
};
