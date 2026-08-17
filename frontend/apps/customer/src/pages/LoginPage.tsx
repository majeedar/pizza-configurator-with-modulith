import { useState } from "react";
import { Alert, Box, Button, Stack, TextField, ToggleButton, ToggleButtonGroup, Typography } from "@mui/material";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../state/AuthContext";
import { ApiError } from "../api/client";

export default function LoginPage() {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const { login, register } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit() {
    setSubmitting(true);
    setError(null);
    try {
      if (mode === "login") {
        await login(email, password);
      } else {
        await register(name, email, phoneNumber || null, password);
      }
      navigate("/");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Box sx={{ maxWidth: 400 }}>
      <Typography variant="h4" gutterBottom>
        Account
      </Typography>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        Logging in is optional — you can always check out as a guest.
      </Typography>

      <ToggleButtonGroup
        value={mode}
        exclusive
        onChange={(_, value) => value && setMode(value)}
        sx={{ my: 2 }}
        fullWidth
      >
        <ToggleButton value="login">Log in</ToggleButton>
        <ToggleButton value="register">Register</ToggleButton>
      </ToggleButtonGroup>

      <Stack spacing={2}>
        {mode === "register" && (
          <TextField label="Name" value={name} onChange={(e) => setName(e.target.value)} required />
        )}
        <TextField label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        {mode === "register" && (
          <TextField
            label="Phone number (optional)"
            value={phoneNumber}
            onChange={(e) => setPhoneNumber(e.target.value)}
          />
        )}
        <TextField
          label="Password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />

        {error && <Alert severity="error">{error}</Alert>}

        <Button variant="contained" size="large" onClick={handleSubmit} disabled={submitting}>
          {mode === "login" ? "Log in" : "Create account"}
        </Button>
      </Stack>
    </Box>
  );
}
