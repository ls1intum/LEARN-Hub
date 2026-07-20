import { describe, it, expect } from "vitest";
import { Editor } from "@tiptap/core";
import { StarterKit } from "@tiptap/starter-kit";
import {
  Table,
  TableRow,
  TableCell,
  TableHeader,
} from "@tiptap/extension-table";
import { UnifiedImage } from "@/components/ui/RichTextEditor/extensions/UnifiedImage";
import { HtmlBlock } from "@/components/ui/RichTextEditor/extensions/HtmlBlock";
import {
  markdownToTiptapHtml,
  tiptapHtmlToMarkdown,
} from "@/utils/markdownTiptap";

const DATA_URI = "data:image/png;base64,ABCD=";
const ANNOTATION = "<!-- learnhub-image:id=fig1; prompt=a cat -->";

// Regex the source/preview editor uses to discover regeneratable images.
const IMAGE_BLOCK_RE =
  /(?:<!--\s*learnhub-image:id=([^;]*?);\s*prompt=([\s\S]*?)\s*-->\s*)?!\[([^\]]*)\]\(data:[^;]+;base64,[A-Za-z0-9+/]+=*\)/g;

function countRegeneratableImages(markdown: string): number {
  IMAGE_BLOCK_RE.lastIndex = 0;
  let n = 0;
  let m: RegExpExecArray | null;
  while ((m = IMAGE_BLOCK_RE.exec(markdown)) !== null) {
    if (m[1]) n++; // only annotated (regeneratable) images
  }
  return n;
}

// Exercise the exact load → TipTap → save path the rich editor uses.
function roundtrip(markdown: string): string {
  const editor = new Editor({
    element: document.createElement("div"),
    extensions: [
      StarterKit,
      UnifiedImage,
      HtmlBlock,
      Table.configure({ resizable: false }),
      TableRow,
      TableCell,
      TableHeader,
    ],
    content: markdownToTiptapHtml(markdown),
  });
  const html = editor.getHTML();
  editor.destroy();
  return tiptapHtmlToMarkdown(html);
}

describe("markdownToTiptapHtml - images inside HTML wrappers", () => {
  it("lifts an AI image out of a tightly-wrapped <div> as a regeneratable node", () => {
    const md = `<div style="text-align:center">\n${ANNOTATION}\n![fig1](${DATA_URI})\n</div>`;
    const html = markdownToTiptapHtml(md);

    // Image carries its annotation as data-lh attributes (→ UnifiedImage)…
    expect(html).toMatch(/<img[^>]*data-lh="true"/);
    expect(html).toContain('data-lh-id="fig1"');
    // …and is NOT buried as text inside an html-block carrier.
    expect(html).toContain("data-html-block");
  });

  it("renders both tight and loose wrappers identically", () => {
    const tight = `<div style="text-align:center">\n${ANNOTATION}\n![fig1](${DATA_URI})\n</div>`;
    const loose = `<div style="text-align:center">\n\n${ANNOTATION}\n![fig1](${DATA_URI})\n\n</div>`;
    expect(markdownToTiptapHtml(tight)).toContain('data-lh-id="fig1"');
    expect(markdownToTiptapHtml(loose)).toContain('data-lh-id="fig1"');
  });
});

describe("markdownToTiptapHtml - image display in the edit view", () => {
  it("renders a base64 markdown image as an <img> with its data URI src", () => {
    const html = markdownToTiptapHtml(`![Diagramm](${DATA_URI})`);

    expect(html).toMatch(/<img[^>]*src="data:image\/png;base64,ABCD="/);
    // Not left as literal markdown text.
    expect(html).not.toContain("![Diagramm]");
  });

  it("preserves the image data URI through a round-trip", () => {
    const back = roundtrip(`![photo](${DATA_URI})`);

    // The dummy image content survives the edit-view round-trip and stays a
    // markdown image (not dropped or turned into literal text).
    expect(back).toContain(DATA_URI);
    expect(back).toMatch(/!\[[^\]]*\]\(data:image\/png;base64,/);
  });
});

describe("rich-editor round-trip (markdown → TipTap → markdown)", () => {
  it("keeps a wrapped AI image regeneratable and preserves the wrapper", () => {
    const md = `<div style="text-align:center">\n${ANNOTATION}\n![fig1](${DATA_URI})\n</div>`;
    const back = roundtrip(md);

    expect(countRegeneratableImages(back)).toBe(1);
    expect(back).toContain("learnhub-image:id=fig1");
    expect(back).toContain("text-align:center");

    // Stable across a second round-trip.
    expect(countRegeneratableImages(roundtrip(back))).toBe(1);
  });

  it("keeps a standalone AI image regeneratable (annotation not stripped)", () => {
    const md = `${ANNOTATION}\n![fig1](${DATA_URI})`;
    const back = roundtrip(md);
    expect(countRegeneratableImages(back)).toBe(1);
    expect(back).toContain("learnhub-image:id=fig1");
  });

  it("preserves an image-free HTML block", () => {
    const md = `<div class="note">Hello</div>`;
    const back = roundtrip(md);
    expect(back).toContain('<div class="note">Hello</div>');
  });
});
