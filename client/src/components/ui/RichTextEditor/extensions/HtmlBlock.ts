import { Node } from "@tiptap/core";
import { ReactNodeViewRenderer } from "@tiptap/react";
import { HtmlBlockNodeView } from "../HtmlBlockNodeView";

declare module "@tiptap/core" {
  interface Commands<ReturnType> {
    htmlBlock: {
      insertHtmlBlock: (content: string) => ReturnType;
    };
  }
}

export const HtmlBlock = Node.create({
  name: "htmlBlock",
  group: "block",
  atom: true,
  draggable: true,

  addAttributes() {
    return {
      content: {
        default: "",
        parseHTML: (el) =>
          decodeURIComponent(el.getAttribute("data-html-content") ?? ""),
        renderHTML: (attrs) => ({
          "data-html-content": encodeURIComponent(
            (attrs.content as string) ?? "",
          ),
        }),
      },
    };
  },

  parseHTML() {
    return [{ tag: "div[data-html-block='true']" }];
  },

  renderHTML({ node, HTMLAttributes }) {
    // Emit the content as a (text) child so the serialized carrier is not
    // "blank". turndown drops blank nodes before any custom rule runs, which
    // would otherwise lose the whole HTML block on a rich-mode save round-trip.
    // On re-parse the child is ignored - parseHTML reads data-html-content.
    return [
      "div",
      { "data-html-block": "true", ...HTMLAttributes },
      (node.attrs.content as string) ?? "",
    ];
  },

  addCommands() {
    return {
      insertHtmlBlock:
        (content: string) =>
        ({ commands }) =>
          commands.insertContent({ type: this.name, attrs: { content } }),
    };
  },

  addNodeView() {
    return ReactNodeViewRenderer(HtmlBlockNodeView);
  },
});
