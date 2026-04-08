const socket = io();

const actionBtn = document.getElementById("action-btn");
const sharingCircle = document.getElementById("sharing-circle");
const statusText = document.getElementById("status-text");
const logBox = document.getElementById("log");
const copyBtn = document.getElementById("copy-btn");
const nativeShareBtn = document.getElementById("native-share-btn");

let watchId = null;
let isSharing = false;

// Join the session room for updates/control
socket.emit("join-session", SESSION_ID);

copyBtn.addEventListener("click", () => {
    navigator.clipboard.writeText(TRACK_URL).then(() => {
        const originalText = copyBtn.innerText;
        copyBtn.innerText = "Copied!";
        setTimeout(() => copyBtn.innerText = originalText, 2000);
    });
});

nativeShareBtn.addEventListener("click", async () => {
    if (navigator.share) {
        try {
            await navigator.share({
                title: 'Track Me Live',
                text: 'Follow my live location on GIET Bus Tracker',
                url: TRACK_URL,
            });
            log("Shared successfully");
        } catch (err) {
            log("Share cancelled");
        }
    } else {
        log("Native share not supported");
    }
});

actionBtn.addEventListener("click", () => {
    if (isSharing) {
        stopSharing();
        socket.emit("stop-sharing", SESSION_ID);
    } else {
        startSharing();
    }
});

function startSharing() {
    if (!navigator.geolocation) {
        log("Geolocation is not supported by your browser.");
        return;
    }

    log("Requesting location access...");

    watchId = navigator.geolocation.watchPosition(
        (position) => {
            const { latitude, longitude } = position.coords;

            // UI Updates
            if (!isSharing) {
                isSharing = true;
                sharingCircle.classList.add("active");
                statusText.innerText = "Sharing Live";
                statusText.classList.add("active");
                statusText.classList.remove("stopped");
                
                actionBtn.innerText = "Stop Sharing";
                actionBtn.classList.add("active");
            }

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
}

function stopSharing() {
    if (watchId !== null) {
        navigator.geolocation.clearWatch(watchId);
        watchId = null;
    }
    
    isSharing = false;
    
    sharingCircle.classList.remove("active");
    statusText.innerText = "Stopped";
    statusText.classList.remove("active");
    statusText.classList.add("stopped");
    
    actionBtn.innerText = "Start Sharing";
    actionBtn.classList.remove("active");
    
    log("Stopped sharing.");
}

function log(msg) {
    const p = document.createElement("div");
    p.className = "log-entry caption-text fade-in";
    p.innerText = msg;
    
    logBox.appendChild(p);
    
    // Keep max 5 lines
    while (logBox.children.length > 5) {
        logBox.removeChild(logBox.firstChild);
    }
}
