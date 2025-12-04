package Servers.CrawlerServer.Components;

import Common.DataStructures.PageData;

import Servers.BarrelServer.Interfaces.IBarrelCrawler;
import Servers.CrawlerServer.DataStructures.BarrelInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

public class Crawler implements Runnable {
    private final int id;
    private final URLQueue queue;
    final List<BarrelInfo> barrelsList;

    private volatile boolean run = true;

    public Crawler(int id, URLQueue queue, List<BarrelInfo> barrelsList) {
        this.id = id;
        this.queue = queue;
        this.barrelsList = barrelsList;
    }

    private static final Set<String> STOP_WORDS = Set.of(
            // PT
            "de", "a", "o", "que", "e", "do", "da", "em", "um", "para", "é", "com", "não", "uma",
            "os", "no", "se", "na", "por", "as", "dos", "como", "mas", "ao", "ou", "das", "à",
            "ele", "ela", "eles", "elas", "são", "foi", "ser", "está", "há", "tem", "também",
            // EN
            "the", "and", "of", "to", "in", "that", "is", "for", "it", "on", "was", "with", "by",
            "at", "from", "an", "be", "this", "which", "or", "are", "we", "you", "your", "our", "us",
            "they", "their", "not", "have", "has", "had", "but", "about", "will", "can", "if"
    );

    private String cleanToken(String token) {
        if (token == null) return null;
        token = token.toLowerCase().trim();
        token = token.replaceAll("^\\p{Punct}+|\\p{Punct}+$", "");
        if (token.isBlank()) return null;
        return token;
    }

    private boolean isValidURL(String url) {
        if (url == null || url.isBlank()) return false;
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private boolean anyBarrelRegistered() {
        for (BarrelInfo bi : barrelsList) {
            if (bi != null && bi.getStatus() && bi.getStub() != null)
                return true;
        }
        return false;
    }

    private void submitToBarrels(PageData pageData) {
        int MAX_RETRIES = 10;
        //"Pause" if no barrels registered
        while (!anyBarrelRegistered()) {
            System.err.println("[Crawler " + id + "] No barrels available. Waiting for barrel registration. Retrying in 5s...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        synchronized (barrelsList) {

            //Iterate through registered barrels
            for (BarrelInfo barrelInfo : barrelsList) {
                if (barrelInfo == null || barrelInfo.getStub() == null || barrelInfo.getBarrelName() == null) {
                    continue;
                }

                boolean success = false;

                //Confirm packet delivery
                for (int attempt = 0; attempt < MAX_RETRIES; attempt++) { //10s
                    try {
                        IBarrelCrawler stub = barrelInfo.getStub();

                        if (stub == null || !barrelInfo.getStatus()) {
                            System.err.println("[Crawler " + id + "] Barrel '" + barrelInfo.getBarrelName() + "' not ready (attempt " + attempt + ")");
                        } else {
                            if (stub.submitPageData(pageData)) {
                                success = true;
                                break;
                            } else {
                                System.err.println("[Crawler " + id + "] Barrel '" + barrelInfo.getBarrelName() + "' did not confirm delivery (attempt " + attempt + ")");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[Crawler " + id + "] Error: Failed to submit PageData to Barrel '" + barrelInfo.getBarrelName() + "' (attempt " + attempt + 1 + "): " + e.getClass().getSimpleName());
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                if (!success) { //Barrel marked as Offline, packet not sent
                    synchronized (barrelsList) {
                        barrelsList.set(barrelsList.indexOf(barrelInfo), new BarrelInfo());
                    }
                    System.err.println("[Crawler " + id + "] Barrel '" + barrelInfo.getBarrelName() + "' removed from active list after " + MAX_RETRIES + " failed attempts.");
                }
            }
        }
    }

    private void crawling() {
        try {
            while (run) {
                String url = queue.getURL();

                if (!isValidURL(url)) { //Skip Invalid URL
                    continue;
                }

                try {
                    //URL
                    Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(5000).header("Custom-Header", "value").get();

                    PageData pageData = new PageData(url);

                    //Title
                    pageData.setTitle(doc.title());
                    for (String word : doc.title().split("\\s+")) {
                        String token = cleanToken(word);
                        if (token != null) pageData.addToken(token);
                    }

                    //Body snippet (1st 250chars)
                    String bodyText = doc.body().text();
                    String snippet = bodyText.length() > 250 ? bodyText.substring(0, 250) + "..." : bodyText;
                    pageData.setSnippet(snippet);

                    //Keywords
                    Element metaKeywords = doc.selectFirst("meta[name=keywords]");
                    if (metaKeywords != null) {
                        String content = metaKeywords.attr("content");
                        if (!content.isEmpty()) {
                            for (String keyword : content.split(",")) {
                                for (String token : keyword.split("\\s+")) {
                                    token = cleanToken(token);
                                    if (token != null && !Crawler.STOP_WORDS.contains(token)){
                                        pageData.addToken(token);
                                    }
                                }
                            }
                        }
                    }

                    //Tokens
                    StringTokenizer tokens = new StringTokenizer(doc.text());
                    int countTokens = 0;
                    while (tokens.hasMoreElements() && countTokens++ < 100) {
                        String token = cleanToken(tokens.nextToken());
                        if (token != null && !Crawler.STOP_WORDS.contains(token)) {
                            pageData.addToken(token);
                        }
                    }

                    //Links
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        String URL = link.attr("abs:href");
                        if (!URL.isBlank() && isValidURL(URL)) {
                            pageData.addLink(URL);
                            queue.addURL(URL);
                        }
                    }

                    submitToBarrels(pageData);

                } catch (IOException e) {
                    queue.addToVisited(url);
                    System.err.println("[Crawler " + id + "] Error: Failed to process URL: " + url + " -> (" + e.getClass().getSimpleName() +")");
                }
            }

        } catch (InterruptedException e) {
            System.out.println("[Crawler " + id + "] Crawler " + id + " interrupted.");
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        run = false;
    }

    @Override
    public void run() {
        System.out.println("[Crawler " + id + "] Crawler " + id + " thread started.");
        crawling();
        System.out.println("[Crawler " + id + "] Crawler " + id + " thread finished.");
    }
}