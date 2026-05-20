import React, { useEffect, useRef } from "react";
import { useEditor, EditorContent } from "@tiptap/react";
import { StarterKit } from "@tiptap/starter-kit";
import {
  Table,
  TableRow,
  TableCell,
  TableHeader,
} from "@tiptap/extension-table";
import { TextAlign } from "@tiptap/extension-text-align";
import { Underline } from "@tiptap/extension-underline";
import { Link } from "@tiptap/extension-link";
import { Toolbar } from "./Toolbar";
import { UnifiedImage } from "./extensions/UnifiedImage";
import { HtmlBlock } from "./extensions/HtmlBlock";
import { RichTextEditorContext } from "./context";
import { markdownToTiptapHtml, tiptapHtmlToMarkdown } from "@/utils/markdownTiptap";
import type { RegenerateImageParams } from "@/components/ui/MarkdownEditorWithPreview";

interface RichTextEditorProps {
  value: string;
  onChange: (markdown: string) => void;
  onRegenerateImage?: (params: RegenerateImageParams) => Promise<string>;
}

export const RichTextEditor: React.FC<RichTextEditorProps> = ({
  value,
  onChange,
  onRegenerateImage,
}) => {
  // Track whether the current content change is external (prop change)
  // vs internal (user typing) to avoid cursor-jump feedback loops.
  const isExternalUpdate = useRef(false);
  const lastEmittedMarkdown = useRef<string>("");

  const editor = useEditor({
    extensions: [
      StarterKit,
      // Single image extension: handles both AI-annotated and uploaded images
      UnifiedImage,
      HtmlBlock,
      Table.configure({ resizable: false }),
      TableRow,
      TableCell,
      TableHeader,
      TextAlign.configure({
        types: ["heading", "paragraph"],
      }),
      Underline,
      Link.configure({
        openOnClick: false,
        HTMLAttributes: { rel: "noopener noreferrer" },
      }),
    ],
    content: markdownToTiptapHtml(value),
    onUpdate({ editor: ed }) {
      if (isExternalUpdate.current) return;
      const html = ed.getHTML();
      const md = tiptapHtmlToMarkdown(html);
      if (md !== lastEmittedMarkdown.current) {
        lastEmittedMarkdown.current = md;
        onChange(md);
      }
    },
    editorProps: {
      attributes: {
        class:
          "prose prose-sm max-w-none focus:outline-none min-h-[400px] px-4 py-3",
      },
    },
  });

  // Sync when the parent changes the value (e.g. AI generation replaces content)
  useEffect(() => {
    if (!editor) return;
    const currentHtml = editor.getHTML();
    const currentMd = tiptapHtmlToMarkdown(currentHtml);
    if (value !== currentMd && value !== lastEmittedMarkdown.current) {
      isExternalUpdate.current = true;
      editor.commands.setContent(markdownToTiptapHtml(value));
      isExternalUpdate.current = false;
    }
  }, [editor, value]);

  return (
    <RichTextEditorContext.Provider value={{ onRegenerateImage }}>
      <div className="flex flex-col h-full border rounded-md overflow-hidden">
        {editor && <Toolbar editor={editor} />}
        <div className="flex-1 overflow-y-auto">
          <EditorContent editor={editor} className="h-full" />
        </div>
      </div>
    </RichTextEditorContext.Provider>
  );
};
