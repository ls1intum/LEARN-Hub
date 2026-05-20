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

  renderHTML({ HTMLAttributes }) {
    return ["div", { "data-html-block": "true", ...HTMLAttributes }];
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
