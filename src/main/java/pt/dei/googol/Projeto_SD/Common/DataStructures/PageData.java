package pt.dei.googol.Projeto_SD.Common.DataStructures;

import pt.dei.googol.Projeto_SD.Common.Functions.URLCleaner;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class PageData implements Serializable {
    private final String url;
    private String title;
    private String snippet;
    private final Set<String> tokens;
    private final Set<String> extractedLinks;

    public PageData(String url) {
        this.url = url;
        this.title = "";
        this.snippet = "";
        this.tokens = new HashSet<>();
        this.extractedLinks = new HashSet<>();
    }

    public void setTitle(String title) {this.title = title;}

    public void setSnippet(String snippet) {this.snippet = snippet;}

    public void addToken(String token) {tokens.add(token);}

    public void addLink(String link) {
        String url = URLCleaner.cleanURL(link);
        if (url != null) {
            extractedLinks.add(url);
        }
    }

    public String getURL() {return url;}

    public Set<String> getTokens() {return tokens;}

    public Set<String> getExtractedLinks() {return extractedLinks;}

    public String getTitle() {return title;}

    public String getSnippet() {return snippet;}

    //Debug
    public static void printPageData(PageData pageData) {
        System.out.println("========================================");
        System.out.println("URL: " + pageData.getURL());
        System.out.println("Título: " + pageData.getTitle());
        System.out.println("Snippet: " + pageData.getSnippet());
        System.out.println("========================================");
        System.out.println("Tokens: " + pageData.getTokens().size() + " | 🔗 Links: " + pageData.getExtractedLinks().size());
        System.out.println("========================================");

        System.out.println("\nTokens extraídos:");
        for (String token : pageData.getTokens()) {
            System.out.println("  " + token);
        }

        System.out.println("\nLinks extraídos:");
        for (String link : pageData.getExtractedLinks()) {
            System.out.println("  " + link);
        }

        System.out.println("========================================\n\n\n");
    }
}