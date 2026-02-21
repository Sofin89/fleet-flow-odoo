# Design Document: Production-Ready Map Feature

## Technical Context

### System Architecture

FleetFlow is a fleet management system with:
- **Backend**: Spring Boot (Java) REST API with JPA/Hibernate
- **Frontend**: React with Leaflet for map visualization
- **Database**: Relational database (JPA entities)
- **External Services**: OSRM (Open Source Routing Machine) for road-following routes

### Current Implementation

**Backend Components:**
- `Trip` entity: Stores trip data with origin/destination as text strings
- `TripService`: Handles trip lifecycle (create, dispatch, complete, cancel)
- `MapTrackingService`: Generates map markers with route data using hardcoded location coordinates
- `MapController`: Exposes `/api/map/markers` endpoint

**Frontend Components:**
- `Trips.jsx`: Trip management UI with text-based origin/destination input
- `LiveMap.jsx`: Real-time map visualization with Leaflet, OSRM route fetching, and marker display

**Current Limitations:**
1. No trip update functionality after creation
2. Routes display as straight lines (OSRM fetching exists but has no retry/fallback handling)
3. Trip creation uses text input only (no map-based location selection)

### Affected Files

**Backend:**
- `backend/src/main/java/com/fleetflow/entity/Trip.java`
- `backend/src/main/java/com/fleetflow/dto/TripRequest.java`
- `backend/src/main/java/com/fleetflow/dto/TripResponse.java`
- `backend/src/main/java/com/fleetflow/controller/TripController.java`
- `backend/src/main/java/com/fleetflow/service/TripService.java`
- `backend/src/main/java/com/fleetflow/service/MapTrackingService.java`

**Frontend:**
- `frontend/src/pages/Trips.jsx`
- `frontend/src/pages/LiveMap.jsx`
- `frontend/src/api/index.js`

## Design Decisions

### 1. Trip Update Functionality

**Approach**: Add PUT endpoint for updating DRAFT trips only

**Rationale**:
- Only DRAFT trips should be editable (business rule: dispatched trips are immutable)
- Reuse existing validation logic from trip creation
- Maintain trip ID and creation timestamp for audit trail

**Implementation Strategy**:
- Add `updateTrip(Long id, TripRequest request)` method to `TripService`
- Add `PUT /api/trips/{id}` endpoint to `TripController`
- Validate trip status is DRAFT before allowing updates
- Re-run all business validations (vehicle availability, driver status, cargo capacity, license compatibility)

### 2. OSRM Route Display with Retry Logic

**Approach**: Implement retry mechanism with exponential backoff and visual fallback indicators

**Rationale**:
- OSRM is an external service that can fail or timeout
- Straight-line fallback is acceptable but should be clearly indicated
- Caching prevents redundant API calls for same origin-destination pairs

**Implementation Strategy**:

**Backend Changes**:
- Add `OSRMRouteService` to handle route fetching with retry logic
- Cache OSRM routes in-memory (Map<String, RouteData>) keyed by "originLat,originLng-destLat,destLng"
- Add route data to `MapMarkerDTO` (polyline coordinates, distance, isFallback flag)

**Frontend Changes**:
- Move OSRM fetching from frontend to backend for better control and caching
- Display visual indicator (dashed line + warning icon) when route is fallback
- Use actual route distance for progress calculations instead of straight-line distance

### 3. Interactive Map-Based Location Selection

**Approach**: Add map picker modal to trip creation form

**Rationale**:
- Visual selection is more intuitive than text coordinate input
- Allows users to see exact location before creating trip
- Maintains backward compatibility with text input

**Implementation Strategy**:

**Backend Changes**:
- Add `originName` and `destinationName` fields to `Trip` entity (nullable, for custom labels)
- Update `TripRequest` and `TripResponse` DTOs to include name fields
- Store coordinates in existing `origin` and `destination` fields (format: "lat,lng")

**Frontend Changes**:
- Add "Select on Map" button in trip creation modal
- Create `MapLocationPicker` component:
  - Full-screen map modal
  - Click to place origin marker (green pin)
  - Click to place destination marker (red pin)
  - Input fields for custom location names
  - Preview route between selected points
  - Confirm button to return coordinates to form
- Update `Trips.jsx` to handle both text and map-based input
- Display location names in trip list if available

## Implementation Details

### Backend Implementation

#### 1. Trip Update Endpoint

**TripController.java** - Add update endpoint:
```java
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<TripResponse>> update(
    @PathVariable Long id,
    @Valid @RequestBody TripRequest request) {
    return ResponseEntity.ok(ApiResponse.success("Trip updated", 
        tripService.updateTrip(id, request)));
}
```

**TripService.java** - Add update method:
```java
@Transactional
public TripResponse updateTrip(Long id, TripRequest request) {
    Trip trip = findTripById(id);
    
    // Validate: only DRAFT trips can be updated
    if (trip.getStatus() != TripStatus.DRAFT) {
        throw new BusinessException("Only DRAFT trips can be updated. Current status: " + trip.getStatus());
    }
    
    // Load and validate vehicle/driver (reuse existing validation logic)
    Vehicle vehicle = vehicleService.findVehicleById(request.getVehicleId());
    Driver driver = driverService.findDriverById(request.getDriverId());
    
    // Run all business validations (same as createTrip)
    validateTripBusinessRules(vehicle, driver, request);
    
    // Update trip fields
    trip.setVehicle(vehicle);
    trip.setDriver(driver);
    trip.setOrigin(request.getOrigin());
    trip.setDestination(request.getDestination());
    trip.setOriginName(request.getOriginName());
    trip.setDestinationName(request.getDestinationName());
    trip.setCargoWeightKg(request.getCargoWeightKg());
    trip.setRevenue(request.getRevenue() != null ? request.getRevenue() : BigDecimal.ZERO);
    trip.setStartOdometer(vehicle.getOdometerKm());
    
    trip = tripRepository.save(trip);
    log.info("Trip {} updated", trip.getId());
    return toResponse(trip);
}
```

**Trip.java** - Add name fields:
```java
@Column
private String originName;

@Column
private String destinationName;
```

**TripRequest.java** - Add name fields:
```java
private String originName;
private String destinationName;
```

**TripResponse.java** - Add name fields:
```java
private String originName;
private String destinationName;
```

#### 2. OSRM Route Service with Retry Logic

**Create new service: OSRMRouteService.java**:
```java
@Service
@Slf4j
public class OSRMRouteService {
    
    private final RestTemplate restTemplate;
    private final Map<String, RouteData> routeCache = new ConcurrentHashMap<>();
    
    private static final int MAX_RETRIES = 3;
    private static final int BASE_DELAY_MS = 500;
    private static final int TIMEOUT_MS = 8000;
    
    @Data
    @AllArgsConstructor
    public static class RouteData {
        private List<double[]> polyline; // [[lat, lng], ...]
        private double distanceKm;
        private boolean isFallback;
    }
    
    public RouteData getRoute(double originLat, double originLng, 
                              double destLat, double destLng) {
        String cacheKey = String.format("%.5f,%.5f-%.5f,%.5f", 
            originLat, originLng, destLat, destLng);
        
        // Check cache first
        if (routeCache.containsKey(cacheKey)) {
            return routeCache.get(cacheKey);
        }
        
        // Attempt OSRM fetch with retries
        RouteData route = fetchWithRetry(originLat, originLng, destLat, destLng);
        routeCache.put(cacheKey, route);
        return route;
    }
    
    private RouteData fetchWithRetry(double originLat, double originLng,
                                     double destLat, double destLng) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String url = String.format(
                    "https://router.project-osrm.org/route/v1/driving/%.5f,%.5f;%.5f,%.5f?overview=full&geometries=polyline",
                    originLng, originLat, destLng, destLat
                );
                
                // Fetch with timeout
                ResponseEntity<OSRMResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, OSRMResponse.class
                );
                
                if (response.getBody() != null && 
                    "Ok".equals(response.getBody().getCode()) &&
                    response.getBody().getRoutes() != null &&
                    !response.getBody().getRoutes().isEmpty()) {
                    
                    OSRMRoute route = response.getBody().getRoutes().get(0);
                    List<double[]> polyline = decodePolyline(route.getGeometry());
                    double distanceKm = route.getDistance() / 1000.0;
                    
                    log.info("OSRM route fetched successfully on attempt {}", attempt);
                    return new RouteData(polyline, distanceKm, false);
                }
            } catch (Exception e) {
                log.warn("OSRM fetch attempt {} failed: {}", attempt, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(BASE_DELAY_MS * (long) Math.pow(2, attempt - 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
        // Fallback to straight line
        log.warn("OSRM fetch failed after {} attempts, using fallback", MAX_RETRIES);
        return createFallbackRoute(originLat, originLng, destLat, destLng);
    }
    
    private RouteData createFallbackRoute(double originLat, double originLng,
                                          double destLat, double destLng) {
        List<double[]> straightLine = List.of(
            new double[]{originLat, originLng},
            new double[]{destLat, destLng}
        );
        double distance = haversine(originLat, originLng, destLat, destLng);
        return new RouteData(straightLine, distance, true);
    }
    
    private List<double[]> decodePolyline(String encoded) {
        // Polyline decoding algorithm (same as frontend)
        // ... implementation ...
    }
    
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        // Haversine distance calculation
        // ... implementation ...
    }
}
```

**Update MapMarkerDTO.java** - Add route polyline data:
```java
// Add to existing fields:
private List<double[]> routePolyline; // [[lat, lng], ...] for road-following route
private Boolean isRouteFallback; // true if using straight-line fallback
```

**Update MapTrackingService.java** - Use OSRMRouteService:
```java
@RequiredArgsConstructor
public class MapTrackingService {
    private final OSRMRouteService osrmRouteService;
    // ... existing fields ...
    
    public List<MapMarkerDTO> getAllMapMarkers() {
        // ... existing code ...
        
        // For COMBINED markers, fetch route data
        for (Trip trip : activeTrips) {
            // ... existing code ...
            
            // Fetch OSRM route
            OSRMRouteService.RouteData route = osrmRouteService.getRoute(
                originCoords[0], originCoords[1], destCoords[0], destCoords[1]
            );
            
            // Calculate progress using actual route distance
            double totalDist = route.getDistanceKm();
            double remainingDist = calculateRemainingDistance(
                dLoc.getLatitude(), dLoc.getLongitude(), 
                route.getPolyline()
            );
            double progress = Math.min(100, Math.max(0, 
                ((totalDist - remainingDist) / totalDist) * 100));
            
            markers.add(MapMarkerDTO.builder()
                // ... existing fields ...
                .routePolyline(route.getPolyline())
                .isRouteFallback(route.isFallback())
                .totalDistanceKm(totalDist)
                .remainingDistanceKm(remainingDist)
                .progressPercent(progress)
                .build());
        }
        // ... rest of method ...
    }
}
```

#### 3. Map-Based Location Selection

**No backend changes needed** - coordinates are already stored as strings in `origin`/`destination` fields. The new `originName`/`destinationName` fields provide optional labels.

### Frontend Implementation

#### 1. Trip Update UI

**Trips.jsx** - Add edit functionality:
```jsx
// Add state for editing
const [editingTrip, setEditingTrip] = useState(null);

// Add edit handler
const handleUpdate = async (e) => {
    e.preventDefault();
    try {
        await tripAPI.update(editingTrip.id, {
            ...form,
            vehicleId: parseInt(form.vehicleId),
            driverId: parseInt(form.driverId),
            cargoWeightKg: parseFloat(form.cargoWeightKg),
            revenue: form.revenue ? parseFloat(form.revenue) : null,
        });
        toast.success('Trip updated');
        setShowModal(false);
        setEditingTrip(null);
        loadTrips();
    } catch (err) {
        toast.error(err.response?.data?.message || 'Failed to update trip');
    }
};

// Add edit button in actions column (for DRAFT trips only)
{t.status === 'DRAFT' && (
    <button className="btn btn-primary btn-sm" 
            onClick={() => openEdit(t)} 
            title="Edit">
        <Edit size={14} />
    </button>
)}
```

**api/index.js** - Add update method:
```javascript
export const tripAPI = {
    // ... existing methods ...
    update: (id, data) => api.put(`/trips/${id}`, data),
};
```

#### 2. OSRM Route Display with Fallback Indicator

**LiveMap.jsx** - Update route rendering:
```jsx
// Use route polyline from backend instead of fetching in frontend
const routeLines = useMemo(() => {
    if (!showRoutes) return [];
    return filtered
        .filter(m => m.markerType === 'COMBINED' && m.routePolyline)
        .map(m => {
            const fullRoute = m.routePolyline;
            const driverPos = [m.latitude, m.longitude];
            const splitIdx = closestPointIndex(fullRoute, m.latitude, m.longitude);
            
            return {
                key: markerKey(m),
                completed: fullRoute.slice(0, splitIdx + 1),
                remaining: fullRoute.slice(splitIdx),
                snappedPos: fullRoute[splitIdx],
                originPos: fullRoute[0],
                destPos: fullRoute[fullRoute.length - 1],
                origin: m.tripOrigin,
                dest: m.tripDestination,
                isFallback: m.isRouteFallback,
            };
        });
}, [filtered, showRoutes]);

// Update polyline rendering to show fallback indicator
{routeLines.map(r => (
    <span key={`route-${r.key}`}>
        <Polyline positions={r.completed}
            pathOptions={{ 
                color: '#27ae60', 
                weight: 5, 
                opacity: r.isFallback ? 0.5 : 0.85,
                dashArray: r.isFallback ? '5, 10' : null 
            }} />
        <Polyline positions={r.remaining}
            pathOptions={{ 
                color: '#8e44ad', 
                weight: 4, 
                opacity: r.isFallback ? 0.4 : 0.6, 
                dashArray: '10, 8' 
            }} />
        {r.isFallback && (
            <Marker position={r.originPos} 
                    icon={warningIcon}>
                <Popup>
                    <div style={{ fontSize: '0.75rem', color: '#e67e22' }}>
                        ⚠️ Approximate route (straight-line)<br/>
                        Road routing unavailable
                    </div>
                </Popup>
            </Marker>
        )}
        {/* ... origin/dest markers ... */}
    </span>
))}
```

#### 3. Map Location Picker Component

**Create new component: MapLocationPicker.jsx**:
```jsx
import { useState } from 'react';
import { MapContainer, TileLayer, Marker, Polyline, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import { X, Check } from 'lucide-react';

const originIcon = L.divIcon({
    html: '<div style="font-size:24px">📍</div>',
    iconSize: [24, 24],
    iconAnchor: [12, 24],
});

const destIcon = L.divIcon({
    html: '<div style="font-size:24px">🏁</div>',
    iconSize: [24, 24],
    iconAnchor: [12, 24],
});

function MapClickHandler({ onOriginClick, onDestClick, selectingOrigin }) {
    useMapEvents({
        click: (e) => {
            const { lat, lng } = e.latlng;
            if (selectingOrigin) {
                onOriginClick(lat, lng);
            } else {
                onDestClick(lat, lng);
            }
        },
    });
    return null;
}

export default function MapLocationPicker({ onConfirm, onCancel }) {
    const [origin, setOrigin] = useState(null);
    const [destination, setDestination] = useState(null);
    const [originName, setOriginName] = useState('');
    const [destName, setDestName] = useState('');
    const [selectingOrigin, setSelectingOrigin] = useState(true);

    const handleOriginClick = (lat, lng) => {
        setOrigin({ lat, lng });
        setSelectingOrigin(false);
    };

    const handleDestClick = (lat, lng) => {
        setDestination({ lat, lng });
    };

    const handleConfirm = () => {
        if (!origin || !destination) {
            alert('Please select both origin and destination');
            return;
        }
        onConfirm({
            origin: `${origin.lat},${origin.lng}`,
            destination: `${destination.lat},${destination.lng}`,
            originName: originName || null,
            destinationName: destName || null,
        });
    };

    return (
        <div className="modal-overlay">
            <div className="modal" style={{ maxWidth: '90vw', width: 900, maxHeight: '90vh' }}>
                <div className="modal-header">
                    <h3>Select Trip Locations</h3>
                    <button className="modal-close" onClick={onCancel}>
                        <X size={20} />
                    </button>
                </div>
                <div className="modal-body" style={{ padding: 0 }}>
                    <div style={{ padding: 16, background: '#f8f9fa', borderBottom: '1px solid var(--border)' }}>
                        <div style={{ display: 'flex', gap: 12, marginBottom: 12 }}>
                            <button 
                                onClick={() => setSelectingOrigin(true)}
                                style={{
                                    padding: '8px 16px',
                                    borderRadius: 6,
                                    border: selectingOrigin ? '2px solid #27ae60' : '1px solid var(--border)',
                                    background: selectingOrigin ? '#eafaf1' : '#fff',
                                    color: selectingOrigin ? '#27ae60' : 'var(--text-600)',
                                    fontWeight: 600,
                                    cursor: 'pointer',
                                }}>
                                📍 Select Origin {origin && '✓'}
                            </button>
                            <button 
                                onClick={() => setSelectingOrigin(false)}
                                style={{
                                    padding: '8px 16px',
                                    borderRadius: 6,
                                    border: !selectingOrigin ? '2px solid #c0392b' : '1px solid var(--border)',
                                    background: !selectingOrigin ? '#fef5e7' : '#fff',
                                    color: !selectingOrigin ? '#c0392b' : 'var(--text-600)',
                                    fontWeight: 600,
                                    cursor: 'pointer',
                                }}>
                                🏁 Select Destination {destination && '✓'}
                            </button>
                        </div>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                            <div>
                                <label style={{ fontSize: '0.75rem', fontWeight: 600, marginBottom: 4, display: 'block' }}>
                                    Origin Name (optional)
                                </label>
                                <input 
                                    type="text"
                                    className="form-input"
                                    placeholder="e.g., Warehouse A"
                                    value={originName}
                                    onChange={(e) => setOriginName(e.target.value)}
                                    disabled={!origin}
                                />
                            </div>
                            <div>
                                <label style={{ fontSize: '0.75rem', fontWeight: 600, marginBottom: 4, display: 'block' }}>
                                    Destination Name (optional)
                                </label>
                                <input 
                                    type="text"
                                    className="form-input"
                                    placeholder="e.g., Customer Site"
                                    value={destName}
                                    onChange={(e) => setDestName(e.target.value)}
                                    disabled={!destination}
                                />
                            </div>
                        </div>
                    </div>
                    <MapContainer 
                        center={[23.0225, 72.5714]} 
                        zoom={12} 
                        style={{ height: 500, width: '100%' }}>
                        <TileLayer
                            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                            attribution='&copy; OpenStreetMap contributors'
                        />
                        <MapClickHandler 
                            onOriginClick={handleOriginClick}
                            onDestClick={handleDestClick}
                            selectingOrigin={selectingOrigin}
                        />
                        {origin && (
                            <Marker position={[origin.lat, origin.lng]} icon={originIcon} />
                        )}
                        {destination && (
                            <Marker position={[destination.lat, destination.lng]} icon={destIcon} />
                        )}
                        {origin && destination && (
                            <Polyline 
                                positions={[[origin.lat, origin.lng], [destination.lat, destination.lng]]}
                                pathOptions={{ color: '#3498db', weight: 3, dashArray: '10, 5' }}
                            />
                        )}
                    </MapContainer>
                </div>
                <div className="modal-footer">
                    <button className="btn btn-secondary" onClick={onCancel}>
                        Cancel
                    </button>
                    <button 
                        className="btn btn-primary" 
                        onClick={handleConfirm}
                        disabled={!origin || !destination}>
                        <Check size={16} /> Confirm Locations
                    </button>
                </div>
            </div>
        </div>
    );
}
```

**Update Trips.jsx** - Integrate map picker:
```jsx
import MapLocationPicker from '../components/MapLocationPicker';

// Add state
const [showMapPicker, setShowMapPicker] = useState(false);

// Add handler
const handleMapSelection = (locations) => {
    setForm({
        ...form,
        origin: locations.origin,
        destination: locations.destination,
        originName: locations.originName,
        destinationName: locations.destinationName,
    });
    setShowMapPicker(false);
};

// Add button in create modal (after origin/destination inputs)
<button 
    type="button"
    className="btn btn-secondary"
    onClick={() => setShowMapPicker(true)}
    style={{ marginTop: 8 }}>
    🗺️ Select on Map
</button>

// Add map picker modal
{showMapPicker && (
    <MapLocationPicker 
        onConfirm={handleMapSelection}
        onCancel={() => setShowMapPicker(false)}
    />
)}
```

## Testing Strategy

### Unit Tests

**Backend:**
1. `TripServiceTest.updateTrip()`:
   - Test updating DRAFT trip succeeds
   - Test updating DISPATCHED trip fails with BusinessException
   - Test validation errors (cargo exceeds capacity, invalid license, etc.)

2. `OSRMRouteServiceTest`:
   - Test successful route fetch
   - Test retry logic on failure
   - Test fallback route generation
   - Test caching behavior

**Frontend:**
1. `MapLocationPicker.test.jsx`:
   - Test origin selection
   - Test destination selection
   - Test name input
   - Test confirm button disabled until both locations selected

### Integration Tests

1. Trip update flow:
   - Create DRAFT trip → Update origin/destination → Verify changes persisted
   - Create DRAFT trip → Dispatch → Attempt update → Verify rejection

2. OSRM route display:
   - Create trip with known coordinates → Verify route polyline in map markers
   - Simulate OSRM failure → Verify fallback route with isFallback=true

3. Map-based trip creation:
   - Open map picker → Select locations → Confirm → Verify coordinates in form
   - Create trip with map-selected locations → Verify trip created successfully

### Manual Testing Checklist

- [ ] Update DRAFT trip origin/destination
- [ ] Attempt to update DISPATCHED trip (should fail)
- [ ] View map with OSRM routes (road-following)
- [ ] Simulate OSRM failure (network offline) → Verify fallback indicator
- [ ] Create trip using map picker
- [ ] Create trip using text input (backward compatibility)
- [ ] Verify route progress calculations use actual route distance

## Rollout Plan

### Phase 1: Backend Changes
1. Add trip update endpoint and service method
2. Implement OSRMRouteService with retry logic
3. Update MapTrackingService to use OSRMRouteService
4. Add originName/destinationName fields to Trip entity and DTOs
5. Run database migration to add new columns

### Phase 2: Frontend Changes
1. Add trip update UI in Trips.jsx
2. Update LiveMap.jsx to use backend route data and display fallback indicators
3. Create MapLocationPicker component
4. Integrate map picker into trip creation flow

### Phase 3: Testing & Validation
1. Run unit and integration tests
2. Manual testing of all three features
3. Performance testing (OSRM caching effectiveness)

### Phase 4: Deployment
1. Deploy backend changes
2. Deploy frontend changes
3. Monitor OSRM API usage and cache hit rate
4. Gather user feedback on map picker UX

## Risk Mitigation

**Risk 1**: OSRM service downtime affects all route displays
- **Mitigation**: Fallback to straight-line routes with clear visual indicator
- **Monitoring**: Log OSRM failure rate, alert if >20% of requests fail

**Risk 2**: Trip updates could violate business rules (e.g., vehicle becomes unavailable)
- **Mitigation**: Re-run all validation logic on update, same as creation
- **User Experience**: Clear error messages explaining why update failed

**Risk 3**: Map picker UX confusion (users don't understand click-to-select)
- **Mitigation**: Clear instructions, visual feedback (button highlighting), preview route
- **Fallback**: Text input remains available as alternative

**Risk 4**: Performance impact of OSRM route fetching
- **Mitigation**: Aggressive caching, async fetching, timeout limits
- **Monitoring**: Track average response time for /api/map/markers endpoint

## Success Metrics

1. **Trip Update Adoption**: % of DRAFT trips that are updated before dispatch
2. **OSRM Success Rate**: % of route requests that succeed (target: >95%)
3. **Map Picker Usage**: % of new trips created using map picker vs text input
4. **Route Display Accuracy**: User feedback on route accuracy and progress indicators
5. **API Performance**: /api/map/markers response time (target: <500ms with caching)
