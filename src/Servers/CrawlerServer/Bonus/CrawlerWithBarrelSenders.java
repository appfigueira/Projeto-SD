package Servers.CrawlerServer.Bonus;

import Common.DataStructures.PageData;

import Servers.CrawlerServer.Components.URLQueue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

public class CrawlerWithBarrelSenders implements Runnable {
    private final int id;
    private final URLQueue queue;
    private final List<BarrelSender> barrelSenders;

    private volatile boolean run = true;

    public CrawlerWithBarrelSenders(int id, URLQueue queue, List<BarrelSender> barrelSenders) {
        this.id = id;
        this.queue = queue;
        this.barrelSenders = barrelSenders;
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
                                    if (token != null && !STOP_WORDS.contains(token)){
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
                        if (token != null && !STOP_WORDS.contains(token)) {
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

                    for (BarrelSender barrelSender : barrelSenders) {
                        barrelSender.submitPageData(pageData);
                    }

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