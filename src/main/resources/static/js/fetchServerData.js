// Generic fetch wrapper using config.json settings
async function fetchServerData(endpoint, options = {}) {
    try {
        const url = await buildURL(endpoint);
        const response = await fetch(url, options);
        const data = await response.json().catch(() => null);
        return { response, data };
    } catch (err) {
        console.error(`[HTTP] Error fetching ${endpoint}:`, err);
        return { response: null, data: null };
    }
}
