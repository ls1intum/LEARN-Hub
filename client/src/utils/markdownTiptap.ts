import { marked } from "marked";
import TurndownService from "turndown";
// @ts-expect-error - no typings shipped with the plugin
import { gfm } from "turndown-plugin-gfm";

// ─── Learnhub image annotation regex (same shape as MarkdownEditorWithPreview)

const IMAGE_BLOCK_RE =
  /(?:<!--\s*learnhub-image:id=([^;]*?);\s*prompt=([\s\S]*?)\s*-->\s*)?!\[([^\]]*)\]\((data:[^;\s"'`]+;base64,[A-Za-z0-9+/]+=*)\)/g;

/**
 * Convert markdown (potentially containing learnhub-image annotations) to HTML
 * suitable for loading into the TipTap editor.
 *
 * Learnhub annotations are folded into the <img> element as data-lh-* attributes
 * so they survive the TipTap round-trip.
 */
export function markdownToTiptapHtml(markdown: string): string {
  IMAGE_BLOCK_RE.lastIndex = 0;

  // Replace every annotated (or plain) markdown image with an <img> tag that
  // carries the annotation data as attributes.  marked will treat them as
  // pass-through HTML, so the attributes reach TipTap intact.
  const processed = markdown.replace(
    IMAGE_BLOCK_RE,
    (_match, id, prompt, alt, src) => {
      const lhId =
        (
          (id as string | undefined) ??
          (alt as string | undefined) ??
          ""
        ).trim() || "image";
      const lhPrompt = ((prompt as string | undefined) ?? "").trim();
      return `<img src="${src}" alt="${alt || lhId}" data-lh="true" data-lh-id="${lhId}" data-lh-prompt="${encodeURIComponent(lhPrompt)}">`;
    },
  );

  return marked.parse(processed, {
    walkTokens(token) {
      if (token.type === "html") {
        const raw = (token as { type: "html"; text: string }).text.trim();
        // Bare <img> elements come from the image pre-processor and must reach
        // the UnifiedImage extension — skip wrapping them as HTML blocks.
        if (!raw || /^<img[\s>]/i.test(raw)) return;
        (token as { type: "html"; text: string }).text =
          `<div data-html-block="true" data-html-content="${encodeURIComponent(raw)}">${raw}</div>\n`;
      }
    },
  }) as string;
}

// ─── Turndown instance (shared, stateless enough for reuse) ─────────────────

function buildTurndown(): TurndownService {
  const td = new TurndownService({
    headingStyle: "atx",
    codeBlockStyle: "fenced",
    bulletListMarker: "-",
    hr: "---",
  });

  td.use(gfm as (service: TurndownService) => void);

  // Reconstruct the learnhub-image annotation + markdown image syntax.
  // Images without a data-lh-id (i.e. user-uploaded) are saved as plain images.
  td.addRule("learnhubImage", {
    filter(node) {
      return (
        node.nodeName === "IMG" &&
        (node as Element).getAttribute("data-lh") === "true"
      );
    },
    replacement(_content, node) {
      const el = node as Element;
      const src = el.getAttribute("src") ?? "";
      const alt = el.getAttribute("alt") ?? "";
      const id = el.getAttribute("data-lh-id") ?? "";
      const prompt = decodeURIComponent(
        el.getAttribute("data-lh-prompt") ?? "",
      );

      // User-uploaded images (lhId empty) → plain markdown image
      if (!id) {
        return `\n\n![${alt}](${src})\n\n`;
      }

      const annotation = `<!-- learnhub-image:id=${id}; prompt=${prompt} -->`;
      return `\n\n${annotation}\n![${alt}](${src})\n\n`;
    },
  });

  // Keep any other plain images as markdown images
  td.addRule("plainImage", {
    filter: "img",
    replacement(_content, node) {
      const el = node as Element;
      const src = el.getAttribute("src") ?? "";
      const alt = el.getAttribute("alt") ?? "";
      const title = el.getAttribute("title");
      return title
        ? `\n\n![${alt}](${src} "${title}")\n\n`
        : `\n\n![${alt}](${src})\n\n`;
    },
  });

  // Preserve HTML blocks that TipTap exposes via the HtmlBlock extension
  td.addRule("htmlBlock", {
    filter(node) {
      return (
        node.nodeName === "DIV" &&
        (node as Element).getAttribute("data-html-block") === "true"
      );
    },
    replacement(_content, node) {
      const encoded = (node as Element).getAttribute("data-html-content") ?? "";
      const html = decodeURIComponent(encoded);
      return `\n\n${html}\n\n`;
    },
  });

  return td;
}

const sharedTurndown = buildTurndown();

/**
 * Convert the HTML produced by TipTap's getHTML() back to markdown.
 * Learnhub image annotations are reconstructed from data-lh-* attributes.
 */
export function tiptapHtmlToMarkdown(html: string): string {
  return sharedTurndown.turndown(html);
}
