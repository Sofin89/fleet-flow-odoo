# Bugfix Requirements Document

## Introduction

The FleetFlow map feature currently has three critical production-readiness issues that prevent it from functioning as a complete fleet tracking solution:

1. **Trip Update Limitation**: Users cannot modify trip details (origin, destination, cargo weight, revenue) after creation, forcing them to cancel and recreate trips for any corrections
2. **Route Display Deficiency**: Map routes display as straight lines between origin and destination instead of following actual road networks, making it impossible to accurately track driver progress and provide realistic ETAs
3. **Trip Creation UX Limitation**: Users can only enter origin and destination as text coordinates, lacking an intuitive visual map-based selection method for choosing locations

These issues significantly impact operational usability and the accuracy of fleet tracking information displayed to dispatchers and managers.

## Bug Analysis

### Current Behavior (Defect)

**Trip Update Issues:**

1.1 WHEN a user attempts to update trip details (origin, destination, cargo weight, or revenue) for an existing trip in DRAFT status THEN the system provides no update functionality, forcing trip cancellation and recreation

1.2 WHEN a trip is created with incorrect origin or destination information THEN the system cannot correct the route information, causing map markers and route displays to show wrong locations

1.3 WHEN cargo weight or revenue needs adjustment before dispatch THEN the system requires canceling the trip and losing the trip ID and creation timestamp

**Trip Creation UX Issues:**

1.4 WHEN a user creates a new trip THEN the system only accepts text-based coordinate input for origin and destination, requiring users to manually look up or guess coordinates

1.5 WHEN a user needs to specify a pickup or delivery location THEN the system provides no visual map interface to point and click on the desired location

1.6 WHEN a user enters coordinates as text THEN the system provides no visual confirmation of where that location appears on the map before trip creation

**Route Display Issues:**

1.7 WHEN a trip is dispatched and displayed on the map THEN the system shows a straight line between origin and destination instead of following actual roads

1.8 WHEN OSRM route fetching fails or times out THEN the system falls back to straight-line display without retry or error indication to the user

1.9 WHEN calculating trip progress percentage and remaining distance THEN the system uses straight-line distance calculations, resulting in inaccurate progress indicators and ETAs

1.10 WHEN multiple trips share the same origin-destination pair THEN the system may display inconsistent routes (some with OSRM data, some with straight lines) due to race conditions in route fetching

### Expected Behavior (Correct)

**Trip Update Functionality:**

2.1 WHEN a user updates trip details for a trip in DRAFT status THEN the system SHALL save the updated origin, destination, cargo weight, and revenue without changing the trip ID or creation timestamp

2.2 WHEN a user attempts to update a trip in DISPATCHED, COMPLETED, or CANCELLED status THEN the system SHALL reject the update with a clear error message indicating that only DRAFT trips can be modified

2.3 WHEN trip origin or destination is updated THEN the system SHALL immediately reflect the new route on the map with updated markers and route polylines

2.4 WHEN cargo weight is updated THEN the system SHALL validate against vehicle capacity and reject updates that exceed the maximum load capacity

**Trip Creation UX Functionality:**

2.5 WHEN a user creates a new trip THEN the system SHALL provide an interactive map interface where users can click to select origin and destination points

2.6 WHEN a user clicks on the map to select a location THEN the system SHALL place a marker at that position and capture the coordinates automatically

2.7 WHEN a user selects origin or destination via map click THEN the system SHALL allow the user to enter a custom name/label for that location (e.g., "Warehouse A", "Customer Site - Downtown")

2.8 WHEN a user has selected both origin and destination on the map THEN the system SHALL display a preview of the route before trip creation is finalized

2.9 WHEN a user wants to adjust a selected location THEN the system SHALL allow clicking again to reposition the origin or destination marker

**Route Display Functionality:**

2.10 WHEN a trip is displayed on the map THEN the system SHALL show routes following actual road networks using OSRM routing data

2.11 WHEN OSRM route fetching fails THEN the system SHALL retry the request up to 3 times with exponential backoff before falling back to straight-line display

2.12 WHEN OSRM route fetching ultimately fails after retries THEN the system SHALL display a visual indicator on the map showing that the route is approximate (straight-line) rather than road-following

2.13 WHEN calculating trip progress and remaining distance THEN the system SHALL use the actual road-following route distance, not straight-line distance

2.14 WHEN multiple trips share the same origin-destination pair THEN the system SHALL cache and reuse OSRM route data to ensure consistent display and reduce API calls

### Unchanged Behavior (Regression Prevention)

**Trip Management:**

3.1 WHEN a trip is created with valid data THEN the system SHALL CONTINUE TO validate vehicle availability, driver duty status, license validity, and cargo capacity

3.2 WHEN a trip is dispatched THEN the system SHALL CONTINUE TO update vehicle status to ON_TRIP and maintain all existing dispatch logic

3.3 WHEN a trip is completed THEN the system SHALL CONTINUE TO update odometer readings, calculate revenue, and reset vehicle/driver status

3.4 WHEN a trip is cancelled THEN the system SHALL CONTINUE TO reset vehicle and driver status appropriately

**Trip Creation:**

3.11 WHEN a user creates a trip with text-based coordinate input (legacy method) THEN the system SHALL CONTINUE TO accept and validate coordinate strings in the existing format

3.12 WHEN trip creation validation runs THEN the system SHALL CONTINUE TO check vehicle availability, driver duty status, license validity, and cargo capacity regardless of whether coordinates were entered via text or map selection

**Map Display:**

3.13 WHEN displaying driver-only markers (not on trip) THEN the system SHALL CONTINUE TO show their current location with appropriate icons and status

3.14 WHEN displaying vehicle-only markers (not on trip) THEN the system SHALL CONTINUE TO show their location with status-based icons (IDLE, IN_SHOP)

3.15 WHEN displaying combined markers (driver + vehicle on trip) THEN the system SHALL CONTINUE TO show real-time position updates every 5 seconds

3.16 WHEN users filter or search map markers THEN the system SHALL CONTINUE TO apply filters correctly without affecting route display

3.17 WHEN users toggle route visibility THEN the system SHALL CONTINUE TO show/hide route polylines without affecting marker display

3.18 WHEN map markers are clicked THEN the system SHALL CONTINUE TO display detailed popup information with trip progress, speed, and ETA
