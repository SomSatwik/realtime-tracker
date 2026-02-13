const socket = io();

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
    if (destinationMarker) {
        destinationMarker.setLatLng(e.latlng);
    } else {
        destinationMarker = L.marker(e.latlng, { icon: destIcon }).addTo(map);
        alert("Destination set! ETA will update on next location received.");
    }

    // Recalculate if we already have user location
    if (lastKnownPosition) {
        getRouteAndEta(lastKnownPosition.lat, lastKnownPosition.lng, lat, lng);
    }
});

socket.on("receive-location", (data) => {
    const { id, latitude, longitude } = data;

    lastKnownPosition = { lat: latitude, lng: longitude };

    // Update Map Center (Optional: only if following?)
    // map.setView([latitude, longitude], 16); // Only center initially or if requested

    // Update Marker
    if (markers[id]) {
        markers[id].setLatLng([latitude, longitude]);
    } else {
        markers[id] = L.marker([latitude, longitude], { icon: customIcon }).addTo(map);
        map.setView([latitude, longitude], 16); // Center on first update
    }

    // Add to history path
    const latLng = [latitude, longitude];
    historyPoints.push(latLng);
    historyPolyline.setLatLngs(historyPoints);

    statusEl.innerText = `Updating: ${new Date().toLocaleTimeString()}`;
    statusEl.style.color = "green";

    // Recalculate Route if destination is set
    if (destinationMarker) {
        const destLatLng = destinationMarker.getLatLng();
        getRouteAndEta(latitude, longitude, destLatLng.lat, destLatLng.lng);
    }
});

socket.on("sharing-stopped", () => {
    statusEl.innerText = "User stopped sharing.";
    statusEl.style.color = "red";
    // alert("The user has stopped sharing their location.");
});

socket.on("user-disconnected", (id) => {
    if (markers[id]) {
        map.removeLayer(markers[id]);
        delete markers[id];
    }
});

async function getRouteAndEta(startLat, startLng, destLat, destLng) {
    // OSRM expects: longitude,latitude
    const url = `https://router.project-osrm.org/route/v1/driving/${startLng},${startLat};${destLng},${destLat}?overview=full&geometries=geojson`;

    try {
        const response = await fetch(url);
        const data = await response.json();

        if (data.routes && data.routes.length > 0) {
            const route = data.routes[0];
            const coordinates = route.geometry.coordinates; // [lng, lat]

            // Convert to Leaflet [lat, lng]
            const latLngs = coordinates.map(coord => [coord[1], coord[0]]);
            routePolyline.setLatLngs(latLngs);

            // Update ETA
            const durationSeconds = route.duration;
            const minutes = Math.ceil(durationSeconds / 60);

            etaEl.classList.remove("hidden");
            etaVal.innerText = `${minutes} min`;
        }
    } catch (err) {
        console.error("Routing Error:", err);
    }
}
