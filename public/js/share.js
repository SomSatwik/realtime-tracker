const socket = io();
const startBtn = document.getElementById("start-btn");
const stopBtn = document.getElementById("stop-btn");
const statusIndicator = document.getElementById("status-indicator");
const statusText = document.getElementById("status-text");
const logBox = document.getElementById("log");
const copyBtn = document.getElementById("copy-btn");

let watchId = null;

// Join the session room for updates/control
socket.emit("join-session", SESSION_ID);

copyBtn.addEventListener("click", () => {
    const copyText = document.getElementById("share-link");
    copyText.select();
    document.execCommand("copy");
    copyBtn.innerText = "Copied!";
    setTimeout(() => copyBtn.innerText = "Copy", 2000);
});

startBtn.addEventListener("click", () => {
    if (!navigator.geolocation) {
        log("Geolocation is not supported by your browser.");
        return;
    }

    log("Requesting location access...");

    watchId = navigator.geolocation.watchPosition(
        (position) => {
            const { latitude, longitude } = position.coords;

            // UI Updates
            statusIndicator.classList.add("status-active");
            statusText.innerText = "Sharing Live Location";
            startBtn.classList.add("hidden");
            stopBtn.classList.remove("hidden");

            log(`Sent: ${latitude.toFixed(5)}, ${longitude.toFixed(5)}`);

            // Emit to server
            socket.emit("send-location", {
                sessionId: SESSION_ID,
                latitude,
                longitude
            });
        },
        (error) => {
            let msg = "Error: ";
            switch (error.code) {
                case error.PERMISSION_DENIED: msg += "User denied request."; break;
                case error.POSITION_UNAVAILABLE: msg += "Location unavailable."; break;
                case error.TIMEOUT: msg += "Request timed out."; break;
                default: msg += "Unknown error."; break;
            }
            log(msg);
            stopSharing(); // Reset UI
        },
        {
            enableHighAccuracy: true,
            timeout: 5000,
            maximumAge: 0
        }
    );
});

stopBtn.addEventListener("click", () => {
    stopSharing();
    // Notify server/viewers
    socket.emit("stop-sharing", SESSION_ID);
});

function stopSharing() {
    if (watchId !== null) {
        navigator.geolocation.clearWatch(watchId);
        watchId = null;
    }
    statusIndicator.classList.remove("status-active");
    statusText.innerText = "Sharing Stopped";
    startBtn.classList.remove("hidden");
    stopBtn.classList.add("hidden");
    log("Stopped sharing.");
}

function log(msg) {
    const p = document.createElement("div");
    p.innerText = `${new Date().toLocaleTimeString()}: ${msg}`;
    logBox.prepend(p);
}
