import { createContext } from "react";
import type { RegenerateImageParams } from "@/components/ui/MarkdownEditorWithPreview";

export interface RichTextEditorContextValue {
  onRegenerateImage?: (params: RegenerateImageParams) => Promise<string>;
}

export const RichTextEditorContext =
  createContext<RichTextEditorContextValue | null>(null);
