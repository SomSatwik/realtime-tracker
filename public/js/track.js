const socket = io();

// Map Initialization
const map = L.map('map', { zoomControl: false }).setView([0, 0], 2);

// OpenStreetMap free tile layer (No API key needed)
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
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

    if (destinationMarker) {
        destinationMarker.setLatLng(e.latlng);
    } else {
        destinationMarker = L.marker(e.latlng, { icon: destIcon }).addTo(map);
        destHint.style.display = "none";
        etaDivider.style.display = "block";
    }

    if (lastKnownPosition) {
        getRouteAndEta(lastKnownPosition.lat, lastKnownPosition.lng, lat, lng);
    }
});

socket.on("receive-location", (data) => {
    const { id, latitude, longitude } = data;

    lastKnownPosition = { lat: latitude, lng: longitude };

    // Enable Center Button First Time
    if (centerBtn.disabled) centerBtn.disabled = false;

    // Follow Mode
    if (isFollowing) {
        map.setView([latitude, longitude], map.getZoom(), { animate: true });
    }

    // Update Marker
    if (markers[id]) {
        markers[id].setLatLng([latitude, longitude]);
    } else {
        markers[id] = L.marker([latitude, longitude], { icon: customIcon }).addTo(map);
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
    if (destinationMarker) {
        const destLatLng = destinationMarker.getLatLng();
        getRouteAndEta(latitude, longitude, destLatLng.lat, destLatLng.lng);
    }
});

socket.on("sharing-stopped", () => {
    liveDot.style.display = "none";
    statusTitle.innerText = "User stopped sharing";
    statusTitle.style.color = "var(--text-secondary)";
    lastUpdated.innerText = "--";
});

socket.on("user-disconnected", (id) => {
    if (markers[id]) {
        map.removeLayer(markers[id]);
        delete markers[id];
    }
    // Assume primary sharer disconnected for now
    liveDot.style.display = "none";
    statusTitle.innerText = "User disconnected";
    lastUpdated.innerText = "--";
});

async function getRouteAndEta(startLat, startLng, destLat, destLng) {
    const url = `https://router.project-osrm.org/route/v1/driving/${startLng},${startLat};${destLng},${destLat}?overview=full&geometries=geojson`;

    try {
        const response = await fetch(url);
        const data = await response.json();

        if (data.routes && data.routes.length > 0) {
            const route = data.routes[0];
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
        }
    } catch (err) {
        console.error("Routing Error:", err);
    }
}
