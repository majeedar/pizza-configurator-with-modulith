import { createTheme } from "@mui/material/styles";

// Touch-first KDS design (agent.md §8.2): larger targets, high contrast.
export const theme = createTheme({
  palette: {
    primary: { main: "#c0392b" },
    secondary: { main: "#27ae60" },
  },
  shape: { borderRadius: 10 },
  components: {
    MuiButton: {
      defaultProps: { size: "large" },
      styleOverrides: { root: { fontWeight: 600 } },
    },
  },
});
