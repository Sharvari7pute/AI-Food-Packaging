import katex from "katex"

/** Renders a TeX formula (display mode) with KaTeX on the server/client. */
export function Tex({ children, inline = false }: { children: string; inline?: boolean }) {
  const html = katex.renderToString(children, { displayMode: !inline, throwOnError: false, output: "html" })
  return <span className="block overflow-x-auto py-1" dangerouslySetInnerHTML={{ __html: html }} />
}
