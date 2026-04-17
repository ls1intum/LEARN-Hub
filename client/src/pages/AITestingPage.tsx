import React, { useState, useCallback, useRef } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Loader2, FlaskConical, Upload, FileText } from "lucide-react";
import { MarkdownEditorWithPreview } from "@/components/ui/MarkdownEditorWithPreview";
import { apiService } from "@/services/apiService";
import { authService } from "@/services/authService";
import { PageHeader } from "@/components/ui/PageHeader";

type MarkdownType =
  | "uebung"
  | "deckblatt"
  | "hintergrundwissen"
  | "artikulationsschema";

interface TestMarkdownResponse {
  uebungMarkdown?: string;
  uebungLoesungMarkdown?: string;
  deckblattMarkdown?: string;
  artikulationsschemaMarkdown?: string;
  hintergrundwissenMarkdown?: string;
}

type ResultTab = "primary" | "secondary";

const MARKDOWN_TYPES: MarkdownType[] = [
  "uebung",
  "deckblatt",
  "hintergrundwissen",
  "artikulationsschema",
];

const TYPE_LABEL_KEYS: Record<MarkdownType, string> = {
  uebung: "aiTesting.typeUebung",
  deckblatt: "aiTesting.typeDeckblatt",
  hintergrundwissen: "aiTesting.typeHintergrundwissen",
  artikulationsschema: "aiTesting.typeArtikulationsschema",
};

const ORIENTATION: Record<MarkdownType, "portrait" | "landscape"> = {
  uebung: "portrait",
  deckblatt: "portrait",
  hintergrundwissen: "portrait",
  artikulationsschema: "landscape",
};

export const AITestingPage: React.FC = () => {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [selectedType, setSelectedType] = useState<MarkdownType>("uebung");
  const [metadataJson, setMetadataJson] = useState<string>("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<TestMarkdownResponse | null>(null);
  const [activeResultTab, setActiveResultTab] = useState<ResultTab>("primary");

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setSelectedFile(file);
    setResult(null);
    setError(null);
  };

  const handleGenerate = async () => {
    if (!selectedFile) return;
    setIsLoading(true);
    setError(null);
    setResult(null);

    try {
      const formData = new FormData();
      formData.append("file", selectedFile);
      formData.append("type", selectedType);
      if (metadataJson.trim()) {
        formData.append("metadata", metadataJson.trim());
      }

      const response = await authService.makeAuthenticatedRequest(
        "/api/dev/test-markdown",
        { method: "POST", body: formData },
      );

      if (!response.ok) {
        const err = await response
          .json()
          .catch(() => ({ message: response.statusText }));
        throw new Error(err.message ?? `HTTP ${response.status}`);
      }

      const data: TestMarkdownResponse = await response.json();
      setResult(data);
      setActiveResultTab("primary");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Generation failed");
    } finally {
      setIsLoading(false);
    }
  };

  const primaryMarkdown = result
    ? (result.uebungMarkdown ??
      result.deckblattMarkdown ??
      result.hintergrundwissenMarkdown ??
      result.artikulationsschemaMarkdown ??
      "")
    : "";

  const secondaryMarkdown = result?.uebungLoesungMarkdown ?? "";
  const hasSecondary = !!secondaryMarkdown;

  const makePrimaryPreviewFn = useCallback(
    (markdown: string) =>
      apiService.previewMarkdownPdf(
        markdown,
        ORIENTATION[selectedType],
        selectedFile?.name,
      ),
    [selectedType, selectedFile],
  );

  const makeSecondaryPreviewFn = useCallback(
    (markdown: string) =>
      apiService.previewMarkdownPdf(markdown, "portrait", selectedFile?.name),
    [selectedFile],
  );

  return (
    <div className="py-6 space-y-6">
      <PageHeader
        title={t("aiTesting.title")}
        description={t("aiTesting.description")}
      />

      {/* Configuration */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">
            {t("aiTesting.configTitle")}
          </CardTitle>
          <CardDescription>{t("aiTesting.configDescription")}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* File upload */}
          <div className="space-y-2">
            <label className="text-sm font-medium">
              {t("aiTesting.fileLabel")}
            </label>
            <div
              className="flex items-center gap-3 rounded-md border border-dashed p-4 cursor-pointer hover:bg-muted/50 transition-colors"
              onClick={() => fileInputRef.current?.click()}
            >
              <Upload className="h-5 w-5 text-muted-foreground shrink-0" />
              {selectedFile ? (
                <div className="flex items-center gap-2 min-w-0">
                  <FileText className="h-4 w-4 text-primary shrink-0" />
                  <span className="text-sm truncate">{selectedFile.name}</span>
                  <span className="text-xs text-muted-foreground shrink-0">
                    ({(selectedFile.size / 1024).toFixed(0)} KB)
                  </span>
                </div>
              ) : (
                <span className="text-sm text-muted-foreground">
                  {t("aiTesting.filePlaceholder")}
                </span>
              )}
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept="application/pdf"
              className="hidden"
              onChange={handleFileChange}
            />
          </div>

          {/* Type selector */}
          <div className="space-y-2">
            <label className="text-sm font-medium">
              {t("aiTesting.typeLabel")}
            </label>
            <Select
              value={selectedType}
              onValueChange={(v) => setSelectedType(v as MarkdownType)}
            >
              <SelectTrigger className="w-full max-w-sm">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {MARKDOWN_TYPES.map((value) => (
                  <SelectItem key={value} value={value}>
                    {t(TYPE_LABEL_KEYS[value])}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Optional metadata */}
          <div className="space-y-2">
            <label className="text-sm font-medium">
              {t("aiTesting.metadataLabel")}{" "}
              <span className="text-muted-foreground font-normal">
                ({t("aiTesting.metadataOptional")})
              </span>
            </label>
            <textarea
              className="w-full rounded-md border bg-background px-3 py-2 text-sm font-mono min-h-[80px] resize-y focus:outline-none focus:ring-2 focus:ring-ring"
              placeholder={
                '{"ageMin": 10, "ageMax": 12, "name": "My Activity"}'
              }
              value={metadataJson}
              onChange={(e) => setMetadataJson(e.target.value)}
            />
          </div>

          {/* Generate button */}
          <Button
            onClick={handleGenerate}
            disabled={!selectedFile || isLoading}
            className="w-full sm:w-auto"
          >
            {isLoading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                {t("aiTesting.generating")}
              </>
            ) : (
              <>
                <FlaskConical className="mr-2 h-4 w-4" />
                {t("aiTesting.generateButton")}
              </>
            )}
          </Button>
        </CardContent>
      </Card>

      {/* Error */}
      {error && (
        <Card className="border-destructive">
          <CardContent className="pt-6">
            <p className="text-sm text-destructive">{error}</p>
          </CardContent>
        </Card>
      )}

      {/* Results */}
      {result && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">
              {t("aiTesting.resultTitle")}
            </CardTitle>
            {hasSecondary && (
              <div className="flex gap-2 pt-1">
                <Button
                  variant={
                    activeResultTab === "primary" ? "default" : "outline"
                  }
                  size="sm"
                  onClick={() => setActiveResultTab("primary")}
                >
                  {t("aiTesting.tabUebung")}
                </Button>
                <Button
                  variant={
                    activeResultTab === "secondary" ? "default" : "outline"
                  }
                  size="sm"
                  onClick={() => setActiveResultTab("secondary")}
                >
                  {t("aiTesting.tabLoesung")}
                </Button>
              </div>
            )}
          </CardHeader>
          <CardContent>
            {activeResultTab === "primary" && primaryMarkdown && (
              <MarkdownEditorWithPreview
                value={primaryMarkdown}
                onChange={() => {}}
                renderPreviewFn={makePrimaryPreviewFn}
              />
            )}
            {activeResultTab === "secondary" && secondaryMarkdown && (
              <MarkdownEditorWithPreview
                value={secondaryMarkdown}
                onChange={() => {}}
                renderPreviewFn={makeSecondaryPreviewFn}
              />
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
};
