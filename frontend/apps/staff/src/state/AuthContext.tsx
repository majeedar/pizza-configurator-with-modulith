import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import * as authApi from "../api/auth";
import type { StaffLoginResponse, StaffRole } from "../api/types";

interface AuthState {
  employeeId: string | null;
  username: string | null;
  displayName: string | null;
  role: StaffRole | null;
  token: string | null;
}

interface AuthContextValue extends AuthState {
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<StaffLoginResponse>;
  logout: () => void;
}

const STORAGE_KEY = "staff-web.auth";

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const EMPTY_STATE: AuthState = { employeeId: null, username: null, displayName: null, role: null, token: null };

function loadStoredAuth(): AuthState {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return EMPTY_STATE;
  }
  try {
    return JSON.parse(raw) as AuthState;
  } catch {
    return EMPTY_STATE;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(loadStoredAuth);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      isAuthenticated: state.token !== null,
      login: async (username, password) => {
        const result = await authApi.login(username, password);
        const next: AuthState = {
          employeeId: result.employeeId,
          username: result.username,
          displayName: result.displayName,
          role: result.role,
          token: result.token,
        };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        setState(next);
        return result;
      },
      logout: () => {
        localStorage.removeItem(STORAGE_KEY);
        setState(EMPTY_STATE);
      },
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
