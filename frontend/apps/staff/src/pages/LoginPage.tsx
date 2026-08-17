import { useState } from "react";
import { Alert, Box, Button, Stack, TextField, Typography } from "@mui/material";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../state/AuthContext";
import { ApiError } from "../api/client";

/**
 * Single shared login screen for both roles (agent.md §8.4) — post-login
 * routing (not this page) decides KDS vs Admin Portal.
 */
export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit() {
    setSubmitting(true);
    setError(null);
    try {
      const result = await login(username, password);
      navigate(result.role === "ADMIN" ? "/admin" : "/board");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Box sx={{ maxWidth: 400, mx: "auto", mt: 8 }}>
      <Typography variant="h4" gutterBottom>
        Staff sign in
      </Typography>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        Kitchen and Admin accounts share this login.
      </Typography>

      <Stack spacing={2} sx={{ mt: 3 }}>
        <TextField
          label="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
          required
          autoFocus
        />
        <TextField
          label="Password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
          required
        />

        {error && <Alert severity="error">{error}</Alert>}

        <Button variant="contained" size="large" onClick={handleSubmit} disabled={submitting}>
          Log in
        </Button>
      </Stack>
    </Box>
  );
}
