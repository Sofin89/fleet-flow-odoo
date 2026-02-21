import { useAuth } from '../contexts/AuthContext';

/**
 * useRoleGuard Hook
 * 
 * Provides utility functions for role-based access control checks.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4
 * 
 * @returns {Object} Object containing role check functions
 */
export function useRoleGuard() {
  const { user } = useAuth();

  /**
   * Check if user has a specific role
   * @param {string} role - Role to check
   * @returns {boolean} True if user has the specified role
   */
  const hasRole = (role) => {
    if (!user) return false;
    return user.role === role;
  };

  /**
   * Check if user has any of the specified roles
   * @param {string[]} roles - Array of roles to check
   * @returns {boolean} True if user has any of the specified roles
   */
  const hasAnyRole = (roles) => {
    if (!user) return false;
    return roles.includes(user.role);
  };

  /**
   * Check if user has all of the specified roles
   * Note: In this system, a user can only have one role, so this will only
   * return true if the roles array contains exactly the user's role
   * @param {string[]} roles - Array of roles to check
   * @returns {boolean} True if user has all of the specified roles
   */
  const hasAllRoles = (roles) => {
    if (!user) return false;
    // Since users have only one role, check if that role is in the array
    // and the array has only one element
    return roles.length === 1 && roles.includes(user.role);
  };

  return {
    hasRole,
    hasAnyRole,
    hasAllRoles,
    user
  };
}
