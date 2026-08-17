import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import * as authApi from "../api/auth";

interface AuthState {
  customerId: string | null;
  name: string | null;
  email: string | null;
  token: string | null;
}

interface AuthContextValue extends AuthState {
  isGuest: boolean;
  register: (name: string, email: string, phoneNumber: string | null, password: string) => Promise<void>;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const STORAGE_KEY = "pizza-configurator.auth";

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function loadStoredAuth(): AuthState {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return { customerId: null, name: null, email: null, token: null };
  }
  try {
    return JSON.parse(raw) as AuthState;
  } catch {
    return { customerId: null, name: null, email: null, token: null };
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(loadStoredAuth);

  useEffect(() => {
    if (state.token) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  }, [state]);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      isGuest: state.token === null,
      register: async (name, email, phoneNumber, password) => {
        const result = await authApi.register(name, email, phoneNumber, password);
        setState({ customerId: result.customerId, name: result.name, email: result.email, token: result.token });
      },
      login: async (email, password) => {
        const result = await authApi.login(email, password);
        setState({ customerId: result.customerId, name: result.name, email: result.email, token: result.token });
      },
      logout: () => setState({ customerId: null, name: null, email: null, token: null }),
    }),
    [state]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
