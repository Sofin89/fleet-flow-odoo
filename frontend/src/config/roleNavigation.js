import {
  LayoutDashboard,
  Truck,
  Users,
  Navigation,
  Wrench,
  Fuel,
  BarChart3,
  MapPin,
  UserCircle,
  Sparkles,
  TrendingUp,
  Wallet,
  Award
} from 'lucide-react';

/**
 * ROLE_NAVIGATION Configuration
 * 
 * Maps user roles to their authorized navigation menu items.
 * Defines which features each role can access in the application.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4
 */

// AI features group (Fleet Manager & Analyst)
export const AI_FEATURES_GROUP = {
  groupLabel: 'AI features',
  icon: Sparkles,
  items: [
    { path: '/ai/predictive', label: 'Predictive Analytics', icon: TrendingUp },
    { path: '/ai/financial', label: 'Financial Intelligence', icon: Wallet },
    { path: '/ai/driver-performance', label: 'Driver Performance', icon: Award }
  ]
};

// All available menu items (flat)
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
    { path: '/reports', label: 'Reports', icon: BarChart3 },
    'AI_GROUP'
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
 * Get navigation items for a specific role.
 * Returns a mixed array: plain items { path, label, icon } and/or AI_GROUP placeholder.
 * Resolve AI_GROUP with AI_FEATURES_GROUP when rendering (Layout).
 * @param {string} role - User role
 * @returns {Array} Array of navigation items (and optional 'AI_GROUP') for the role
 */
export function getNavigationForRole(role) {
  if (!role) return [];

  const navigation = ROLE_NAVIGATION[role];

  if (navigation === 'ALL') {
    return [...ALL_MENU_ITEMS, AI_FEATURES_GROUP];
  }

  if (!navigation) return [];
  const resolved = [];
  for (const item of navigation) {
    if (item === 'AI_GROUP') {
      resolved.push(AI_FEATURES_GROUP);
    } else {
      resolved.push(item);
    }
  }
  return resolved;
}

/**
 * Check if a role has access to a specific path
 * @param {string} role - User role
 * @param {string} path - Path to check
 * @returns {boolean} True if role has access to the path
 */
export function hasAccessToPath(role, path) {
  const navigation = getNavigationForRole(role);
  return navigation.some(item => {
    if (item.groupLabel && item.items) {
      return item.items.some(sub => sub.path === path);
    }
    return item.path === path;
  });
}
