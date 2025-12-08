package pt.dei.googol.Projeto_SD.Servers.BarrelServer.Components;

import pt.dei.googol.Projeto_SD.Common.DataStructures.*;
import pt.dei.googol.Projeto_SD.Common.Functions.URLCleaner;
import pt.dei.googol.Projeto_SD.Servers.BarrelServer.DataStructures.PageHeader;
import pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces.IBarrelBarrel;
import pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces.IBarrelCrawler;
import pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces.IBarrelGateway;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Barrel extends UnicastRemoteObject implements IBarrelGateway, IBarrelCrawler, IBarrelBarrel {
    String name;
    private int nPagesReceived = 0;
    //Main Indexes
    private final HashMap<String, PageHeader> pageHeaderIndex = new HashMap<>(); //<URL, PageHeader>  PageHeader contains Title and Body Snippet
    private final ConcurrentHashMap<String, Set<String>> tokenIndex; //<Token, URLs>
    private final ConcurrentHashMap<String, Set<String>> linkIndex; //<URL, Linking URLs>
    private final AtomicInteger nTokenURLs = new AtomicInteger(0);
    private final AtomicInteger nLinkingURLs = new AtomicInteger(0);
    private static Boolean exportBarrelStats = true;
    private volatile boolean isReady = false; //safety flag

    public Barrel(String name) throws RemoteException {
        this.name = name;
        this.tokenIndex = new ConcurrentHashMap<>();
        this.linkIndex = new ConcurrentHashMap<>();
    }


    public void restoreBackup(Map<String, PageHeader> pageHeaderIndexCopy,
                              Map<String, Set<String>> tokenIndexCopy,
                              Map<String, Set<String>> linkIndexCopy) {
        if (pageHeaderIndexCopy != null) {
            pageHeaderIndex.putAll(pageHeaderIndexCopy);
        }

        if (tokenIndexCopy != null) {
            for (Map.Entry<String, Set<String>> e : tokenIndexCopy.entrySet()) {
                tokenIndex.put(e.getKey(), new HashSet<>(e.getValue()));
            }
            nTokenURLs.set(tokenIndex.values().stream().mapToInt(Set::size).sum());
        }

        if (linkIndexCopy != null) {
            for (Map.Entry<String, Set<String>> e : linkIndexCopy.entrySet()) {
                linkIndex.put(e.getKey(), new HashSet<>(e.getValue()));
            }
            nLinkingURLs.set(linkIndex.values().stream().mapToInt(Set::size).sum());
        }

        isReady = true;
        System.out.println("[Barrel Server] Backup restore complete for barrel '" + name + "'. Ready to receive PageData.");
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    @Override
    public void systemShutdown() throws RemoteException {
        BarrelServer.shutdown();
    }

    @Override
    public Map<String, PageHeader> getPageHeaderIndex() throws RemoteException {
        return new HashMap<>(pageHeaderIndex);
    }

    @Override
    public Map<String, Set<String>> getTokenIndex() throws RemoteException {
        Map<String, Set<String>> copy = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : tokenIndex.entrySet()) {
            copy.put(e.getKey(), new HashSet<>(e.getValue()));
        }
        return copy;
    }

    @Override
    public Map<String, Set<String>> getLinkIndex() throws RemoteException {
        Map<String, Set<String>> copy = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : linkIndex.entrySet()) {
            copy.put(e.getKey(), new HashSet<>(e.getValue()));
        }
        return copy;
    }

    public String getBarrelName() {
        return name;
    }

    //Returns true if URL exists in pageHeaderIndex, false if not
    @Override
    public boolean checkURL(String url) throws RemoteException {
        url = URLCleaner.cleanURL(url);
        if (url == null) return false;
        return pageHeaderIndex.containsKey(url);
    }

    @Override
    public synchronized boolean submitPageData(PageData pageData) throws RemoteException {
        if (!isReady) return false;

        String url = pageData.getURL();

        //Page Header Index
        pageHeaderIndex.put(url, new PageHeader(pageData.getTitle(), pageData.getSnippet()));

        //Token Index
        //for (key: token <- URLs)
        for (String token : pageData.getTokens()) {
            tokenIndex.compute(token, (_, urls) -> {
                if (urls == null) {
                    urls = new HashSet<>();
                }
                int oldSize = urls.size();
                urls.add(url);
                if (urls.size() > oldSize) {
                    nTokenURLs.incrementAndGet();
                }
                return urls;
            });
        }

        //Link Index
        //for (key: link <- URLs)
        for (String link : pageData.getExtractedLinks()) {
            linkIndex.compute(link, (_, urls) -> {
                if (urls == null) {
                    urls = new HashSet<>();
                }
                int oldSize = urls.size();
                urls.add(url);
                if (urls.size() > oldSize) {
                    nLinkingURLs.incrementAndGet();
                }
                return urls;
            });
        }

        nPagesReceived++;

        if (exportBarrelStats) {
            BarrelServer.exportBarrelStats(new BarrelStats(
                    name,
                    nPagesReceived,
                    pageHeaderIndex.size(),
                    tokenIndex.size(),
                    nTokenURLs.get(),
                    linkIndex.size(),
                    nLinkingURLs.get()
            ));
        }

        //DEBUG
        //printIndexes();
        return true;
    }

    //status values:
    //-1: Error
    //0: Results
    //1: Empty Results
    @Override
    public synchronized SearchResult searchGatewayBarrel(List<String> searchWords, int pageNumber, int URLsPerPage) throws RemoteException {

        System.out.println("[Barrel Server] Search Words: " + String.join(" ", searchWords));
        Set<String> resultUrls = null;

        for (String token : searchWords) {
            //token -> URL List
            Set<String> TokenInURLs = tokenIndex.getOrDefault(token.toLowerCase(), Set.of());
            if (resultUrls == null) {
                resultUrls = new HashSet<>(TokenInURLs);
            } else {
                resultUrls.retainAll(TokenInURLs);
            }
        }

        //No Search Results
        if (resultUrls == null || resultUrls.isEmpty()) return new SearchResult(0, Collections.emptyList());

        //URL <- Links pointing to URL
        List<String> sortedURLs = resultUrls.stream().sorted(Comparator
            .comparingInt((String url) -> linkIndex.getOrDefault(url, Set.of()).size()) //Compare function return value (called twice). Function returns amount links pointing to URL
            .reversed()).toList();

        int startIndex = pageNumber * URLsPerPage;
        int endIndex = Math.min(startIndex + URLsPerPage, sortedURLs.size()); //Check if endIndex > size URL list

        //No more Search Results
        if (startIndex >= sortedURLs.size()) {
            return new SearchResult(1, Collections.emptyList());
        }

        List<URLHeader> results = new ArrayList<>();
        for (String url : sortedURLs.subList(startIndex, endIndex)) {
            PageHeader header = pageHeaderIndex.get(url);
            results.add(new URLHeader(url, header.title(), header.snippet()));
        }

        //Search Results
        return new SearchResult(0, results);
    }

    //status values:
    //0: Results
    //1: Empty Results
    @Override
    public synchronized LinkingURLsResult getLinkingURLsGatewayBarrel(String url) {
        url = URLCleaner.cleanURL(url);
        if (url == null) {
            return new LinkingURLsResult(1, Collections.emptySet());
        }
        System.out.println("[Barrel Server] Target URL: " + url);
        Set<String> links = linkIndex.get(url);
        if (links == null) {
            return new LinkingURLsResult(1, Collections.emptySet());
        }
        return new LinkingURLsResult(0, links);
    }

    @Override
    public synchronized BarrelStats getBarrelStats() throws RemoteException{
        exportBarrelStats = true;
        return new BarrelStats(name, nPagesReceived ,pageHeaderIndex.size(),
                tokenIndex.size(), tokenIndex.values().stream().mapToInt(Set::size).sum(),
                linkIndex.size(), linkIndex.values().stream().mapToInt(Set::size).sum());
    }

    @Override
    public void stopExportBarrelStats() throws RemoteException {
        exportBarrelStats = false;
    }

    //DEBUG
    @SuppressWarnings("unused")
    public void printIndexes() {
        System.out.println("========== PAGE HEADER INDEX ==========");
        pageHeaderIndex.forEach((url, header) -> System.out.println("📄 " + url + " → Title: \"" + header.title() + "\" -> Body Snippet: \"" + header.snippet() + "\""));

        System.out.println("\n========== TOKEN INDEX ==========");
        tokenIndex.forEach((token, urls) -> System.out.println("🧩 " + token + " → " + urls));

        System.out.println("\n========== LINK INDEX ==========");
        linkIndex.forEach((link, urls) -> System.out.println("🔗 " + link + " ← " + urls));

        System.out.println("========================================\n");
    }

}