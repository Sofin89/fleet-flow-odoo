import { createContext, useContext, useState, useEffect } from 'react';
import { authAPI } from '../api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const savedUser = localStorage.getItem('fleetflow_user');
        const savedToken = localStorage.getItem('fleetflow_token');
        if (savedUser && savedToken) {
            setUser(JSON.parse(savedUser));
        }
        setLoading(false);
    }, []);

    const login = async (email, password) => {
        const res = await authAPI.login({ email, password });
        const data = res.data.data;
        localStorage.setItem('fleetflow_token', data.token);
        localStorage.setItem('fleetflow_user', JSON.stringify(data));
        setUser(data);
        return data;
    };

    const register = async (fullName, email, password, role) => {
        const res = await authAPI.register({ fullName, email, password, role });
        const data = res.data.data;
        localStorage.setItem('fleetflow_token', data.token);
        localStorage.setItem('fleetflow_user', JSON.stringify(data));
        setUser(data);
        return data;
    };

    const logout = () => {
        localStorage.removeItem('fleetflow_token');
        localStorage.removeItem('fleetflow_user');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, register, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
}

export const useAuth = () => useContext(AuthContext);
