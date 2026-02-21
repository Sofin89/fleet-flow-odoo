import { Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { toast } from 'react-hot-toast';
import { useEffect, useRef } from 'react';

/**
 * RoleGuard Component
 * 
 * Protects routes and conditionally renders UI elements based on user role.
 * Provides route protection with redirect logic and conditional rendering with fallback.
 * 
 * Requirements: 6.5, 7.2
 * 
 * @param {string[]} allowedRoles - Array of roles that are allowed to access the content
 * @param {React.ReactNode} children - Content to render if user has required role
 * @param {React.ReactNode} fallback - Optional fallback content to render if user doesn't have required role
 * @param {string} redirect - Path to redirect to if user doesn't have required role (default: '/dashboard')
 */
export default function RoleGuard({ allowedRoles, children, fallback = null, redirect = '/' }) {
  const { user } = useAuth();
  const hasShownToast = useRef(false);

  useEffect(() => {
    // Reset toast flag when component unmounts
    return () => {
      hasShownToast.current = false;
    };
  }, []);

  // If not authenticated, redirect to login
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // Check if user's role is in the allowed roles
  const hasAccess = allowedRoles.includes(user.role);

  if (!hasAccess) {
    // If redirect is specified, show error and redirect
    if (redirect) {
      if (!hasShownToast.current) {
        toast.error('You do not have permission to access this feature');
        hasShownToast.current = true;
      }
      return <Navigate to={redirect} replace />;
    }
    
    // Otherwise, render fallback
    return fallback;
  }

  // User has access, render children
  return children;
}
