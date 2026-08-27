import nextCoreWebVitals from "eslint-config-next/core-web-vitals";
import nextTypescript from "eslint-config-next/typescript";
import boundaries from "eslint-plugin-boundaries";

// Chiều import FSD: app -> views -> widgets -> features -> entities -> shared.
// Tầng dưới không bao giờ biết tầng trên; cùng tầng không import lẫn nhau.
// Nguồn: 01-rd/system/frontend_architecture.md mục 2.
const FSD_LAYERS = ["app", "views", "widgets", "features", "entities", "shared"];

const elementTypes = FSD_LAYERS.map((type) => ({
  type,
  pattern: type === "app" ? "app/**" : `${type}/*`,
  mode: "folder",
  capture: type === "app" ? undefined : ["slice"],
}));

const eslintConfig = [
  ...nextCoreWebVitals,
  ...nextTypescript,
  {
    files: ["**/*.{ts,tsx}"],
    plugins: {
      boundaries,
    },
    settings: {
      "boundaries/include": ["src/**/*.{ts,tsx}"],
      "boundaries/elements": elementTypes,
    },
    rules: {
      eqeqeq: ["error", "always", { null: "ignore" }],
      "no-console": ["error", { allow: ["warn", "error"] }],
      "@typescript-eslint/no-unused-vars": [
        "error",
        { argsIgnorePattern: "^_", varsIgnorePattern: "^_" },
      ],
      "@typescript-eslint/consistent-type-imports": ["error", { prefer: "type-imports" }],
      "no-restricted-imports": [
        "error",
        {
          paths: [
            {
              name: "next-intl",
              message:
                "Dùng @/shared/i18n (client) hoặc @/shared/i18n/server (server), không import next-intl trực tiếp ngoài shared/i18n.",
            },
            {
              name: "dompurify",
              message: "Dùng sanitizeHtml() từ @/shared/lib, không import dompurify trực tiếp.",
            },
          ],
          // Public API Rule (quy tắc vàng số 1 của FSD, frontend_architecture.md:93-95):
          // chỉ được import một slice qua index.ts ở gốc slice, không trỏ sâu vào file bên trong.
          // shared/ được miễn vì nó chia theo segment kỹ thuật, không có slice (shared/i18n/server
          // là entry point hợp lệ, không phải deep import).
          patterns: [
            {
              group: [
                "@/entities/*/*",
                "@/features/*/*",
                "@/widgets/*/*",
                "@/views/*/*",
                "../entities/*/*",
                "../features/*/*",
                "../widgets/*/*",
                "../views/*/*",
              ],
              message:
                "Deep import bị chặn (Public API Rule) — import qua index.ts của slice, ví dụ '@/entities/user' thay vì '@/entities/user/model/types'.",
            },
          ],
        },
      ],
      "boundaries/element-types": [
        "error",
        {
          default: "disallow",
          rules: [
            { from: "app", allow: ["views", "widgets", "features", "entities", "shared", "app"] },
            { from: "views", allow: ["widgets", "features", "entities", "shared"] },
            { from: "widgets", allow: ["features", "entities", "shared"] },
            { from: "features", allow: ["entities", "shared"] },
            { from: "entities", allow: ["shared"] },
            { from: "shared", allow: ["shared"] },
          ],
        },
      ],
    },
  },
  {
    files: ["src/shared/i18n/**"],
    rules: {
      "no-restricted-imports": "off",
    },
  },
  {
    ignores: [".next/**", "node_modules/**", "playwright-report/**", "test-results/**"],
  },
];

export default eslintConfig;
