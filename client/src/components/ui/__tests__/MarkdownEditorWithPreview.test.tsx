import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import type { ComponentProps } from "react";
import {
  render,
  screen,
  waitFor,
  cleanup,
  fireEvent,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MarkdownEditorWithPreview } from "../MarkdownEditorWithPreview";

// A long dummy payload so we can assert it is collapsed (not flooding the editor).
const BASE64 = "A".repeat(400);
const IMAGE_DATA_URI = `data:image/png;base64,${BASE64}`;
const MARKDOWN_WITH_IMAGE = `# Titel\n\n![pic](${IMAGE_DATA_URI})\n`;

function renderEditor(
  overrides: Partial<ComponentProps<typeof MarkdownEditorWithPreview>> = {},
) {
  const onChange = vi.fn();
  const renderPreviewFn = vi.fn(
    async () => new Blob(["%PDF-1.4"], { type: "application/pdf" }),
  );
  const utils = render(
    <MarkdownEditorWithPreview
      value={MARKDOWN_WITH_IMAGE}
      onChange={onChange}
      renderPreviewFn={renderPreviewFn}
      {...overrides}
    />,
  );
  return { onChange, renderPreviewFn, ...utils };
}

beforeEach(() => {
  // jsdom doesn't implement blob URL helpers used by the preview pane.
  globalThis.URL.createObjectURL = vi.fn(() => "blob:preview-url");
  globalThis.URL.revokeObjectURL = vi.fn();
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("MarkdownEditorWithPreview — image in the edit view (source mode)", () => {
  it("collapses the base64 payload into a short token in the textarea", async () => {
    const user = userEvent.setup();
    renderEditor();

    // Default is rich mode; switch to the raw source editor.
    await user.click(screen.getByRole("button", { name: /Quellcode/i }));

    const textarea = screen.getByRole("textbox") as HTMLTextAreaElement;
    expect(textarea.value).toContain("<base64-img:");
    // The megabyte-sized payload must NOT be present verbatim in the editor.
    expect(textarea.value).not.toContain(BASE64);
    // The data URI prefix is kept so the image stays recognizable.
    expect(textarea.value).toContain("data:image/png;base64,");
  });

  it("restores the full base64 payload before handing the value to onChange", async () => {
    const user = userEvent.setup();
    const { onChange } = renderEditor();

    await user.click(screen.getByRole("button", { name: /Quellcode/i }));
    const textarea = screen.getByRole("textbox") as HTMLTextAreaElement;

    // Edit the collapsed display value; the component must expand the token
    // back to the real base64 before calling onChange.
    fireEvent.change(textarea, {
      target: { value: textarea.value + "\n\nNeuer Absatz" },
    });

    expect(onChange).toHaveBeenCalled();
    const emitted = onChange.mock.calls.at(-1)?.[0] as string;
    expect(emitted).toContain(BASE64);
    expect(emitted).toContain("Neuer Absatz");
  });
});

describe("MarkdownEditorWithPreview — PDF preview", () => {
  it("renders the markdown to a PDF preview iframe (dummy blob)", async () => {
    const { renderPreviewFn } = renderEditor();

    await waitFor(() => expect(renderPreviewFn).toHaveBeenCalled(), {
      timeout: 2000,
    });
    expect(renderPreviewFn.mock.calls[0][0]).toContain(
      "data:image/png;base64,",
    );

    const iframe = await screen.findByTitle("Dokumentvorschau", undefined, {
      timeout: 2000,
    });
    expect(iframe).toHaveAttribute("src", "blob:preview-url");
  });
});

describe("MarkdownEditorWithPreview — image regeneration", () => {
  it("invokes onRegenerateImage when the regenerate control is used", async () => {
    const user = userEvent.setup();
    const onRegenerateImage = vi.fn(async () => `![pic](${IMAGE_DATA_URI})`);
    renderEditor({ onRegenerateImage });

    // Regeneration toolbar only appears in source mode.
    await user.click(screen.getByRole("button", { name: /Quellcode/i }));

    const regenButton = screen.getByRole("button", {
      name: /Bild neu generieren/i,
    });
    await user.click(regenButton);

    await waitFor(() => expect(onRegenerateImage).toHaveBeenCalledTimes(1));
    expect(onRegenerateImage.mock.calls[0][0]).toMatchObject({
      imageId: "pic",
    });
  });
});
