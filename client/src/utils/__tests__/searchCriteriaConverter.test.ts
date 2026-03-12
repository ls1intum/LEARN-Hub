import { describe, it, expect } from "vitest";
import { convertSearchCriteriaToFormData } from "../searchCriteriaConverter";

describe("searchCriteriaConverter", () => {
  describe("convertSearchCriteriaToFormData", () => {
    describe("Basic Field Conversions", () => {
      it("should convert numeric fields correctly", () => {
        const result = convertSearchCriteriaToFormData({
          targetAge: 10,
          targetDuration: 45,
          maxActivityCount: 5,
        });

        expect(result.targetAge).toBe(10);
        expect(result.targetDuration).toBe(45);
        expect(result.maxActivityCount).toBe(5);
      });

      it("should parse string numbers to integers", () => {
        const result = convertSearchCriteriaToFormData({
          targetAge: "12",
          targetDuration: "60",
          maxActivityCount: "3",
        });

        expect(result.targetAge).toBe(12);
        expect(result.targetDuration).toBe(60);
        expect(result.maxActivityCount).toBe(3);
      });

      it("should convert boolean fields correctly", () => {
        const result = convertSearchCriteriaToFormData({
          allowLessonPlans: true,
          includeBreaks: false,
        });

        expect(result.allowLessonPlans).toBe(true);
        expect(result.includeBreaks).toBe(false);
      });

      it("should parse string booleans correctly", () => {
        const result = convertSearchCriteriaToFormData({
          allowLessonPlans: "true",
          includeBreaks: "false",
        });

        expect(result.allowLessonPlans).toBe(true);
        expect(result.includeBreaks).toBe(false);
      });

      it("should handle uppercase string booleans", () => {
        const result = convertSearchCriteriaToFormData({
          allowLessonPlans: "TRUE",
          includeBreaks: "FALSE",
        });

        expect(result.allowLessonPlans).toBe(true);
        expect(result.includeBreaks).toBe(false);
      });
    });

    describe("Array Field Conversions", () => {
      it("should convert array fields correctly", () => {
        const result = convertSearchCriteriaToFormData({
          format: ["unplugged", "digital"],
          bloomLevels: ["apply", "analyze"],
          topics: ["decomposition", "patterns"],
        });

        expect(result.format).toEqual(["unplugged", "digital"]);
        expect(result.bloomLevels).toEqual(["apply", "analyze"]);
        expect(result.topics).toEqual(["decomposition", "patterns"]);
      });

      it("should handle comma-separated strings in array fields", () => {
        const result = convertSearchCriteriaToFormData({
          format: "unplugged,digital",
          bloomLevels: "apply, analyze",
          preferredTopics: "decomposition,patterns,abstraction",
        });

        expect(result.format).toEqual(["unplugged", "digital"]);
        expect(result.bloomLevels).toEqual(["apply", "analyze"]);
        expect(result.topics).toEqual([
          "decomposition",
          "patterns",
          "abstraction",
        ]);
      });

      it("should filter empty strings from comma-separated values", () => {
        const result = convertSearchCriteriaToFormData({
          format: "unplugged,,digital,",
          bloomLevels: ",apply,",
        });

        expect(result.format).toEqual(["unplugged", "digital"]);
        expect(result.bloomLevels).toEqual(["apply"]);
      });

      it("should convert single string to array for certain fields", () => {
        const result = convertSearchCriteriaToFormData({
          resourcesNeeded: "computers",
          topics: "decomposition",
        });

        expect(result.resourcesNeeded).toEqual(["computers"]);
        expect(result.topics).toEqual(["decomposition"]);
      });
    });

    describe("Server/Client Parameter Name Mapping", () => {
      it("should map availableResources to resourcesNeeded", () => {
        const result = convertSearchCriteriaToFormData({
          availableResources: ["computers", "tablets"],
        });

        expect(result.resourcesNeeded).toEqual(["computers", "tablets"]);
      });

      it("should handle comma-separated availableResources", () => {
        const result = convertSearchCriteriaToFormData({
          availableResources: "computers,tablets,handouts",
        });

        expect(result.resourcesNeeded).toEqual([
          "computers",
          "tablets",
          "handouts",
        ]);
      });

      it("should map preferredTopics to topics", () => {
        const result = convertSearchCriteriaToFormData({
          preferredTopics: ["decomposition", "patterns"],
        });

        expect(result.topics).toEqual(["decomposition", "patterns"]);
      });

      it("should handle comma-separated preferredTopics", () => {
        const result = convertSearchCriteriaToFormData({
          preferredTopics: "decomposition,patterns",
        });

        expect(result.topics).toEqual(["decomposition", "patterns"]);
      });

      it("should prioritize client names over server names", () => {
        const result = convertSearchCriteriaToFormData({
          resourcesNeeded: ["computers"],
          availableResources: ["tablets"],
        });

        // Should use availableResources when both present (server takes priority)
        expect(result.resourcesNeeded).toEqual(["tablets"]);
      });
    });

    describe("Priority Categories Handling", () => {
      it("should convert priorityCategories array", () => {
        const result = convertSearchCriteriaToFormData({
          priorityCategories: ["format", "bloomLevel"],
        });

        expect(result.priorityCategories).toEqual(["format", "bloomLevel"]);
      });

      it("should handle comma-separated priorityCategories", () => {
        const result = convertSearchCriteriaToFormData({
          priorityCategories: "format,bloomLevel,topics",
        });

        expect(result.priorityCategories).toEqual([
          "format",
          "bloomLevel",
          "topics",
        ]);
      });

      it("should map legacy priority_fields to priorityCategories", () => {
        const result = convertSearchCriteriaToFormData({
          priority_fields: ["format", "topics"],
        });

        expect(result.priorityCategories).toEqual(["format", "topics"]);
      });

      it("should default to empty array when no priority categories", () => {
        const result = convertSearchCriteriaToFormData({
          targetAge: 10,
        });

        expect(result.priorityCategories).toEqual([]);
      });

      it("should prioritize priorityCategories over priority_fields", () => {
        const result = convertSearchCriteriaToFormData({
          priorityCategories: ["format"],
          priority_fields: ["topics"],
        });

        expect(result.priorityCategories).toEqual(["format"]);
      });
    });

    describe("Edge Cases", () => {
      it("should handle empty object", () => {
        const result = convertSearchCriteriaToFormData({});

        expect(result.priorityCategories).toEqual([]);
        expect(Object.keys(result).length).toBe(1); // Only priorityCategories
      });

      it("should handle undefined values", () => {
        const result = convertSearchCriteriaToFormData({
          targetAge: undefined,
          format: undefined,
        });

        expect(result.targetAge).toBeUndefined();
        expect(result.format).toBeUndefined();
      });

      it("should handle null values (converts to NaN for numbers)", () => {
        const result = convertSearchCriteriaToFormData({
          targetAge: null,
          format: null,
        });

        // parseInt(String(null)) = parseInt('null') = NaN
        expect(result.targetAge).toBeNaN();
        expect(result.format).toBeUndefined();
      });

      it("should handle mixed types in arrays", () => {
        const result = convertSearchCriteriaToFormData({
          format: [1, "digital", true],
        });

        expect(result.format).toEqual(["1", "digital", "true"]);
      });

      it("should handle whitespace in comma-separated strings", () => {
        const result = convertSearchCriteriaToFormData({
          format: "  unplugged  ,  digital  ,  hybrid  ",
        });

        expect(result.format).toEqual(["unplugged", "digital", "hybrid"]);
      });

      it("should handle empty strings in arrays", () => {
        const result = convertSearchCriteriaToFormData({
          format: ["unplugged", "", "digital"],
        });

        expect(result.format).toEqual(["unplugged", "", "digital"]);
      });

      it("should handle zero values correctly", () => {
        const result = convertSearchCriteriaToFormData({
          targetAge: 0,
          targetDuration: 0,
          maxActivityCount: 0,
        });

        expect(result.targetAge).toBe(0);
        expect(result.targetDuration).toBe(0);
        expect(result.maxActivityCount).toBe(0);
      });

      it('should handle string "0" for booleans as false', () => {
        const result = convertSearchCriteriaToFormData({
          allowLessonPlans: "0",
          includeBreaks: "no",
        });

        expect(result.allowLessonPlans).toBe(false);
        expect(result.includeBreaks).toBe(false);
      });
    });

    describe("Real-world Scenarios", () => {
      it("should convert complete search criteria from server", () => {
        const serverCriteria = {
          targetAge: 10,
          format: ["unplugged", "digital"],
          availableResources: ["computers", "handouts"],
          bloomLevels: ["apply", "analyze"],
          targetDuration: 45,
          preferredTopics: ["decomposition"],
          allowLessonPlans: true,
          maxActivityCount: 5,
          includeBreaks: false,
          priorityCategories: ["format", "bloomLevel"],
        };

        const result = convertSearchCriteriaToFormData(serverCriteria);

        expect(result).toEqual({
          targetAge: 10,
          format: ["unplugged", "digital"],
          resourcesNeeded: ["computers", "handouts"],
          bloomLevels: ["apply", "analyze"],
          targetDuration: 45,
          topics: ["decomposition"],
          allowLessonPlans: true,
          maxActivityCount: 5,
          includeBreaks: false,
          priorityCategories: ["format", "bloomLevel"],
        });
      });

      it("should handle URL query parameters format", () => {
        const urlParams = {
          targetAge: "12",
          format: "unplugged,digital",
          availableResources: "computers,tablets",
          bloomLevels: "apply",
          targetDuration: "60",
          allowLessonPlans: "true",
        };

        const result = convertSearchCriteriaToFormData(urlParams);

        expect(result.targetAge).toBe(12);
        expect(result.format).toEqual(["unplugged", "digital"]);
        expect(result.resourcesNeeded).toEqual(["computers", "tablets"]);
        expect(result.targetDuration).toBe(60);
        expect(result.allowLessonPlans).toBe(true);
      });
    });
  });
});
