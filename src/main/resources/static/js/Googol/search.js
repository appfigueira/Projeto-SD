const queryInput = document.getElementById("query");
const searchButton = document.getElementById("searchButton");
const resultsDiv = document.getElementById("results");
const errorDiv = document.getElementById("error");
const paginationDiv = document.getElementById("pagination");
const mainContainer = document.getElementById("main-container");
const aiSummaryContainer = document.getElementById("ai-summary-container");
const aiSummaryContent = document.getElementById("ai-summary-content");

// Load on page open
window.addEventListener("DOMContentLoaded", () => {
    const params = new URLSearchParams(window.location.search);
    const query = params.get("q");
    const page = params.get("p") || 0;

    if (query) {
        queryInput.value = query;
        loadResults(query, page);
    }
});

// Search on click
searchButton.addEventListener("click", () => {
    const query = queryInput.value.trim();
    if (!query) {
        errorDiv.innerHTML = "Search bar cannot be empty";
        return;
    }

    window.history.pushState({}, "", `/search?q=${encodeURIComponent(query)}&p=0`);
    loadResults(query, 0);
});

// Search on Enter
queryInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter") {
        const query = queryInput.value.trim();
        if (!query) {
            errorDiv.innerHTML = "Search bar cannot be empty";
            return;
        }

        window.history.pushState({}, "", `/search?q=${encodeURIComponent(query)}&p=0`);
        loadResults(query, 0);
    }
});

async function loadAISummary(query, searchResults) {
    aiSummaryContainer.classList.add("visible");
    aiSummaryContent.innerHTML = '<div class="ai-summary-loading">Generating AI summary...</div>';

    try {
        const context = searchResults.slice(0, 5).map(r =>
            `Title: ${r.title}\nURL: ${r.url}\nSnippet: ${r.snippet}`
        ).join("\n\n");

        const prompt = `You are an assistant that provides concise, clear summaries or explanations for a given topic or question. Follow these instructions:
        * Give a short, clear summary of the topic or question.
        * Include only key facts; avoid extra details.
        * Use numbers or data if relevant.
        * Pick the most common or relevant meaning if multiple exist.
        * Keep tone neutral and informative.
        Search words: ${query}`;

       const { response, data } = await fetchServerData("/apis/ai/generate", {
           method: "POST",
           headers: { "Content-Type": "application/json" },
           body: JSON.stringify({ prompt: prompt })
       });

       if (!response || !response.ok || !data) {
           throw new Error("AI service unavailable");
       }

        aiSummaryContent.innerHTML = `<p class="ai-summary-content">${data.text}</p>`;

    } catch (err) {
        console.error("AI Summary error:", err);
        aiSummaryContent.innerHTML = '<p class="ai-summary-error">Unable to generate AI summary at this moment.</p>';
    }
}

async function loadResults(query, page) {
    resultsDiv.innerHTML = "";
    errorDiv.innerHTML = "";
    paginationDiv.innerHTML = "";
    aiSummaryContainer.classList.remove("visible");

    try {
        const { response, data } = await fetchServerData(`/api/search?q=${encodeURIComponent(query)}&p=${page}`);

        if (!response) return errorDiv.innerHTML = "Service unavailable";
        if (response.status === 201) return errorDiv.innerHTML = "No search results found";
        if (response.status === 400) return errorDiv.innerHTML = "Search bar cannot be empty";
        if (response.status === 401) return errorDiv.innerHTML = "Invalid search words";
        if (response.status === 500) return errorDiv.innerHTML = "Service unavailable";

        // Load AI summary only on first page
        if (page == 0 && data.length > 0) {
            loadAISummary(query, data);
        }

        data.forEach(r => {
            const div = document.createElement("div");
            div.className = "result-item";
            div.innerHTML = `
                <a href="${r.url}" target="_blank">${r.title}</a>
                <div class="result-url">${r.url}</div>
                <div class="result-snippet">${r.snippet}</div>
            `;
            resultsDiv.appendChild(div);
        });

        if (data.length > 0) mainContainer.classList.add("has-results");

        // Pagination
        if (page > 0) {
            const prevButton = document.createElement("button");
            prevButton.textContent = "← Previous";
            prevButton.onclick = () => {
                window.history.pushState({}, "", `/search?q=${encodeURIComponent(query)}&p=${page-1}`);
                loadResults(query, page - 1);
            };
            paginationDiv.appendChild(prevButton);
        }

        const pageNumber = document.createElement("span");
        pageNumber.textContent = `${parseInt(page) + 1}`;
        pageNumber.style.padding = "20px";
        paginationDiv.appendChild(pageNumber);

        if (data.length === 10) {
            const nextButton = document.createElement("button");
            nextButton.textContent = "Next →";
            nextButton.onclick = () => {
                window.history.pushState({}, "", `/search?q=${encodeURIComponent(query)}&p=${parseInt(page)+1}`);
                loadResults(query, parseInt(page) + 1);
            };
            paginationDiv.appendChild(nextButton);
        }

    } catch (err) {
        errorDiv.innerHTML = "Service unavailable";
        console.error(err);
    }
}