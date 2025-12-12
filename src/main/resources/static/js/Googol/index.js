const urlInput = document.getElementById("urlInput");
const indexButton = document.getElementById("indexButton");
const resultDiv = document.getElementById("result");
const mainContainer = document.getElementById("main-container");

// Load on page open
window.addEventListener("DOMContentLoaded", () => {
    const params = new URLSearchParams(window.location.search);
    const target = params.get("url");

    if (target) {
        urlInput.value = target;
        performIndex(target);
    }
});

// Trigger index on click
indexButton.addEventListener("click", () => {
    const target = urlInput.value.trim();
    if (!target) {
        resultDiv.innerHTML = "URL cannot be empty";
        resultDiv.className = "error";
        return;
    }

    window.history.pushState({}, "", `/index?url=${encodeURIComponent(target)}`);
    performIndex(target);
});

// Trigger index on Enter key
urlInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter") {
        const target = urlInput.value.trim();
        if (!target) {
            resultDiv.innerHTML = "URL cannot be empty";
            resultDiv.className = "error";
            return;
        }

        window.history.pushState({}, "", `/index?url=${encodeURIComponent(target)}`);
        performIndex(target);
    }
});

async function performIndex(target) {
    resultDiv.innerHTML = "";
    resultDiv.className = "";

    try {
        const { response, data } = await fetchServerData("/api/index", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ url: target })
        });

        if (!response || !data) {
            resultDiv.innerHTML = "Service unavailable";
            resultDiv.className = "error";
        } else if (response.ok) {
            resultDiv.innerHTML = data.msg || "URL indexed";
            resultDiv.className = "success";
        } else {
            resultDiv.innerHTML = data.msg || "Unknown error";
            resultDiv.className = "error";
        }

        mainContainer.classList.add("has-results");

    } catch (err) {
        resultDiv.innerHTML = "Service unavailable";
        resultDiv.className = "error";
        console.error(err);
    }
}
