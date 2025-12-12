const queryInput = document.getElementById('query');
const searchButton = document.getElementById('searchButton');
const resultsDiv = document.getElementById('results');
const errorDiv = document.getElementById('error');
const paginationDiv = document.getElementById('pagination');
const mainContainer = document.getElementById('main-container');

let currentPage = 0;
let currentQuery = '';

// Load on page open
window.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const query = params.get('q');
    const page = parseInt(params.get('p')) || 0;

    if (query) {
        queryInput.value = query;
        currentQuery = query;
        currentPage = page;
        performSearch(query, page);
    }
});

// Search on button click
searchButton.addEventListener('click', () => {
    const query = queryInput.value.trim();
    if (!query) {
        errorDiv.textContent = 'Search bar cannot be empty';
        return;
    }
    currentQuery = query;
    currentPage = 0;
    searchNews(query, currentPage);

    const newUrl = `hackernews?q=${encodeURIComponent(query)}&p=0`;
    window.history.pushState({}, "", newUrl);
});

// Search on Enter key
queryInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        e.preventDefault(); // evita submit de form
        const query = queryInput.value.trim();
        if (!query) {
            errorDiv.textContent = 'Search bar cannot be empty';
            return;
        }
        currentQuery = query;
        currentPage = 0;
        searchNews(query, currentPage);

        const newUrl = `hackernews?q=${encodeURIComponent(query)}&p=0`;
        window.history.pushState({}, "", newUrl);
    }
});

async function searchNews(query, page) {
    resultsDiv.innerHTML = '';
    errorDiv.textContent = '';
    paginationDiv.innerHTML = '';

    resultsDiv.innerHTML = '<p style="text-align: center; color: #AAAAAA; margin-top: 20px;">Searching...</p>';

    try {
        const { response, data } = await fetchServerData(`/api/hackernews/search?q=${encodeURIComponent(query)}&p=${page}`);

        resultsDiv.innerHTML = '';

        if (!response) {
            errorDiv.textContent = 'Service unavailable';
            return;
        }

        switch (response.status) {
            case 200:
                if (data && data.length > 0) {
                    mainContainer.classList.add('has-results');
                    displayResults(data);
                    displayPagination(page);
                } else {
                    errorDiv.textContent = 'No search results found';
                }
                break;
            case 201:
                errorDiv.textContent = 'No search results found';
                break;
            case 400:
                errorDiv.textContent = 'Query cannot be empty';
                break;
            case 401:
                errorDiv.textContent = 'Invalid search query';
                break;
            case 500:
                errorDiv.textContent = 'Service unavailable';
                break;
            default:
                errorDiv.textContent = 'Unexpected error';
                break;
        }

    } catch (err) {
        resultsDiv.innerHTML = '';
        errorDiv.textContent = 'Service unavailable';
        console.error('Search error:', err);
    }
}

function displayResults(news) {
    news.forEach(item => {
        const resultItem = document.createElement('div');
        resultItem.className = 'result-item';

        const title = document.createElement('a');
        title.href = item.url;
        title.target = '_blank';
        title.textContent = item.title;

        const url = document.createElement('div');
        url.className = 'result-url';
        url.textContent = item.url;

        const snippet = document.createElement('div');
        snippet.className = 'result-snippet';
        snippet.textContent = item.snippet || 'No description available.';

        resultItem.appendChild(title);
        resultItem.appendChild(url);
        resultItem.appendChild(snippet);
        resultsDiv.appendChild(resultItem);
    });
}

function displayPagination(page) {
    paginationDiv.innerHTML = '';

    // Previous button
    if (page > 0) {
        const prevButton = document.createElement('button');
        prevButton.textContent = 'Previous';
        prevButton.addEventListener('click', () => {
            const newPage = page - 1;
            currentPage = newPage;
            searchNews(currentQuery, newPage);

            const newUrl = `hackernews?q=${encodeURIComponent(currentQuery)}&p=${newPage}`;
            window.history.pushState({}, "", newUrl);
        });
        paginationDiv.appendChild(prevButton);
    }

    // Page indicator
    const pageIndicator = document.createElement('span');
    pageIndicator.textContent = `Page ${page + 1}`;
    pageIndicator.style.color = '#FFFFFF';
    pageIndicator.style.fontWeight = 'bold';
    paginationDiv.appendChild(pageIndicator);

    // Next button
    const nextButton = document.createElement('button');
    nextButton.textContent = 'Next';
    nextButton.addEventListener('click', () => {
        const newPage = page + 1;
        currentPage = newPage;
        searchNews(currentQuery, newPage);

        const newUrl = `hackernews?q=${encodeURIComponent(currentQuery)}&p=${newPage}`;
        window.history.pushState({}, "", newUrl);
    });
    paginationDiv.appendChild(nextButton);
}