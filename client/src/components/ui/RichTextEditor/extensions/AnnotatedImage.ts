import { Node, mergeAttributes } from "@tiptap/core";
import { ReactNodeViewRenderer } from "@tiptap/react";
import { ImageNodeView } from "../ImageNodeView";

export interface AnnotatedImageAttrs {
  src: string;
  alt: string;
  lhId: string;
  lhPrompt: string;
}

/**
 * TipTap node for images that carry learnhub-image annotation data.
 * Renders via ImageNodeView which shows the image plus an inline regenerate panel.
 */
export const AnnotatedImage = Node.create({
  name: "annotatedImage",
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
    ];
  },

  renderHTML({ HTMLAttributes }) {
    return [
      "img",
      mergeAttributes(HTMLAttributes, {
        "data-lh": "true",
        "data-lh-id": HTMLAttributes.lhId,
        "data-lh-prompt": encodeURIComponent(HTMLAttributes.lhPrompt ?? ""),
        src: HTMLAttributes.src,
        alt: HTMLAttributes.alt,
      }),
    ];
  },

  addNodeView() {
    return ReactNodeViewRenderer(ImageNodeView);
  },
});
