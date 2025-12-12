let config = null;

// Load config.json (centralized)
async function loadConfig() {
    if (!config) {
        try {
            const res = await fetch('/config.json');
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            config = await res.json();
            console.log('[loadConfig] Loaded config:', config);
        } catch (err) {
            console.error('[loadConfig] Error loading config.json:', err);
            // fallback seguro
            config = { serverIP: 'localhost', serverPort: 8080 };
        }
    }
    return config;
}

// Build full URL with protocol, IP and port
async function buildURL(endpoint) {
    const cfg = await loadConfig();
    if (!cfg.serverIP || !cfg.serverPort) {
        console.error('[buildURL] serverIP or serverPort missing in config:', cfg);
        return endpoint;
    }
    const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
    return `${protocol}//${cfg.serverIP}:${cfg.serverPort}${endpoint}`;
}