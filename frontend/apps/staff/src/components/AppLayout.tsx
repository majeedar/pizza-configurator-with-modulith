import { useCallback, useEffect, useState } from "react";
import { AppBar, Badge, Box, Button, Chip, Container, Toolbar, Typography } from "@mui/material";
import { Link, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../state/AuthContext";
import { fetchActiveOrders } from "../api/kitchen";
import { fetchReviewQueue } from "../api/kitchenReviews";

const BADGE_POLL_INTERVAL_MS = 8000;

export default function AppLayout() {
  const { isAuthenticated, displayName, role, token, logout } = useAuth();
  const location = useLocation();
  const onBoardRoute = location.pathname.startsWith("/board");
  const onAdminRoute = location.pathname.startsWith("/admin");
  const onReviewsRoute = location.pathname.startsWith("/reviews");

  // How many things are waiting for this staff member's attention right
  // now — a freshly-placed order not yet approved, or a review request not
  // yet decided — shown as a badge on the nav button for whichever page
  // they're *not* currently looking at, so nothing gets missed while
  // working the other queue.
  const [newOrderCount, setNewOrderCount] = useState(0);
  const [openReviewCount, setOpenReviewCount] = useState(0);

  const refreshBadges = useCallback(async () => {
    if (!token) return;
    try {
      const orders = await fetchActiveOrders(token);
      setNewOrderCount(orders.filter((o) => o.status === "CONFIRMED").length);
    } catch {
      // Transient — the next poll will retry; don't clear a possibly-still-accurate count.
    }
    try {
      const reviews = await fetchReviewQueue(token);
      setOpenReviewCount(reviews.filter((r) => r.status === "OPEN").length);
    } catch {
      // Same as above.
    }
  }, [token]);

  useEffect(() => {
    if (!isAuthenticated) return;
    refreshBadges();
    const interval = setInterval(refreshBadges, BADGE_POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [isAuthenticated, refreshBadges]);

  return (
    <Box sx={{ display: "flex", flexDirection: "column", minHeight: "100vh" }}>
      <AppBar position="static" color="primary">
        <Toolbar sx={{ gap: 2 }}>
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 700 }}>
            🍕 Pizza Configurator — Staff
          </Typography>

          {isAuthenticated && (
            <>
              {/* Production board and Reviews are independent destinations reachable by
                  both KITCHEN and ADMIN (same as their backend routes) — always both
                  present, rather than one toggle button whose target depended on
                  whether the *previous* page happened to be /board (it wasn't always,
                  e.g. landing here from /reviews, which left no way back). */}
              <Badge badgeContent={newOrderCount} color="error" sx={{ "& .MuiBadge-badge": { right: -6, top: 2 } }}>
                <Button color="inherit" variant={onBoardRoute ? "outlined" : "text"} component={Link} to="/board">
                  Production board
                </Button>
              </Badge>
              <Badge badgeContent={openReviewCount} color="error" sx={{ "& .MuiBadge-badge": { right: -6, top: 2 } }}>
                <Button color="inherit" variant={onReviewsRoute ? "outlined" : "text"} component={Link} to="/reviews">
                  Reviews
                </Button>
              </Badge>
              {role === "ADMIN" && (
                <Button color="inherit" variant={onAdminRoute ? "outlined" : "text"} component={Link} to="/admin">
                  Admin Portal
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
