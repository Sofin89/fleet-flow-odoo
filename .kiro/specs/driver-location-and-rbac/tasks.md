# Implementation Plan: Driver Location & RBAC

## Overview

This implementation plan breaks down the Driver Location & RBAC feature into discrete coding tasks. The feature adds real-time driver location tracking with manual control, visual distinction for the logged-in driver on the map, and comprehensive role-based access control across five user roles (MANAGER, DISPATCHER, SAFETY_OFFICER, ANALYST, DRIVER).

The implementation follows a layered approach: database schema → backend services → backend API → frontend services → frontend UI components → integration and testing.

## Tasks

- [ ] 1. Database schema and entity setup
  - [x] 1.1 Create database migration for schema changes
    - Add columns to driver_locations table: sharing_active, accuracy, consecutive_failures, last_error
    - Create location_history table with indexes
    - Create audit_logs table with indexes
    - _Requirements: 13.2, 13.3, 7.5_
  
  - [x] 1.2 Create LocationHistory entity and repository
    - Write LocationHistory JPA entity with validation constraints
    - Create LocationHistoryRepository interface with custom query methods
    - _Requirements: 11.3, 11.4, 11.5_
  
  - [x] 1.3 Create AuditLog entity and repository
    - Write AuditLog JPA entity
    - Create AuditLogRepository interface
    - _Requirements: 7.5_
  
  - [x] 1.4 Update DriverLocation entity
    - Add new fields: sharingActive, accuracy, consecutiveFailures, lastError
    - Update validation constraints
    - _Requirements: 2.5, 15.1_

- [ ] 2. Backend RBAC infrastructure
  - [x] 2.1 Create @RoleAllowed annotation
    - Define custom annotation with String[] value parameter
    - Set retention policy to RUNTIME and target to METHOD
    - _Requirements: 7.1, 7.3_
  
  - [x] 2.2 Implement RoleBasedAccessInterceptor
    - Create HandlerInterceptor implementation
    - Extract role from SecurityContext
    - Check role against @RoleAllowed annotation
    - Return 403 Forbidden for unauthorized access
    - _Requirements: 7.1, 7.2, 7.3, 7.4_
  
  - [ ]* 2.3 Write property test for RBAC interceptor
    - **Property 18: Route and API Authorization**
    - **Validates: Requirements 7.1, 7.3**
    - Generate random roles and endpoints, verify authorization logic
  
  - [x] 2.4 Create AuditLogService
    - Implement logUnauthorizedAccess method
    - Save audit log entries to database
    - _Requirements: 7.5_
  
  - [ ]* 2.5 Write property test for audit logging
    - **Property 20: Unauthorized Access Audit Logging**
    - **Validates: Requirements 7.5**
    - Generate random unauthorized access attempts, verify all are logged
  
  - [x] 2.6 Register interceptor in WebMvcConfigurer
    - Add RoleBasedAccessInterceptor to interceptor registry
    - _Requirements: 7.1_

- [ ] 3. Backend location services
  - [x] 3.1 Create LocationWebSocketHandler
    - Implement broadcastLocationUpdate method
    - Implement broadcastSharingStatusChange method
    - Use SimpMessagingTemplate to send to /topic/locations
    - _Requirements: 1.4, 2.5, 3.2_
  
  - [ ]* 3.2 Write property test for WebSocket broadcasting
    - **Property 3: Location Update Broadcasting**
    - **Validates: Requirements 1.4, 3.2**
    - Generate random location updates, verify all are broadcast
  
  - [x] 3.3 Implement DriverLocationService core methods
    - Implement startLocationSharing method
    - Implement stopLocationSharing method
    - Implement updateLocation method with validation
    - Implement getAllActiveLocations method
    - Implement getDriverLocation method
    - _Requirements: 1.2, 2.2, 2.4, 3.1, 15.1_
  
  - [ ]* 3.4 Write property test for location storage
    - **Property 1: Location Storage with Timestamp**
    - **Validates: Requirements 1.2, 13.2**
    - Generate random valid coordinates, verify storage with timestamp
  
  - [ ]* 3.5 Write property test for location sharing state
    - **Property 4: Location Sharing State Persistence**
    - **Validates: Requirements 2.5**
    - Generate random state changes, verify persistence
  
  - [ ]* 3.6 Write property test for location sharing stop
    - **Property 6: Location Sharing Stop**
    - **Validates: Requirements 2.4, 13.4**
    - Verify sharing_active flag set to false and no new captures
  
  - [x] 3.7 Implement location history methods
    - Implement getLocationHistory with date range validation
    - Implement saveToHistory helper method
    - Enforce MAX_HISTORY_DAYS = 30 constraint
    - _Requirements: 11.4, 11.5_
  
  - [ ]* 3.8 Write property test for location history time limit
    - **Property 30: Location History Time Limit**
    - **Validates: Requirements 11.5**
    - Generate random date ranges, verify 30-day limit enforcement
  
  - [x] 3.9 Implement scheduled purge job
    - Create @Scheduled method to purge records older than 90 days
    - Run daily at 2 AM
    - Log purge operations
    - _Requirements: 13.3_
  
  - [ ]* 3.10 Write property test for location history retention
    - **Property 34: Location History Retention**
    - **Validates: Requirements 13.3**
    - Create records with various ages, verify 90-day purge

- [ ] 4. Backend API controllers
  - [-] 4.1 Create DriverLocationController with endpoints
    - POST /api/drivers/locations/start with @RoleAllowed("DRIVER")
    - POST /api/drivers/locations/stop with @RoleAllowed("DRIVER")
    - PUT /api/drivers/locations with @RoleAllowed("DRIVER")
    - GET /api/drivers/locations with @RoleAllowed for non-drivers
    - GET /api/drivers/locations/{driverId} with role check
    - GET /api/drivers/locations/{driverId}/history with role check
    - _Requirements: 1.1, 2.2, 2.4, 3.1, 3.5, 11.4_
  
  - [ ]* 4.2 Write integration tests for location endpoints
    - Test all endpoints with valid and invalid roles
    - Test authorization enforcement
    - _Requirements: 7.1, 7.2, 7.3, 7.4_
  
  - [~] 4.3 Create DTOs for location API
    - Create LocationUpdateRequest with validation annotations
    - Create DriverLocationResponse
    - Create LocationHistoryResponse
    - _Requirements: 1.2, 3.1, 11.4_
  
  - [~] 4.4 Implement GlobalExceptionHandler for location errors
    - Handle UnauthorizedException → 403
    - Handle LocationSharingNotActiveException → 400
    - Handle InvalidLocationDataException → 400
    - Handle HistoryRangeTooLargeException → 400
    - Handle WebSocketException → log but don't fail request
    - _Requirements: 1.3, 15.3_
  
  - [ ]* 4.5 Write property test for error handling
    - **Property 2: Geolocation Error Handling**
    - **Validates: Requirements 1.3, 15.3**
    - Generate random error conditions, verify proper handling

- [ ] 5. Backend trip filtering and RBAC enhancements
  - [~] 5.1 Update TripController.getTrips with role-based filtering
    - Filter to driver's trips for DRIVER role
    - Return all trips for MANAGER, DISPATCHER, SAFETY_OFFICER
    - Return all trips in read-only mode for ANALYST
    - _Requirements: 8.1, 8.2, 8.3_
  
  - [ ]* 5.2 Write property test for trip filtering
    - **Property 21: Role-Based Trip Filtering**
    - **Validates: Requirements 8.1, 8.2, 8.3**
    - Generate random roles and trip assignments, verify filtering
  
  - [~] 5.3 Add @RoleAllowed to TripController.updateTrip
    - Restrict to MANAGER and DISPATCHER only
    - _Requirements: 14.2_
  
  - [~] 5.4 Add @RoleAllowed to TripController.deleteTrip
    - Restrict to MANAGER and DISPATCHER only
    - _Requirements: 14.2_
  
  - [~] 5.5 Add driver access validation to TripController.getTripById
    - Allow drivers to view only their own trips
    - Allow other roles to view all trips
    - _Requirements: 8.5_
  
  - [ ]* 5.6 Write property test for driver trip access restriction
    - **Property 23: Driver Trip Access Restriction**
    - **Validates: Requirements 8.5**
    - Generate random trip assignments, verify access control

- [ ] 6. Checkpoint - Backend implementation complete
  - Ensure all backend tests pass
  - Verify database migrations run successfully
  - Test API endpoints with Postman or similar tool
  - Ask the user if questions arise

- [ ] 7. Frontend GeolocationService
  - [~] 7.1 Create GeolocationService class
    - Implement startTracking method with watchPosition
    - Implement stopTracking method
    - Implement getCurrentPosition method
    - Implement isSupported method
    - Implement checkPermission method
    - Configure HIGH_ACCURACY_OPTIONS with enableHighAccuracy: true
    - _Requirements: 1.1, 1.5, 2.2, 2.4, 3.5, 12.5_
  
  - [ ]* 7.2 Write property test for high accuracy configuration
    - **Property 45: High Accuracy Configuration**
    - **Validates: Requirements 1.5**
    - Verify enableHighAccuracy is always true
  
  - [~] 7.3 Implement error handling in GeolocationService
    - Handle PERMISSION_DENIED with modal display
    - Handle POSITION_UNAVAILABLE with retry and backoff
    - Handle TIMEOUT with increased timeout retry
    - Track consecutive failures
    - _Requirements: 1.3, 12.1, 12.2, 15.3_
  
  - [ ]* 7.4 Write property test for consecutive failure tracking
    - **Property 42: Consecutive Failure Auto-Stop**
    - **Validates: Requirements 15.4**
    - Simulate consecutive failures, verify auto-stop at 5 failures
  
  - [~] 7.5 Implement 30-second tracking interval
    - Use setInterval for periodic updates
    - Clear interval on stopTracking
    - _Requirements: 2.3, 3.1_
  
  - [ ]* 7.6 Write property test for location update interval
    - **Property 5: Location Update Interval**
    - **Validates: Requirements 2.3, 3.1**
    - Verify updates occur at 30-second intervals

- [ ] 8. Frontend RBAC infrastructure
  - [~] 8.1 Create RoleGuard component
    - Implement route protection with allowedRoles prop
    - Implement conditional rendering with fallback prop
    - Implement redirect logic with toast error message
    - _Requirements: 6.5, 7.2_
  
  - [ ]* 8.2 Write property test for role-based navigation
    - **Property 16: Role-Based Navigation Menu**
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4**
    - Generate random roles, verify correct menu items displayed
  
  - [ ]* 8.3 Write property test for navigation link hiding
    - **Property 17: Navigation Link Hiding**
    - **Validates: Requirements 6.5**
    - Verify unauthorized links are hidden, not disabled
  
  - [~] 8.2 Create useRoleGuard hook
    - Implement hasRole function
    - Implement hasAnyRole function
    - Implement hasAllRoles function
    - _Requirements: 6.1, 6.2, 6.3, 6.4_
  
  - [~] 8.3 Define ROLE_NAVIGATION configuration
    - Map DRIVER to Dashboard, LiveMap, My Trips
    - Map ANALYST to Dashboard, LiveMap, Reports
    - Map SAFETY_OFFICER to Dashboard, Drivers, Trips, LiveMap, Reports
    - Map MANAGER and DISPATCHER to ALL
    - _Requirements: 6.1, 6.2, 6.3, 6.4_
  
  - [~] 8.4 Update Navigation component with role-based rendering
    - Use ROLE_NAVIGATION to filter menu items
    - Hide unauthorized links completely
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 9. Frontend location API client
  - [~] 9.1 Create location API service
    - Implement startLocationSharing API call
    - Implement stopLocationSharing API call
    - Implement updateLocation API call
    - Implement getDriverLocation API call
    - Implement getLocationHistory API call
    - _Requirements: 2.2, 2.4, 3.1, 11.4_
  
  - [~] 9.2 Implement API error handling with retry logic
    - Handle 401 Unauthorized → logout
    - Handle 403 Forbidden → show error toast
    - Handle 429 Too Many Requests → show rate limit message
    - Handle 500 Server Error → retry with exponential backoff
    - Handle network errors → show connection error
    - _Requirements: 3.4, 15.3_
  
  - [ ]* 9.3 Write property test for location update retry logic
    - **Property 8: Location Update Retry Logic**
    - **Validates: Requirements 3.4**
    - Simulate failures, verify 3 retries with exponential backoff

- [ ] 10. Frontend DriverDashboard component
  - [~] 10.1 Create DriverDashboard component structure
    - Create component with location sharing status indicator
    - Add Start/Stop/Update location buttons
    - Display current coordinates and last update timestamp
    - Show today's assigned trips summary
    - Add quick link to LiveMap
    - Display statistics: total trips completed, total distance
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_
  
  - [ ]* 10.2 Write property test for dashboard content
    - **Property 24: Driver Dashboard Content**
    - **Validates: Requirements 9.2, 9.3, 9.5**
    - Generate random driver data, verify all elements displayed
  
  - [~] 10.3 Implement location sharing controls
    - Wire Start button to GeolocationService.startTracking
    - Wire Stop button to GeolocationService.stopTracking
    - Wire Update button to manual location capture
    - Update UI based on sharing state
    - _Requirements: 2.1, 2.2, 2.4, 3.5_
  
  - [ ]* 10.4 Write property test for location sharing start trigger
    - **Property 43: Location Sharing Start Trigger**
    - **Validates: Requirements 2.2**
    - Verify watchPosition is called on start button click
  
  - [ ]* 10.5 Write property test for manual location update
    - **Property 9: Manual Location Update**
    - **Validates: Requirements 3.5**
    - Verify immediate capture on Update button click
  
  - [~] 10.6 Fetch and display today's trips
    - Call trip API filtered by driver
    - Display trip summary cards
    - _Requirements: 9.2_
  
  - [~] 10.7 Fetch and display driver statistics
    - Call driver stats API
    - Display total trips and distance
    - _Requirements: 9.5_

- [ ] 11. Frontend LiveMap enhancements for Me Marker
  - [~] 11.1 Create ME_MARKER_ICON constant
    - Use red color (#e74c3c)
    - Set larger size (38px)
    - Use distinct driver SVG icon
    - _Requirements: 4.1, 4.2, 4.4_
  
  - [~] 11.2 Update getIcon function with Me Marker logic
    - Check if marker.markerType === 'DRIVER' && marker.driverId === currentUserId
    - Return ME_MARKER_ICON for current user
    - Return standard icon for other drivers
    - _Requirements: 4.1, 4.5_
  
  - [ ]* 11.3 Write property test for Me Marker visual distinction
    - **Property 10: Me Marker Visual Distinction**
    - **Validates: Requirements 4.1, 4.2, 4.4**
    - Verify unique styling for current user's marker
  
  - [ ]* 11.4 Write property test for standard marker styling
    - **Property 12: Standard Marker Styling for Non-Drivers**
    - **Validates: Requirements 4.5**
    - Verify non-drivers see standard styling for all markers
  
  - [~] 11.5 Update marker popup with "You are here" text
    - Add conditional text for Me Marker
    - _Requirements: 4.3_
  
  - [ ]* 11.6 Write property test for Me Marker popup text
    - **Property 11: Me Marker Popup Text**
    - **Validates: Requirements 4.3**
    - Verify "You are here" text appears for current user
  
  - [~] 11.7 Add accuracy warning display to marker popup
    - Show warning icon and accuracy value when > 100m
    - Style with yellow background
    - _Requirements: 15.2_
  
  - [ ]* 11.8 Write property test for accuracy display
    - **Property 40: Accuracy Recording and Display**
    - **Validates: Requirements 15.1, 15.5**
    - Generate random accuracy values, verify display
  
  - [ ]* 11.9 Write property test for low accuracy warning
    - **Property 41: Low Accuracy Warning**
    - **Validates: Requirements 15.2**
    - Verify warning appears when accuracy > 100m

- [ ] 12. Frontend LiveMap auto-centering
  - [~] 12.1 Create AutoCenterOnDriver component
    - Use useMap hook to access Leaflet map instance
    - Track hasManuallyPanned state
    - Center map on driver marker when not manually panned
    - Set zoom level to 14
    - _Requirements: 5.1, 5.2_
  
  - [ ]* 12.2 Write property test for driver map auto-centering
    - **Property 13: Driver Map Auto-Centering**
    - **Validates: Requirements 5.1, 5.2**
    - Verify map centers on Me Marker at zoom 14
  
  - [~] 12.3 Implement manual pan detection
    - Listen to map 'dragstart' event
    - Set hasManuallyPanned to true on drag
    - Disable auto-centering after manual pan
    - _Requirements: 5.5_
  
  - [ ]* 12.4 Write property test for manual pan disables auto-center
    - **Property 15: Manual Pan Disables Auto-Center**
    - **Validates: Requirements 5.5**
    - Simulate manual pan, verify no auto-centering on updates
  
  - [~] 12.5 Implement default centering for non-drivers
    - Check user role
    - Center on default fleet location for non-drivers
    - _Requirements: 5.4_
  
  - [ ]* 12.6 Write property test for non-driver default centering
    - **Property 14: Non-Driver Default Centering**
    - **Validates: Requirements 5.4**
    - Verify non-drivers see default fleet location
  
  - [~] 12.7 Handle location unavailable fallback
    - Center on default location if driver location not available
    - _Requirements: 5.3_

- [ ] 13. Frontend WebSocket integration for real-time updates
  - [~] 13.1 Subscribe to /topic/locations in LiveMap
    - Connect to WebSocket on component mount
    - Subscribe to location update events
    - Subscribe to sharing status change events
    - _Requirements: 3.2, 3.3_
  
  - [~] 13.2 Implement location update handler
    - Update marker position in state
    - Trigger map re-render
    - _Requirements: 3.3_
  
  - [ ]* 13.3 Write property test for marker position update
    - **Property 7: Marker Position Update**
    - **Validates: Requirements 3.3**
    - Generate random location updates, verify marker moves
  
  - [ ]* 13.4 Write property test for WebSocket marker update timing
    - **Property 44: WebSocket Marker Update**
    - **Validates: Requirements 3.3**
    - Verify updates received within 1 second
  
  - [~] 13.3 Implement sharing status change handler
    - Update driver marker visibility based on sharing_active
    - Show/hide marker when sharing starts/stops
    - _Requirements: 2.5_

- [ ] 14. Frontend trip assignment notifications
  - [~] 14.1 Create WebSocket subscription for trip assignments
    - Subscribe to /topic/trip-assignments/{driverId}
    - _Requirements: 10.1_
  
  - [~] 14.2 Implement notification toast display
    - Show toast with trip details on assignment
    - Add click handler to navigate to trip details
    - _Requirements: 10.2, 10.3_
  
  - [ ]* 14.3 Write property test for trip assignment notification
    - **Property 25: Trip Assignment Notification**
    - **Validates: Requirements 10.1, 10.2**
    - Generate random trip assignments, verify notifications
  
  - [ ]* 14.4 Write property test for notification navigation
    - **Property 26: Notification Navigation**
    - **Validates: Requirements 10.3**
    - Verify clicking notification navigates to trip details
  
  - [~] 14.5 Implement offline notification queue
    - Store notifications in localStorage when offline
    - Display queued notifications on next login
    - _Requirements: 10.4_
  
  - [ ]* 14.6 Write property test for offline notification delivery
    - **Property 27: Offline Notification Delivery**
    - **Validates: Requirements 10.4**
    - Simulate offline state, verify notifications queued
  
  - [~] 14.7 Implement notification read status
    - Mark notification as read when trip details viewed
    - Update notification badge count
    - _Requirements: 10.5_
  
  - [ ]* 14.8 Write property test for notification read status
    - **Property 28: Notification Read Status**
    - **Validates: Requirements 10.5**
    - Verify notifications marked read on trip view

- [ ] 15. Frontend driver profile page
  - [~] 15.1 Create DriverProfile component
    - Display personal information and role
    - Show list of completed trips with dates and distances
    - Display location sharing statistics
    - Show timeline of location updates for current day
    - _Requirements: 11.1, 11.2, 11.3, 11.4_
  
  - [ ]* 15.2 Write property test for driver profile content
    - **Property 29: Driver Profile Content**
    - **Validates: Requirements 11.2, 11.3, 11.4**
    - Generate random driver data, verify all elements displayed
  
  - [~] 15.3 Implement location history date range selector
    - Add date picker for start and end dates
    - Enforce 30-day maximum range
    - Display error if range exceeds limit
    - _Requirements: 11.5_
  
  - [~] 15.4 Fetch and display location history
    - Call getLocationHistory API with date range
    - Display timeline visualization
    - _Requirements: 11.4_

- [ ] 16. Frontend geolocation permission handling
  - [~] 16.1 Create PermissionModal component
    - Display explanation of why location access is needed
    - Show browser-specific instructions for enabling permissions
    - Add close button
    - _Requirements: 12.1, 12.2_
  
  - [~] 16.2 Implement permission check on driver login
    - Call GeolocationService.checkPermission on mount
    - Display appropriate messaging based on status
    - _Requirements: 12.5_
  
  - [ ]* 16.3 Write property test for permission denial handling
    - **Property 31: Permission Denial Handling**
    - **Validates: Requirements 12.1, 12.2, 12.3**
    - Simulate permission denial, verify modal and button state
  
  - [ ]* 16.4 Write property test for permission grant handling
    - **Property 32: Permission Grant Handling**
    - **Validates: Requirements 12.4**
    - Simulate permission grant, verify features enabled
  
  - [ ]* 16.5 Write property test for login permission check
    - **Property 33: Login Permission Check**
    - **Validates: Requirements 12.5**
    - Verify permission check occurs on driver login
  
  - [~] 16.3 Update DriverDashboard with permission-based UI
    - Disable Start button when permission denied
    - Enable all features when permission granted
    - _Requirements: 12.3, 12.4_

- [ ] 17. Frontend ANALYST read-only enforcement
  - [~] 17.1 Update all data pages with ANALYST role checks
    - Hide edit, delete, create buttons for ANALYST
    - Show only read and export functionality
    - _Requirements: 14.1_
  
  - [ ]* 17.2 Write property test for ANALYST UI read-only mode
    - **Property 35: ANALYST UI Read-Only Mode**
    - **Validates: Requirements 14.1**
    - Verify action buttons hidden for ANALYST
  
  - [~] 17.2 Update LiveMap with ANALYST role checks
    - Disable control actions for ANALYST
    - Display all markers in read-only mode
    - _Requirements: 14.3_
  
  - [ ]* 17.3 Write property test for ANALYST map read-only
    - **Property 37: ANALYST Map Read-Only**
    - **Validates: Requirements 14.3**
    - Verify controls disabled for ANALYST
  
  - [~] 17.3 Update Reports page with ANALYST role checks
    - Allow data export for ANALYST
    - Prevent modification of report configurations
    - _Requirements: 14.4_
  
  - [ ]* 17.4 Write property test for ANALYST report permissions
    - **Property 38: ANALYST Report Permissions**
    - **Validates: Requirements 14.4**
    - Verify export allowed, modification prevented

- [ ] 18. Checkpoint - Frontend implementation complete
  - Ensure all frontend tests pass
  - Verify UI renders correctly for all roles
  - Test location sharing flow end-to-end
  - Ask the user if questions arise

- [ ] 19. Integration and end-to-end testing
  - [ ] 19.1 Write E2E test for driver location sharing flow
    - Login as driver
    - Start location sharing
    - Verify location appears on map
    - Stop location sharing
    - Verify location stops updating
    - _Requirements: 1.1, 2.2, 2.4, 3.1, 3.3_
  
  - [ ] 19.2 Write E2E test for role-based navigation
    - Login as each role
    - Verify correct navigation menu items
    - Attempt to access unauthorized routes
    - Verify redirects and error messages
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 7.2_
  
  - [ ] 19.3 Write E2E test for Me Marker visual distinction
    - Login as driver
    - Open LiveMap
    - Verify Me Marker has unique styling
    - Verify map centers on Me Marker
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 5.1, 5.2_
  
  - [ ] 19.4 Write E2E test for trip filtering
    - Login as driver
    - Verify only assigned trips visible
    - Login as manager
    - Verify all trips visible
    - _Requirements: 8.1, 8.2_
  
  - [ ] 19.5 Write E2E test for trip assignment notification
    - Login as driver
    - Assign trip to driver (via admin action)
    - Verify notification appears
    - Click notification
    - Verify navigation to trip details
    - _Requirements: 10.1, 10.2, 10.3_
  
  - [ ] 19.6 Write E2E test for ANALYST read-only access
    - Login as ANALYST
    - Verify no edit/delete buttons on data pages
    - Attempt API modification via browser console
    - Verify 403 Forbidden response
    - _Requirements: 14.1, 14.2, 14.3, 14.5_

- [ ] 20. Backend property-based test implementation
  - [ ]* 20.1 Write property test for unauthorized access response
    - **Property 19: Unauthorized Access Response**
    - **Validates: Requirements 7.2, 7.4**
    - Generate random unauthorized requests, verify 403 responses
  
  - [ ]* 20.2 Write property test for trip detail content
    - **Property 22: Trip Detail Content**
    - **Validates: Requirements 8.4**
    - Generate random trips, verify all details displayed
  
  - [ ]* 20.3 Write property test for permission grant handling
    - **Property 32: Permission Grant Handling**
    - **Validates: Requirements 12.4**
    - Verify all features enabled when permission granted
  
  - [ ]* 20.4 Write property test for ANALYST API write restriction
    - **Property 36: ANALYST API Write Restriction**
    - **Validates: Requirements 14.2**
    - Generate random write requests from ANALYST, verify 403
  
  - [ ]* 20.5 Write property test for dual-layer read-only enforcement
    - **Property 39: Dual-Layer Read-Only Enforcement**
    - **Validates: Requirements 14.5**
    - Verify both frontend and backend enforce ANALYST restrictions

- [ ] 21. Final checkpoint and documentation
  - Ensure all tests pass (unit, property-based, integration, E2E)
  - Verify all 45 correctness properties have corresponding tests
  - Run full test suite and fix any failures
  - Verify location data privacy requirements (HTTPS, 90-day retention)
  - Test with real browser geolocation on mobile and desktop
  - Ask the user if questions arise

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property-based tests validate universal correctness properties from the design document
- Unit tests and integration tests validate specific examples and edge cases
- Checkpoints ensure incremental validation and provide opportunities for user feedback
- Backend uses Java with Spring Boot, jqwik for property-based testing
- Frontend uses JavaScript/TypeScript with React, fast-check for property-based testing
- All property tests must run minimum 100 iterations due to randomization
- WebSocket broadcasting failures should not fail location update requests
- Location history is limited to 30-day queries and 90-day retention
- ANALYST role has read-only access enforced at both frontend and backend layers
