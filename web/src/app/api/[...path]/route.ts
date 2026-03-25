import { NextRequest, NextResponse } from "next/server";

const BACKEND_URL = process.env.BACKEND_URL || "http://localhost:8000";

async function proxy(req: NextRequest) {
  const { pathname, search } = req.nextUrl;
  // Strip /api prefix and map to /v1
  const backendPath = "/v1" + pathname.replace(/^\/api/, "");
  const url = `${BACKEND_URL}${backendPath}${search}`;

  const headers: Record<string, string> = {};
  req.headers.forEach((value, key) => {
    // Skip hop-by-hop and host headers
    if (!["host", "connection", "transfer-encoding"].includes(key.toLowerCase())) {
      headers[key] = value;
    }
  });

  try {
    const resp = await fetch(url, {
      method: req.method,
      headers,
      body: req.method !== "GET" && req.method !== "HEAD" ? await req.text() : undefined,
      redirect: "follow",
    });

    const body = await resp.arrayBuffer();
    const respHeaders = new Headers();
    resp.headers.forEach((value, key) => {
      if (!["transfer-encoding", "connection"].includes(key.toLowerCase())) {
        respHeaders.set(key, value);
      }
    });

    return new NextResponse(body, {
      status: resp.status,
      headers: respHeaders,
    });
  } catch {
    return NextResponse.json(
      { detail: "Backend unavailable — is the FastAPI server running on port 8000?" },
      { status: 502 }
    );
  }
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
export const HEAD = proxy;
export const OPTIONS = proxy;
