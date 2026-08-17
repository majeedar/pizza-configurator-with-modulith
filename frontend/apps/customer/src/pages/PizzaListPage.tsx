import { useEffect, useState } from "react";
import {
  Alert,
  Card,
  CardActionArea,
  CardContent,
  CardMedia,
  CircularProgress,
  Grid,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import { fetchPizzas, resolveImageUrl } from "../api/catalog";
import type { PizzaSummary } from "../api/types";

export default function PizzaListPage() {
  const [pizzas, setPizzas] = useState<PizzaSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchPizzas()
      .then(setPizzas)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load pizzas"));
  }, []);

  if (error) {
    return <Alert severity="error">{error}</Alert>;
  }
  if (!pizzas) {
    return <CircularProgress />;
  }

  return (
    <>
      <Typography variant="h4" gutterBottom>
        Choose your pizza
      </Typography>
      <Grid container spacing={3}>
        {pizzas.map((pizza) => (
          <Grid key={pizza.pizzaId} size={{ xs: 12, sm: 6, md: 4 }}>
            <Card>
              <CardActionArea onClick={() => navigate(`/configure/${pizza.pizzaId}`)}>
                {resolveImageUrl(pizza.imageUrl) && (
                  <CardMedia
                    component="img"
                    height="160"
                    image={resolveImageUrl(pizza.imageUrl)!}
                    alt={pizza.name}
                  />
                )}
                <CardContent>
                  <Typography variant="h6">{pizza.name}</Typography>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    {pizza.description}
                  </Typography>
                  <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                    from {pizza.basePrice.toFixed(2)} €
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </>
  );
}
