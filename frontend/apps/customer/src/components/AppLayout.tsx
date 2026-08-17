import { AppBar, Badge, Box, Button, Container, Toolbar, Typography } from "@mui/material";
import ShoppingBasketIcon from "@mui/icons-material/ShoppingBasketOutlined";
import { Link as RouterLink, Outlet } from "react-router-dom";
import { useAuth } from "../state/AuthContext";
import { useBasket } from "../state/BasketContext";
import GetTheAppBanner from "./GetTheAppBanner";

export default function AppLayout() {
  const { isGuest, name, logout } = useAuth();
  const { basket } = useBasket();
  const itemCount = basket?.items.length ?? 0;

  return (
    <Box sx={{ display: "flex", flexDirection: "column", minHeight: "100vh" }}>
      <AppBar position="static" color="primary">
        <Toolbar sx={{ gap: 2 }}>
          <Typography
            variant="h6"
            component={RouterLink}
            to="/"
            sx={{ flexGrow: 1, color: "inherit", textDecoration: "none", fontWeight: 700 }}
          >
            🍕 Pizza Configurator
          </Typography>

          {isGuest ? (
            <Button color="inherit" component={RouterLink} to="/login">
              Log in
            </Button>
          ) : (
            <>
              <Typography variant="body2">Hi, {name}</Typography>
              <Button color="inherit" onClick={logout}>
                Log out
              </Button>
            </>
          )}

          <Button color="inherit" component={RouterLink} to="/basket" startIcon={
            <Badge badgeContent={itemCount} color="secondary">
              <ShoppingBasketIcon />
            </Badge>
          }>
            Basket
          </Button>
        </Toolbar>
      </AppBar>

      <Container component="main" sx={{ flexGrow: 1, py: 4 }}>
        <Outlet />
      </Container>

      <Box component="footer" sx={{ py: 3, textAlign: "center", bgcolor: "grey.100" }}>
        <GetTheAppBanner />
      </Box>
    </Box>
  );
}
