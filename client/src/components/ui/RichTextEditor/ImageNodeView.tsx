import React, { useContext, useState } from "react";
import { NodeViewWrapper } from "@tiptap/react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Loader2, RefreshCw, Trash2, ChevronUp } from "lucide-react";
import { useTranslation } from "react-i18next";
import { RichTextEditorContext } from "./context";
import type { NodeViewProps } from "@tiptap/react";

export const ImageNodeView: React.FC<NodeViewProps> = ({
  node,
  updateAttributes,
  deleteNode,
}) => {
  const { t } = useTranslation();
  const ctx = useContext(RichTextEditorContext);
  const [customPrompt, setCustomPrompt] = useState("");
  const [isRegenerating, setIsRegenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [panelOpen, setPanelOpen] = useState(false);

  const { src, alt, lhId, lhPrompt } = node.attrs as {
    src: string;
    alt: string;
    lhId: string;
    lhPrompt: string;
  };

  // Only AI-generated images (lhId set) can be regenerated
  const canRegenerate = !!lhId && !!ctx?.onRegenerateImage;

  const handleRegenerate = async () => {
    if (!ctx?.onRegenerateImage) return;
    setError(null);
    setIsRegenerating(true);
    try {
      const newBlock = await ctx.onRegenerateImage({
        imageId: lhId,
        description: lhPrompt,
        customPrompt,
      });
      // newBlock is the full markdown: <!-- annotation --> \n ![alt](data:...)
      // Extract src from it
      const srcMatch = /!\[[^\]]*\]\((data:[^)]+)\)/.exec(newBlock);
      const annotationMatch =
        /<!--\s*learnhub-image:id=([^;]*?);\s*prompt=([\s\S]*?)\s*-->/.exec(
          newBlock,
        );
      if (srcMatch) {
        updateAttributes({
          src: srcMatch[1],
          lhId: annotationMatch?.[1]?.trim() ?? lhId,
          lhPrompt: annotationMatch?.[2]?.trim() ?? lhPrompt,
        });
      }
    } catch {
      setError(t("markdownEditor.imageRegenError"));
    } finally {
      setIsRegenerating(false);
    }
  };

  return (
    <NodeViewWrapper className="my-4 rounded-md border overflow-hidden">
      {/* Image */}
      <img
        src={src}
        alt={alt}
        className="w-full h-auto block"
        draggable={false}
      />

      {/* Toolbar row */}
      <div className="flex items-center gap-2 px-3 py-1.5 bg-muted/40 border-t text-xs">
        <span className="text-muted-foreground truncate flex-1">
          {lhId || alt}
        </span>
        {canRegenerate && (
          <Button
            size="sm"
            variant="ghost"
            className="h-7 px-2 text-xs"
            onClick={() => setPanelOpen((v) => !v)}
            title={panelOpen ? undefined : "Regenerate image"}
          >
            {panelOpen ? (
              <ChevronUp className="h-3.5 w-3.5" />
            ) : (
              <RefreshCw className="h-3.5 w-3.5" />
            )}
          </Button>
        )}
        <Button
          size="sm"
          variant="ghost"
          className="h-7 px-2 text-xs text-destructive hover:text-destructive"
          onClick={deleteNode}
          title="Remove image"
        >
          <Trash2 className="h-3.5 w-3.5" />
        </Button>
      </div>

      {/* Regenerate panel — only for AI-generated images */}
      {panelOpen && canRegenerate && (
        <div className="flex flex-col gap-1.5 px-3 py-2 bg-muted/20 border-t">
          {lhPrompt && (
            <p className="text-xs text-muted-foreground">
              {t("markdownEditor.originalPrompt")}: {lhPrompt}
            </p>
          )}
          <div className="flex gap-2">
            <Input
              value={customPrompt}
              onChange={(e) => setCustomPrompt(e.target.value)}
              placeholder={t("markdownEditor.imagePromptPlaceholder")}
              className="h-8 text-xs flex-1"
              disabled={isRegenerating}
              onKeyDown={(e) => {
                if (e.key === "Enter") void handleRegenerate();
              }}
            />
            <Button
              size="sm"
              variant="secondary"
              className="h-8 text-xs shrink-0"
              onClick={() => void handleRegenerate()}
              disabled={isRegenerating}
            >
              {isRegenerating ? (
                <Loader2 className="h-3.5 w-3.5 mr-1 animate-spin" />
              ) : (
                <RefreshCw className="h-3.5 w-3.5 mr-1" />
              )}
              {t("markdownEditor.regenerateImage")}
            </Button>
          </div>
          {error && <p className="text-xs text-destructive">{error}</p>}
        </div>
      )}
    </NodeViewWrapper>
  );
};
