const queryInput = document.getElementById("query");
const searchButton = document.getElementById("searchButton");
const resultsDiv = document.getElementById("results");
const errorDiv = document.getElementById("error");
const mainContainer = document.getElementById("main-container");

// Load on page open
window.addEventListener("DOMContentLoaded", () => {
    const params = new URLSearchParams(window.location.search);
    const target = params.get("url");

    if (target) {
        queryInput.value = target;
        loadResults(target);
    }
});

// Trigger search on click
searchButton.addEventListener("click", () => {
    const target = queryInput.value.trim();
    if (!target) {
        errorDiv.innerHTML = "Target URL cannot be empty";
        return;
    }

    window.history.pushState({}, "", `/links?url=${encodeURIComponent(target)}`);
    loadResults(target);
});

// Trigger search on Enter
queryInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter") {
        const target = queryInput.value.trim();
        if (!target) {
            errorDiv.innerHTML = "Target URL cannot be empty";
            return;
        }

        window.history.pushState({}, "", `/links?url=${encodeURIComponent(target)}`);
        loadResults(target);
    }
});

async function loadResults(target) {
    resultsDiv.innerHTML = "";
    errorDiv.innerHTML = "";

    try {
        const { response, data: links } = await fetchServerData(`/api/links?url=${encodeURIComponent(target)}`);

        if (!response) return errorDiv.innerHTML = "Service unavailable";
        if (response.status === 400) return errorDiv.innerHTML = "Target URL cannot be empty";
        if (response.status === 201) return errorDiv.innerHTML = "No links found for this URL";
        if (response.status === 500) return errorDiv.innerHTML = "Service unavailable";

        links.forEach(url => {
            const div = document.createElement("div");
            div.className = "result-item";
            div.innerHTML = `<a href="${url}" target="_blank">${url}</a>`;
            resultsDiv.appendChild(div);
        });

        mainContainer.classList.add("has-results");

    } catch (err) {
        errorDiv.innerHTML = "Service unavailable";
        console.error(err);
    }
}
