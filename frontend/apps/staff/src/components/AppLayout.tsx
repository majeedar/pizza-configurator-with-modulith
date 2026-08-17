import { AppBar, Box, Button, Chip, Container, Toolbar, Typography } from "@mui/material";
import { Link, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../state/AuthContext";

export default function AppLayout() {
  const { isAuthenticated, displayName, role, logout } = useAuth();
  const location = useLocation();
  const onAdminRoute = location.pathname.startsWith("/admin");

  return (
    <Box sx={{ display: "flex", flexDirection: "column", minHeight: "100vh" }}>
      <AppBar position="static" color="primary">
        <Toolbar sx={{ gap: 2 }}>
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 700 }}>
            🍕 Pizza Configurator — Staff
          </Typography>

          {isAuthenticated && (
            <>
              {/* ROLE_ADMIN "can also reach the KDS views" (agent.md §8.3) — a two-way
                  switch, not a one-time landing choice at login. */}
              {role === "ADMIN" && (
                <Button
                  color="inherit"
                  variant={onAdminRoute ? "outlined" : "text"}
                  component={Link}
                  to={onAdminRoute ? "/board" : "/admin"}
                >
                  {onAdminRoute ? "Production board" : "Admin Portal"}
                </Button>
              )}
              <Chip label={role} color="secondary" size="small" />
              <Typography variant="body2">{displayName}</Typography>
              <Button color="inherit" onClick={logout}>
                Log out
              </Button>
            </>
          )}
        </Toolbar>
      </AppBar>

      <Container component="main" maxWidth={false} sx={{ flexGrow: 1, py: 4 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
