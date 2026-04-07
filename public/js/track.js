const socket = io();

<<<<<<< HEAD
// Map Initialization
const map = L.map('map', { zoomControl: false }).setView([0, 0], 2);

// CartoDB Light theme
L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
    maxZoom: 19
}).addTo(map);

const markers = {};

// Polylines styling
const historyPolyline = L.polyline([], { color: '#007AFF', weight: 3, opacity: 0.5 }).addTo(map);
const historyPoints = [];

let routePolyline = L.polyline([], { color: '#FF3B30', weight: 2, dashArray: '5, 8' }).addTo(map);
let destinationMarker = null;
let lastKnownPosition = null;

// Apple-style custom marker
const appleMarkerHtml = `<div class="custom-user-marker"></div>`;
const customIcon = L.divIcon({
    html: appleMarkerHtml,
    className: '', 
    iconSize: [20, 20],
    iconAnchor: [10, 10]
});

// Destination Icon (Default Leaflet or simple red)
const destIcon = new L.Icon.Default(); 
// Leaflet uses default icon if we don't pass an icon object, but passing default makes it explicit.

// UI Elements
const liveDot = document.getElementById("live-dot");
const statusTitle = document.getElementById("status-title");
const lastUpdated = document.getElementById("last-updated");
const etaRow = document.getElementById("eta-row");
const etaVal = document.getElementById("eta-val");
const distVal = document.getElementById("dist-val");
const etaDivider = document.getElementById("eta-divider");
const destHint = document.getElementById("dest-hint");
const centerBtn = document.getElementById("center-user-btn");

let isFollowing = false;
let followingTimeout = null;

// Join Session
socket.emit("join-session", SESSION_ID);

centerBtn.addEventListener("click", () => {
    isFollowing = true;
    centerBtn.innerText = "Following...";
    centerBtn.style.color = "#000000";

    if (lastKnownPosition) {
        map.setView([lastKnownPosition.lat, lastKnownPosition.lng], 16, { animate: true });
    }
    
    if (followingTimeout) clearTimeout(followingTimeout);
    
    followingTimeout = setTimeout(() => {
        isFollowing = false;
        centerBtn.innerText = "Center on User";
        centerBtn.style.color = "var(--accent-color)";
    }, 5000);
});

// Set Destination
map.on('click', function (e) {
    const { lat, lng } = e.latlng;

=======
// Initialize Map
const map = L.map('map').setView([0, 0], 2);

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors'
}).addTo(map);

const markers = {};
// Path Traveled (History)
const historyPolyline = L.polyline([], { color: 'blue', weight: 4 }).addTo(map);
const historyPoints = [];

// Route to Destination (Future)
let routePolyline = L.polyline([], { color: 'red', dashArray: '5, 10', weight: 4 }).addTo(map);
let destinationMarker = null;
let lastKnownPosition = null;

// Icon
const customIcon = L.icon({
    iconUrl: 'https://cdn-icons-png.flaticon.com/512/684/684908.png',
    iconSize: [30, 30],
    iconAnchor: [15, 30],
});

const destIcon = L.icon({
    iconUrl: 'https://cdn-icons-png.flaticon.com/512/854/854878.png',
    iconSize: [30, 30],
    iconAnchor: [15, 30],
});

// Join the session
socket.emit("join-session", SESSION_ID);

const statusEl = document.getElementById("status");
const etaEl = document.getElementById("eta");
const etaVal = document.getElementById("eta-val");

// Handle Map Click to Set Destination
map.on('click', function (e) {
    const { lat, lng } = e.latlng;

    // Update/Add Destination Marker
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
    if (destinationMarker) {
        destinationMarker.setLatLng(e.latlng);
    } else {
        destinationMarker = L.marker(e.latlng, { icon: destIcon }).addTo(map);
<<<<<<< HEAD
        destHint.style.display = "none";
        etaDivider.style.display = "block";
    }

=======
        alert("Destination set! ETA will update on next location received.");
    }

    // Recalculate if we already have user location
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
    if (lastKnownPosition) {
        getRouteAndEta(lastKnownPosition.lat, lastKnownPosition.lng, lat, lng);
    }
});

socket.on("receive-location", (data) => {
    const { id, latitude, longitude } = data;

    lastKnownPosition = { lat: latitude, lng: longitude };

<<<<<<< HEAD
    // Enable Center Button First Time
    if (centerBtn.disabled) centerBtn.disabled = false;

    // Follow Mode
    if (isFollowing) {
        map.setView([latitude, longitude], map.getZoom(), { animate: true });
    }
=======
    // Update Map Center (Optional: only if following?)
    // map.setView([latitude, longitude], 16); // Only center initially or if requested
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb

    // Update Marker
    if (markers[id]) {
        markers[id].setLatLng([latitude, longitude]);
    } else {
        markers[id] = L.marker([latitude, longitude], { icon: customIcon }).addTo(map);
<<<<<<< HEAD
        if (!destinationMarker) {
            map.setView([latitude, longitude], 16);
        }
    }

    // Add Path History
    historyPoints.push([latitude, longitude]);
    historyPolyline.setLatLngs(historyPoints);

    // Update UI Status
    liveDot.style.display = "block";
    statusTitle.innerText = "Tracking";
    lastUpdated.innerText = "Updated just now";

    // Recalculate Route
=======
        map.setView([latitude, longitude], 16); // Center on first update
    }

    // Add to history path
    const latLng = [latitude, longitude];
    historyPoints.push(latLng);
    historyPolyline.setLatLngs(historyPoints);

    statusEl.innerText = `Updating: ${new Date().toLocaleTimeString()}`;
    statusEl.style.color = "green";

    // Recalculate Route if destination is set
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
    if (destinationMarker) {
        const destLatLng = destinationMarker.getLatLng();
        getRouteAndEta(latitude, longitude, destLatLng.lat, destLatLng.lng);
    }
});

socket.on("sharing-stopped", () => {
<<<<<<< HEAD
    liveDot.style.display = "none";
    statusTitle.innerText = "User stopped sharing";
    statusTitle.style.color = "var(--text-secondary)";
    lastUpdated.innerText = "--";
=======
    statusEl.innerText = "User stopped sharing.";
    statusEl.style.color = "red";
    // alert("The user has stopped sharing their location.");
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
});

socket.on("user-disconnected", (id) => {
    if (markers[id]) {
        map.removeLayer(markers[id]);
        delete markers[id];
    }
<<<<<<< HEAD
    // Assume primary sharer disconnected for now
    liveDot.style.display = "none";
    statusTitle.innerText = "User disconnected";
    lastUpdated.innerText = "--";
});

async function getRouteAndEta(startLat, startLng, destLat, destLng) {
=======
});

async function getRouteAndEta(startLat, startLng, destLat, destLng) {
    // OSRM expects: longitude,latitude
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
    const url = `https://router.project-osrm.org/route/v1/driving/${startLng},${startLat};${destLng},${destLat}?overview=full&geometries=geojson`;

    try {
        const response = await fetch(url);
        const data = await response.json();

        if (data.routes && data.routes.length > 0) {
            const route = data.routes[0];
<<<<<<< HEAD
            const coordinates = route.geometry.coordinates;

            const latLngs = coordinates.map(coord => [coord[1], coord[0]]);
            routePolyline.setLatLngs(latLngs);

            const durationSeconds = route.duration;
            const minutes = Math.ceil(durationSeconds / 60);
            
            const distanceMeters = route.distance;
            const distanceKm = (distanceMeters / 1000).toFixed(1);

            etaRow.classList.remove("hidden");
            
            // Pop Animation
            const el1 = document.getElementById("eta-block");
            const el2 = document.getElementById("dist-block");
            
            if (etaVal.innerText !== minutes.toString() || distVal.innerText !== distanceKm.toString()) {
                el1.classList.add("pop");
                el2.classList.add("pop");
                setTimeout(() => {
                    el1.classList.remove("pop");
                    el2.classList.remove("pop");
                }, 280);
            }

            etaVal.innerText = minutes;
            distVal.innerText = distanceKm;
=======
            const coordinates = route.geometry.coordinates; // [lng, lat]

            // Convert to Leaflet [lat, lng]
            const latLngs = coordinates.map(coord => [coord[1], coord[0]]);
            routePolyline.setLatLngs(latLngs);

            // Update ETA
            const durationSeconds = route.duration;
            const minutes = Math.ceil(durationSeconds / 60);

            etaEl.classList.remove("hidden");
            etaVal.innerText = `${minutes} min`;
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
        }
    } catch (err) {
        console.error("Routing Error:", err);
    }
}
<<<<<<< HEAD

=======
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
