import { Node } from "@tiptap/core";
import { ReactNodeViewRenderer } from "@tiptap/react";
import { ImageNodeView } from "../ImageNodeView";

export interface UnifiedImageAttrs {
  src: string;
  alt: string;
  /** Non-empty only for AI-generated images. Controls regenerate panel visibility. */
  lhId: string;
  lhPrompt: string;
}

declare module "@tiptap/core" {
  interface Commands<ReturnType> {
    unifiedImage: {
      insertUnifiedImage: (attrs: UnifiedImageAttrs) => ReturnType;
    };
  }
}

/**
 * Single image node that covers both:
 *  - AI-generated images (lhId set)  → shows regenerate panel
 *  - Uploaded images (lhId empty)    → shows delete button only
 */
export const UnifiedImage = Node.create({
  name: "unifiedImage",
  group: "block",
  atom: true,
  draggable: true,

  addAttributes() {
    return {
      src: { default: "" },
      alt: { default: "" },
      lhId: { default: "" },
      lhPrompt: { default: "" },
    };
  },

  parseHTML() {
    return [
      // AI-generated images (carry annotation data attributes)
      {
        tag: "img[data-lh]",
        getAttrs(node) {
          const el = node as HTMLElement;
          return {
            src: el.getAttribute("src") ?? "",
            alt: el.getAttribute("alt") ?? "",
            lhId: el.getAttribute("data-lh-id") ?? "",
            lhPrompt: decodeURIComponent(
              el.getAttribute("data-lh-prompt") ?? "",
            ),
          };
        },
      },
      // Uploaded / plain images
      {
        tag: "img:not([data-lh])",
        getAttrs(node) {
          const el = node as HTMLElement;
          return {
            src: el.getAttribute("src") ?? "",
            alt: el.getAttribute("alt") ?? "",
            lhId: "",
            lhPrompt: "",
          };
        },
      },
    ];
  },

  renderHTML({ HTMLAttributes }) {
    const { src, alt, lhId, lhPrompt } = HTMLAttributes as UnifiedImageAttrs;
    if (lhId) {
      return [
        "img",
        {
          "data-lh": "true",
          "data-lh-id": lhId,
          "data-lh-prompt": encodeURIComponent(lhPrompt ?? ""),
          src,
          alt,
        },
      ];
    }
    return ["img", { src, alt }];
  },

  addCommands() {
    return {
      insertUnifiedImage:
        (attrs: UnifiedImageAttrs) =>
        ({ commands }) => {
          return commands.insertContent({ type: this.name, attrs });
        },
    };
  },

  addNodeView() {
    return ReactNodeViewRenderer(ImageNodeView);
  },
});
