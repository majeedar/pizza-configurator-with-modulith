import { useEffect, useState } from "react";
import { Box, Link, Stack, Typography } from "@mui/material";
import { customerAppQrUrl, fetchCustomerAppLink } from "../api/appLinks";

/**
 * Agent.md §8.1/§8.6: shown identically to guests and logged-in customers.
 * The QR is rendered by pointing straight at the backend's on-demand PNG
 * endpoint — no image data ever touches this component directly, so it
 * can never go stale relative to the admin-configured URL (agent.md §5.1
 * AppLinkSetting: "no stored QR image").
 */
export default function GetTheAppBanner() {
  const [url, setUrl] = useState<string | null>(null);
  const [unavailable, setUnavailable] = useState(false);

  useEffect(() => {
    fetchCustomerAppLink()
      .then((link) => setUrl(link.url))
      .catch(() => setUnavailable(true));
  }, []);

  if (unavailable) {
    return (
      <Typography variant="body2" color="text.secondary">
        📱 Get the Android app — not available right now
      </Typography>
    );
  }

  if (!url) {
    return null;
  }

  return (
    <Stack direction="row" spacing={2} sx={{ alignItems: "center", justifyContent: "center" }}>
      <Box component="img" src={customerAppQrUrl()} alt="QR code to download the customer Android app" sx={{ width: 64, height: 64 }} />
      <Typography variant="body2" color="text.secondary">
        📱 <Link href={url} target="_blank" rel="noopener noreferrer">Get the Android app</Link> — scan the QR or tap the link
      </Typography>
    </Stack>
  );
}
