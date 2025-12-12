const messagesDiv = document.getElementById("messages");
const promptInput = document.getElementById("promptInput");
const sendBtn = document.getElementById("sendBtn");

let isWaiting = false;

sendBtn.addEventListener("click", sendPrompt);

promptInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        sendPrompt();
    }
});

function addMessage(text, type) {
    const div = document.createElement("div");
    div.classList.add("msg");

    if (type === "user") div.classList.add("user-msg");
    else div.classList.add("bot-msg");

    div.textContent = text;
    messagesDiv.appendChild(div);

    // Scroll automático
    messagesDiv.scrollTop = messagesDiv.scrollHeight;
}

async function sendPrompt() {
    if (isWaiting) return;
    const prompt = promptInput.value.trim();
    if (prompt === "") return;

    addMessage(prompt, "user");
    promptInput.value = "";
    isWaiting = true;
    sendBtn.disabled = true;

    try {
        const { response, data } = await fetchServerData("/apis/ai/generate", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ prompt })
        });

        if (!response || !response.ok || !data) {
            addMessage("Erro: serviço indisponível.", "bot");
            return;
        }

        if (data.error) {
            addMessage("AI limit reached. Please try again later.", "bot");
            return;
        }

        addMessage(data.text, "bot");

    } catch (e) {
        console.error(e);
        addMessage("Erro: não foi possível contactar o servidor.", "bot");
    }

    isWaiting = false;
    sendBtn.disabled = false;
}