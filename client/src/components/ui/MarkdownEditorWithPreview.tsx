import React, { useState, useCallback, useRef, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { FileText, Loader2, Eye, Edit3, Code2 } from "lucide-react";
import { logger } from "@/services/logger";
import { useTranslation } from "react-i18next";

// ─── Base64 Collapse Helpers ─────────────────────────────────────

const COLLAPSE_CHARS = 3 * 80; // ~3 rows at 80 chars/row

type Segment =
  | { type: "text"; content: string }
  | { type: "base64"; prefix: string; data: string; index: number };

function parseSegments(text: string): Segment[] {
  const re = /(data:[^;\s"'`]+;base64,)([A-Za-z0-9+/]+=*)/g;
  const segments: Segment[] = [];
  let lastIndex = 0;
  let idx = 0;
  let match: RegExpExecArray | null;

  while ((match = re.exec(text)) !== null) {
    if (match.index > lastIndex) {
      segments.push({ type: "text", content: text.slice(lastIndex, match.index) });
    }
    segments.push({ type: "base64", prefix: match[1], data: match[2], index: idx++ });
    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < text.length) {
    segments.push({ type: "text", content: text.slice(lastIndex) });
  }

  return segments;
}

function containsBase64(text: string): boolean {
  return /(data:[^;\s"'`]+;base64,)[A-Za-z0-9+/]{50,}/.test(text);
}

// ─── Collapsed Source View ───────────────────────────────────────

const CollapsedSourceView: React.FC<{
  value: string;
  onEditClick: () => void;
}> = ({ value, onEditClick }) => {
  const [expandedSet, setExpandedSet] = useState<Set<number>>(new Set());
  const segments = parseSegments(value);

  const toggle = (idx: number) => {
    setExpandedSet((prev) => {
      const next = new Set(prev);
      if (next.has(idx)) next.delete(idx);
      else next.add(idx);
      return next;
    });
  };

  return (
    <pre
      className="w-full h-full min-h-[400px] overflow-auto rounded-md border border-input bg-background px-3 py-2 text-sm font-mono whitespace-pre-wrap break-all cursor-text select-text"
      onClick={onEditClick}
    >
      {segments.map((seg, i) => {
        if (seg.type === "text") {
          return <span key={i}>{seg.content}</span>;
        }

        const expanded = expandedSet.has(seg.index);
        const isLong = seg.data.length > COLLAPSE_CHARS;
        const shown = !isLong || expanded ? seg.data : seg.data.slice(0, COLLAPSE_CHARS);

        return (
          <span key={i}>
            <span className="text-muted-foreground">{seg.prefix}</span>
            {shown}
            {isLong && (
              <button
                className="text-xs text-primary hover:underline ml-1"
                onClick={(e) => { e.stopPropagation(); toggle(seg.index); }}
              >
                {expanded
                  ? " [collapse]"
                  : ` … [+${(seg.data.length - COLLAPSE_CHARS).toLocaleString()} chars]`}
              </button>
            )}
          </span>
        );
      })}
    </pre>
  );
};

// ─── Types ───────────────────────────────────────────────────────

interface MarkdownEditorWithPreviewProps {
  /** Current markdown content (controlled) */
  value: string;
  /** Callback when the user edits the markdown */
  onChange: (value: string) => void;
  /** Async function that converts markdown to a PDF blob */
  renderPreviewFn: (markdown: string) => Promise<Blob>;
}

// ─── Component ───────────────────────────────────────────────────

export const MarkdownEditorWithPreview: React.FC<
  MarkdownEditorWithPreviewProps
> = ({ value, onChange, renderPreviewFn }) => {
  const { t } = useTranslation();

  // PDF preview state
  const [previewPdfUrl, setPreviewPdfUrl] = useState<string | null>(null);
  const [isRenderingPreview, setIsRenderingPreview] = useState(false);
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);

  // Collapsed source view state
  const [showRaw, setShowRaw] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);

  // Debounce ref for preview rendering
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Cleanup blob URL on unmount or change
  useEffect(() => {
    return () => {
      if (previewPdfUrl) {
        URL.revokeObjectURL(previewPdfUrl);
      }
    };
  }, [previewPdfUrl]);

  // Lock scroll when mobile preview modal is open
  useEffect(() => {
    if (!isPreviewModalOpen) return;

    const html = document.documentElement;
    const appScrollContainer = document.querySelector("main");
    const previousHtmlOverflow = html.style.overflow;
    const previousOverflow = document.body.style.overflow;
    const previousBodyTouchAction = document.body.style.touchAction;
    const previousAppOverflowY =
      appScrollContainer instanceof HTMLElement
        ? appScrollContainer.style.overflowY
        : "";

    html.style.overflow = "hidden";
    document.body.style.overflow = "hidden";
    document.body.style.touchAction = "none";

    if (appScrollContainer instanceof HTMLElement) {
      appScrollContainer.style.overflowY = "hidden";
    }

    return () => {
      html.style.overflow = previousHtmlOverflow;
      document.body.style.overflow = previousOverflow;
      document.body.style.touchAction = previousBodyTouchAction;
      if (appScrollContainer instanceof HTMLElement) {
        appScrollContainer.style.overflowY = previousAppOverflowY;
      }
    };
  }, [isPreviewModalOpen]);

  // ─── Preview Rendering ──────────────────────────────────────────

  const renderPreview = useCallback(
    async (markdown: string) => {
      if (!markdown.trim()) {
        setPreviewPdfUrl(null);
        return;
      }

      setIsRenderingPreview(true);
      try {
        const blob = await renderPreviewFn(markdown);
        const url = URL.createObjectURL(blob);
        setPreviewPdfUrl((prev) => {
          if (prev) URL.revokeObjectURL(prev);
          return url;
        });
      } catch (error) {
        logger.error(
          "Preview render error",
          error,
          "MarkdownEditorWithPreview",
        );
      } finally {
        setIsRenderingPreview(false);
      }
    },
    [renderPreviewFn],
  );

  const debouncedRenderPreview = useCallback(
    (markdown: string) => {
      if (debounceTimer.current) {
        clearTimeout(debounceTimer.current);
      }
      debounceTimer.current = setTimeout(() => {
        renderPreview(markdown);
      }, 800);
    },
    [renderPreview],
  );

  // Auto-render preview when value changes (including on mount)
  useEffect(() => {
    debouncedRenderPreview(value);
    return () => {
      if (debounceTimer.current) {
        clearTimeout(debounceTimer.current);
      }
    };
  }, [value, debouncedRenderPreview]);

  const handleMarkdownChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    onChange(e.target.value);
  };

  // ─── Preview Content ────────────────────────────────────────────

  const previewContent = previewPdfUrl ? (
    <iframe
      src={previewPdfUrl}
      className="w-full h-full min-h-[400px] rounded-md border"
      title={t("markdownEditor.previewTitle")}
    />
  ) : (
    <div className="w-full h-full min-h-[400px] flex items-center justify-center rounded-md border bg-muted/30">
      <div className="text-center text-muted-foreground">
        <FileText className="h-12 w-12 mx-auto mb-2 opacity-50" />
        <p className="text-sm">
          {isRenderingPreview
            ? t("markdownEditor.renderingPreview")
            : t("markdownEditor.editToPreview")}
        </p>
      </div>
    </div>
  );

  const previewSpinner = isRenderingPreview && (
    <Loader2 className="h-3 w-3 animate-spin text-muted-foreground" />
  );

  // ─── Render ─────────────────────────────────────────────────────

  return (
    <>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 h-[calc(100vh-16rem)]">
        {/* Left: Markdown Editor */}
        <Card className="flex flex-col min-h-0">
          <CardHeader className="pb-2 flex-shrink-0">
            <CardTitle className="flex items-center gap-2 text-base">
              <Edit3 className="h-4 w-4" />
              {t("markdownEditor.editorTitle")}
              {containsBase64(value) && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="ml-auto h-6 px-2 text-xs text-muted-foreground"
                  onClick={() => {
                    setShowRaw((prev) => {
                      if (prev) return false; // switching to collapsed — nothing extra needed
                      // switching to raw — focus textarea after render
                      setTimeout(() => textareaRef.current?.focus(), 0);
                      return true;
                    });
                  }}
                  title={showRaw ? t("markdownEditor.collapseBase64") : t("markdownEditor.editRaw")}
                >
                  <Code2 className="h-3.5 w-3.5 mr-1" />
                  {showRaw ? t("markdownEditor.collapseBase64") : t("markdownEditor.editRaw")}
                </Button>
              )}
            </CardTitle>
          </CardHeader>
          <CardContent className="flex-1 min-h-0 pb-4">
            {containsBase64(value) && !showRaw ? (
              <CollapsedSourceView
                value={value}
                onEditClick={() => {
                  setShowRaw(true);
                  setTimeout(() => textareaRef.current?.focus(), 0);
                }}
              />
            ) : (
              <textarea
                ref={textareaRef}
                value={value}
                onChange={handleMarkdownChange}
                className="w-full h-full min-h-[400px] resize-none rounded-md border border-input bg-background px-3 py-2 text-sm font-mono ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                placeholder="# Artikulationsschema&#10;&#10;**Thema:** ...&#10;&#10;| Zeit | Phase | Handlungsschritte | Sozialform | Kompetenzen | Medien/Material |&#10;|------|-------|-------------------|------------|-------------|-----------------|&#10;| 5 min | Einstieg | ... | Plenum | ... | ... |"
              />
            )}
          </CardContent>
        </Card>

        {/* Right: PDF Preview (desktop only) */}
        <Card className="hidden lg:flex flex-col min-h-0">
          <CardHeader className="pb-2 flex-shrink-0">
            <CardTitle className="flex items-center gap-2 text-base">
              <Eye className="h-4 w-4" />
              {t("markdownEditor.pdfPreview")}
              {previewSpinner}
            </CardTitle>
          </CardHeader>
          <CardContent className="flex-1 min-h-0 pb-4">
            {previewContent}
          </CardContent>
        </Card>
      </div>

      {/* Mobile: Render Preview button */}
      <div className="lg:hidden mt-4">
        <Button
          variant="outline"
          className="w-full"
          onClick={() => {
            if (!previewPdfUrl && value) {
              debouncedRenderPreview(value);
            }
            setIsPreviewModalOpen(true);
          }}
          disabled={!value}
        >
          <Eye className="h-4 w-4 mr-2" />
          {t("markdownEditor.renderPreview")}
        </Button>
      </div>

      {/* Mobile: PDF Preview Dialog */}
      <Dialog open={isPreviewModalOpen} onOpenChange={setIsPreviewModalOpen}>
        <DialogContent className="w-[calc(100vw-1rem)] max-w-[calc(100vw-1rem)] h-[85vh] overflow-x-hidden flex flex-col p-0">
          <DialogHeader className="px-6 pt-6 pb-2 flex-shrink-0">
            <DialogTitle className="flex items-center gap-2">
              <Eye className="h-4 w-4" />
              {t("markdownEditor.pdfPreview")}
              {previewSpinner}
            </DialogTitle>
          </DialogHeader>
          <div className="flex-1 min-h-0 min-w-0 px-6 pb-6">
            {previewContent}
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
};
