import { describe, it, expect, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { PrivacyPage } from "../PrivacyPage";

afterEach(cleanup);

describe("PrivacyPage", () => {
  it("renders the privacy heading", () => {
    render(<PrivacyPage />);
    // Test i18n default language is German (see src/test/setup.ts)
    expect(
      screen.getByRole("heading", { name: "Datenschutzerklärung", level: 1 }),
    ).toBeInTheDocument();
  });

  it("documents the AI processing via Microsoft Azure OpenAI (GPT-4.1)", () => {
    render(<PrivacyPage />);
    expect(
      screen.getByRole("heading", { name: /Microsoft Azure OpenAI/ }),
    ).toBeInTheDocument();
    expect(screen.getAllByText(/GPT-4\.1/).length).toBeGreaterThan(0);
  });

  it("documents the PDF-to-DOCX conversion via Adobe PDF Services", () => {
    render(<PrivacyPage />);
    expect(
      screen.getByRole("heading", { name: /Adobe PDF Services/ }),
    ).toBeInTheDocument();
  });

  it("names the controller and data protection officer contacts", () => {
    render(<PrivacyPage />);
    expect(
      screen.getAllByText(/Prof\. Dr\. Stephan Krusche/).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText(/beauftragter@datenschutz\.tum\.de/).length,
    ).toBeGreaterThan(0);
  });
});
