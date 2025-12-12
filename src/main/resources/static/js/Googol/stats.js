// WebSocket connection
let socket = null;
let reconnectInterval = 2500; // 2.5s

// Inicializa a conexão WebSocket quando a página carrega
document.addEventListener('DOMContentLoaded', function() {
    connectWebSocket();
});

/**
 * PASSO 1: Conectar ao WebSocket
 * Cria uma conexão WebSocket com o servidor
 */
async function connectWebSocket() {
    const config = await loadConfig();
    const wsUrl = await buildURL('/ws/stats');

    // Determina o protocolo (ws:// ou wss://) baseado no protocolo HTTP
    const protocol = wsUrl.startsWith('https:') ? 'wss:' : 'ws:';

    socket = new WebSocket(wsUrl.replace(/^https?:/, protocol));

    console.log('[WebSocket] Connecting to:', wsUrl);
    updateConnectionStatus('connecting');

    try {
        // PASSO 2: Quando a conexão abre com sucesso
        socket.onopen = function(event) {
            console.log('[WebSocket] WebSocket connection established!');
            updateConnectionStatus('connecting');
        };

        // PASSO 3: Quando recebemos dados do servidor
        socket.onmessage = function(event) {
            try {
                const systemStats = JSON.parse(event.data);

                if (systemStats.status === 0) {
                    updateConnectionStatus('connected');
                } else {
                    updateConnectionStatus('disconnected');
                }

                updateUI(systemStats);
            } catch (error) {
                console.error('[WebSocket] Error parsing data:', error);
            }
        };

        // PASSO 4: Quando há um erro na conexão
        socket.onerror = function(error) {
            console.error('[WebSocket] Error:', error);
            updateConnectionStatus('disconnected');
        };

        // PASSO 5: Quando a conexão fecha
        socket.onclose = function(event) {
            console.log('[WebSocket] Connection closed. Code:', event.code, 'Reason:', event.reason);
            updateConnectionStatus('disconnected');

            console.log(`[WebSocket] Reconnecting in ${reconnectInterval}ms...`);
            setTimeout(connectWebSocket, reconnectInterval);
        };

    } catch (error) {
        console.error('[WebSocket] Error creating connection:', error);
        updateConnectionStatus('disconnected');
    }
}

/**
 * Atualiza o status da conexão na UI
 */
function updateConnectionStatus(status) {
    const statusElement = document.getElementById('connectionStatus');
    const topSearchesSection = document.querySelector('.top-searches-section');
    const barrelsSection = document.getElementById('barrels');

    statusElement.classList.remove('status-connecting', 'status-connected', 'status-disconnected');

    switch(status) {
        case 'connecting':
            statusElement.classList.add('status-connecting');
            statusElement.innerHTML = '🔄 Connecting...';
            // Esconde as secções enquanto conecta
            topSearchesSection.style.display = 'none';
            barrelsSection.style.display = 'none';
            break;
        case 'connected':
            statusElement.classList.add('status-connected');
            statusElement.innerHTML = '🟢 Online';
            // Mostra as secções quando online
            topSearchesSection.style.display = 'block';
            barrelsSection.style.display = 'flex';
            break;
        case 'disconnected':
            statusElement.classList.add('status-disconnected');
            statusElement.innerHTML = '🔴 Offline';
            // Esconde as secções quando offline
            topSearchesSection.style.display = 'none';
            barrelsSection.style.display = 'none';
            break;
    }
}

/**
 * FUNÇÃO PRINCIPAL: Atualiza toda a UI com os dados recebidos
 */
function updateUI(systemStats) {
    //console.log('[UI] Updating interface with:', systemStats);

    // Atualiza Top 10 Searches
    updateTopSearches(systemStats.top10Searches);

    // Atualiza Barrels
    updateBarrels(systemStats.barrelsStats);
}

/**
 * Atualiza a secção de Top 10 Searches
 */
function updateTopSearches(searches) {
    const container = document.getElementById('topSearches');

    // Se não há dados
    if (!searches || searches.length === 0) {
        container.innerHTML = '<div class="no-data">No searches recorded yet</div>';
        return;
    }

    // Cria a grid com as pesquisas
    let html = '<div class="top-searches-grid">';

    searches.forEach((query, index) => {
        html += `
            <div class="search-item">
                <span class="search-rank">#${index + 1}</span>
                <span class="search-query">${query}</span>
            </div>
        `;
    });

    html += '</div>';
    container.innerHTML = html;
}

/**
 * Atualiza a secção de Barrels
 */
function updateBarrels(barrelsStats) {
    const container = document.getElementById('barrels');

    // Se não há dados
    if (!barrelsStats || barrelsStats.length === 0) {
        container.innerHTML = '<div class="no-data">Barrels offline</div>';
        return;
    }

    // Cria os cards dos barrels
    let html = '';

    barrelsStats.forEach(barrel => {
        const isOnline = barrel.status;
        const cardClass = isOnline ? '' : 'offline';
        const statusClass = isOnline ? 'status-online' : 'status-offline';
        const statusText = isOnline ? '🟢 Online' : '🔴 Offline';

        html += `
            <div class="barrel-card ${cardClass}">
                <div class="barrel-header">
                    <div class="barrel-name">${barrel.name}</div>
                    <div class="barrel-status ${statusClass}">${statusText}</div>
                </div>

                <div class="barrel-stats">
                    ${isOnline ? `
                        <!-- Estatísticas de índice -->
                        <div class="stat-row">
                            <span class="stat-label">📄 Pages Received</span>
                            <span class="stat-value">${formatNumber(barrel.nPagesReceived)}</span>
                        </div>

                        <div class="stat-row">
                            <span class="stat-label">📚 Pages in Index</span>
                            <span class="stat-value">${formatNumber(barrel.nPages)}</span>
                        </div>

                        <div class="stat-row">
                            <span class="stat-label">🔤 Tokens</span>
                            <span class="stat-value">${formatNumber(barrel.nTokens)}</span>
                        </div>

                        <div class="stat-row">
                            <span class="stat-label">🔗 Token-URLs</span>
                            <span class="stat-value">${formatNumber(barrel.nTokenURLs)}</span>
                        </div>

                        <div class="stat-row">
                            <span class="stat-label">🌐 URLs</span>
                            <span class="stat-value">${formatNumber(barrel.nURLs)}</span>
                        </div>

                        <div class="stat-row">
                            <span class="stat-label">🔗 Linking URLs</span>
                            <span class="stat-value">${formatNumber(barrel.nLinkingURLs)}</span>
                        </div>

                        <!-- Estatísticas de performance (destacadas) -->
                        <div class="stat-row stat-highlight">
                            <span class="stat-label">⏳ Average Response Time</span>
                            <span class="stat-value">${formatResponseTime(barrel.avgResponseTime)}</span>
                        </div>

                        <div class="stat-row stat-highlight">
                            <span class="stat-label">📡 Requests</span>
                            <span class="stat-value">${formatNumber(barrel.nRequests)}</span>
                        </div>
                    ` : `
                        <div class="no-data">Barrel offline - no data available</div>
                    `}
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

/**
 * Formata números com separador de milhares
 */
function formatNumber(num) {
    if (num === null || num === undefined) return '0';
    return num.toLocaleString('en-US');
}

/**
 * Formata tempo de resposta (em décimas de segundo)
 */
function formatResponseTime(time) {
    if (time === null || time === undefined || time === 0) return '0.0 ds';
    return `${time.toFixed(1)} ds`;
}

// Cleanup quando a página fecha
window.addEventListener('beforeunload', function() {
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.close();
    }
});