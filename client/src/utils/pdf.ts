const PDF_EXTENSION = ".pdf";

const HTML_ESCAPE_MAP: Record<string, string> = {
  "&": "&amp;",
  "<": "&lt;",
  ">": "&gt;",
  '"': "&quot;",
  "'": "&#39;",
};

const escapeHtml = (value: string) =>
  value.replace(/[&<>"']/g, (character) => HTML_ESCAPE_MAP[character]);

export const ensurePdfFilename = (name?: string) => {
  const trimmedName = name?.trim();
  if (!trimmedName) {
    return `document${PDF_EXTENSION}`;
  }

  return trimmedName.toLowerCase().endsWith(PDF_EXTENSION)
    ? trimmedName
    : `${trimmedName}${PDF_EXTENSION}`;
};

const isMobile = () =>
  /iPhone|iPad|iPod|Android/i.test(navigator.userAgent) ||
  (navigator.maxTouchPoints > 1 && /Mac/.test(navigator.platform));

export const openPdfInNewTab = (blob: Blob, title?: string) => {
  const filename = ensurePdfFilename(title);
  const pdfFile =
    blob instanceof File
      ? blob
      : new File([blob], filename, {
          type: blob.type || "application/pdf",
        });
  const pdfUrl = URL.createObjectURL(pdfFile);

  // On mobile, iframes with blob URLs don't render PDFs. Trigger a download
  // instead so the OS opens the file in the native PDF viewer.
  if (isMobile()) {
    const a = document.createElement("a");
    a.href = pdfUrl;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(pdfUrl), 5000);
    return;
  }

  const popup = window.open("", "_blank");

  if (!popup) {
    URL.revokeObjectURL(pdfUrl);
    throw new Error("Unable to open PDF tab");
  }

  const escapedTitle = escapeHtml(filename);

  popup.document.write(`<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <title>${escapedTitle}</title>
    <style>
      html, body {
        height: 100%;
        margin: 0;
        background: #525659;
      }

      iframe {
        border: 0;
        width: 100%;
        height: 100%;
      }
    </style>
  </head>
  <body>
    <iframe src="${pdfUrl}" title="${escapedTitle}"></iframe>
  </body>
</html>`);
  popup.document.close();

  const cleanup = () => {
    URL.revokeObjectURL(pdfUrl);
  };

  popup.addEventListener("pagehide", cleanup, { once: true });

  const closeWatcher = window.setInterval(() => {
    if (popup.closed) {
      window.clearInterval(closeWatcher);
      cleanup();
    }
  }, 1000);
};
