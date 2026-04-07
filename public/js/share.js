const socket = io();
<<<<<<< HEAD

const actionBtn = document.getElementById("action-btn");
const sharingCircle = document.getElementById("sharing-circle");
const statusText = document.getElementById("status-text");
const logBox = document.getElementById("log");
const copyBtn = document.getElementById("copy-btn");
const nativeShareBtn = document.getElementById("native-share-btn");

let watchId = null;
let isSharing = false;
=======
const startBtn = document.getElementById("start-btn");
const stopBtn = document.getElementById("stop-btn");
const statusIndicator = document.getElementById("status-indicator");
const statusText = document.getElementById("status-text");
const logBox = document.getElementById("log");
const copyBtn = document.getElementById("copy-btn");

let watchId = null;
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb

// Join the session room for updates/control
socket.emit("join-session", SESSION_ID);

copyBtn.addEventListener("click", () => {
<<<<<<< HEAD
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
                text: 'Follow my live location on GhostTrack',
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
=======
    const copyText = document.getElementById("share-link");
    copyText.select();
    document.execCommand("copy");
    copyBtn.innerText = "Copied!";
    setTimeout(() => copyBtn.innerText = "Copy", 2000);
});

startBtn.addEventListener("click", () => {
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
    if (!navigator.geolocation) {
        log("Geolocation is not supported by your browser.");
        return;
    }

    log("Requesting location access...");

    watchId = navigator.geolocation.watchPosition(
        (position) => {
            const { latitude, longitude } = position.coords;

            // UI Updates
<<<<<<< HEAD
            if (!isSharing) {
                isSharing = true;
                sharingCircle.classList.add("active");
                statusText.innerText = "Sharing Live";
                statusText.classList.add("active");
                statusText.classList.remove("stopped");
                
                actionBtn.innerText = "Stop Sharing";
                actionBtn.classList.add("active");
            }
=======
            statusIndicator.classList.add("status-active");
            statusText.innerText = "Sharing Live Location";
            startBtn.classList.add("hidden");
            stopBtn.classList.remove("hidden");
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb

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
<<<<<<< HEAD
}
=======
});

stopBtn.addEventListener("click", () => {
    stopSharing();
    // Notify server/viewers
    socket.emit("stop-sharing", SESSION_ID);
});
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb

function stopSharing() {
    if (watchId !== null) {
        navigator.geolocation.clearWatch(watchId);
        watchId = null;
    }
<<<<<<< HEAD
    
    isSharing = false;
    
    sharingCircle.classList.remove("active");
    statusText.innerText = "Stopped";
    statusText.classList.remove("active");
    statusText.classList.add("stopped");
    
    actionBtn.innerText = "Start Sharing";
    actionBtn.classList.remove("active");
    
=======
    statusIndicator.classList.remove("status-active");
    statusText.innerText = "Sharing Stopped";
    startBtn.classList.remove("hidden");
    stopBtn.classList.add("hidden");
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
    log("Stopped sharing.");
}

function log(msg) {
    const p = document.createElement("div");
<<<<<<< HEAD
    p.className = "log-entry caption-text fade-in";
    p.innerText = msg; // Just the message, no timestamp for cleaner look like iMessage
    
    logBox.appendChild(p);
    
    // Keep max 5 lines
    while (logBox.children.length > 5) {
        logBox.removeChild(logBox.firstChild);
    }
}

=======
    p.innerText = `${new Date().toLocaleTimeString()}: ${msg}`;
    logBox.prepend(p);
}
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
