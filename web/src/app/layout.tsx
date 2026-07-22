import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "Movento", template: "%s · Movento" },
  description: "Curated stories, made to move you.",
  metadataBase: new URL(process.env.NEXT_PUBLIC_APP_URL ?? "http://localhost:3000"),
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="dark">
      <body className="font-sans antialiased">{children}</body>
    </html>
  );
}
