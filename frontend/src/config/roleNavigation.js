import {
  LayoutDashboard,
  Truck,
  Users,
  Navigation,
  Wrench,
  Fuel,
  BarChart3,
  MapPin,
  UserCircle
} from 'lucide-react';

/**
 * ROLE_NAVIGATION Configuration
 * 
 * Maps user roles to their authorized navigation menu items.
 * Defines which features each role can access in the application.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4
 */

// All available menu items
export const ALL_MENU_ITEMS = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/vehicles', label: 'Vehicles', icon: Truck },
  { path: '/drivers', label: 'Drivers', icon: Users },
  { path: '/live-map', label: 'Live Map', icon: MapPin },
  { path: '/trips', label: 'Trips', icon: Navigation },
  { path: '/maintenance', label: 'Maintenance', icon: Wrench },
  { path: '/fuel-logs', label: 'Fuel & Expenses', icon: Fuel },
  { path: '/reports', label: 'Reports', icon: BarChart3 }
];

/**
 * Role-based navigation configuration
 * 
 * DRIVER: Dashboard, Live Map, My Trips
 * ANALYST: Dashboard, Live Map, Reports
 * SAFETY_OFFICER: Dashboard, Drivers, Trips, Live Map, Reports
 * MANAGER: All features
 * DISPATCHER: All features
 */
export const ROLE_NAVIGATION = {
  DRIVER: [
    { path: '/', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/live-map', label: 'Live Map', icon: MapPin },
    { path: '/trips', label: 'My Trips', icon: Navigation },
    { path: '/profile', label: 'My Profile', icon: UserCircle }
  ],
  
  ANALYST: [
    { path: '/', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/live-map', label: 'Live Map', icon: MapPin },
    { path: '/reports', label: 'Reports', icon: BarChart3 }
  ],
  
  SAFETY_OFFICER: [
    { path: '/', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/drivers', label: 'Drivers', icon: Users },
    { path: '/trips', label: 'Trips', icon: Navigation },
    { path: '/live-map', label: 'Live Map', icon: MapPin },
    { path: '/reports', label: 'Reports', icon: BarChart3 }
  ],
  
  MANAGER: 'ALL',
  DISPATCHER: 'ALL'
};

/**
 * Get navigation items for a specific role
 * @param {string} role - User role
 * @returns {Array} Array of navigation items for the role
 */
export function getNavigationForRole(role) {
  if (!role) return [];
  
  const navigation = ROLE_NAVIGATION[role];
  
  // If role has access to all items, return all
  if (navigation === 'ALL') {
    return ALL_MENU_ITEMS;
  }
  
  // Otherwise return role-specific items
  return navigation || [];
}

/**
 * Check if a role has access to a specific path
 * @param {string} role - User role
 * @param {string} path - Path to check
 * @returns {boolean} True if role has access to the path
 */
export function hasAccessToPath(role, path) {
  const navigation = getNavigationForRole(role);
  return navigation.some(item => item.path === path);
}
