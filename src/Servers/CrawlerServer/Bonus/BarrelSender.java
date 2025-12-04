package Servers.CrawlerServer.Bonus;

import Common.DataStructures.PageData;
import Servers.CrawlerServer.DataStructures.BarrelInfo;


import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BarrelSender extends Thread {

    private final int id;
    private final BlockingQueue<PageData> queue;
    private final List<BarrelInfo> barrelsList;

    private volatile boolean run = true;

    public BarrelSender(int id, List<BarrelInfo> barrelsList) {
        this.id = id;
        this.queue = new LinkedBlockingQueue<>();
        this.barrelsList = barrelsList;
    }

    public void submitPageData(PageData pageData) {
        queue.add(pageData);
    }

    public static boolean anyServerOnline(List<BarrelInfo> barrelsList) {
        for (BarrelInfo info : barrelsList) {
            if (info.getStatus()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run() {
        System.out.println("[Barrel Sender " + id + "] Barrel Sender thread started.");

        BarrelInfo barrelInfo = barrelsList.get(id);
        int MAX_RETRIES = 10;

        while (run) {
            try {
                int retries = 0;
                //Try Barrel connection
                while ((barrelInfo.getStub() == null || !barrelInfo.getStatus()) && retries < MAX_RETRIES) {
                    retries++;
                    Thread.sleep(1000);
                }

                //Barrel still offline
                if (barrelInfo.getStub() == null || !barrelInfo.getStatus()) {
                    synchronized (barrelInfo) {
                        barrelInfo.setStatus(false);
                        barrelInfo.setStub(null);
                    }

                    if (anyServerOnline(barrelsList)) { //Data discarded, no data loss
                        queue.clear();
                        System.err.println("[Barrel Sender " + id + "] Barrel '" + barrelInfo.getBarrelName() + "' offline. Servers online, queue cleared.");
                    } else { //Data preserved, chance of data loss
                        System.err.println("[Barrel Sender " + id + "] Barrel '" + barrelInfo.getBarrelName() + "' offline. Servers offline, queue data preserved.");
                    }

                    //Wait 30s for next connection attempt
                    System.err.println("[Barrel Sender " + id + "] Retrying in 30s...");
                    Thread.sleep(30000);
                    continue;
                }

                synchronized (barrelInfo) {
                    barrelInfo.setStatus(true);
                }

                PageData data = queue.take();

                //DEBUG
                //printPageData(data);

                try {
                    if (!barrelInfo.getStub().submitPageData(data)) {
                        queue.add(data);
                        System.err.println("[Barrel Sender " + id + "] Error: Barrel '" + barrelInfo.getBarrelName() + "' did not confirm packet delivery. Retrying in 5s...");
                        Thread.sleep(5000);
                    }
                } catch (RemoteException e) {
                    synchronized (barrelInfo) {
                        barrelInfo.setStatus(false);
                        barrelInfo.setStub(null);
                    }
                    queue.add(data);
                    System.err.println("[Barrel Sender " + id + "] Error: Failed to connect to Barrel '" + barrelInfo.getBarrelName() + "'.");
                }
            } catch (InterruptedException e) {
                System.out.println("[Barrel Sender " + id + "] Barrel Sender thread interrupted.");
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[Barrel Sender " + id + "] Barrel Sender thread finished.");
    }

    public void shutdown() {
        run = false;
    }

    //DEBUG
    @SuppressWarnings("unused")
    public static void printPageData(PageData pageData) {
        System.out.println("========================================");

        System.out.println("URL: " + pageData.getURL());
        System.out.println("Título: " + pageData.getTitle());
        System.out.println("Snippet: " + pageData.getSnippet());
        System.out.println("========================================");

        System.out.println("📖 Tokens: " + pageData.getTokens().size() + " | 🔗 Links: " + pageData.getExtractedLinks().size());
        System.out.println("========================================");

        System.out.println("\n📖 Tokens extraídos:");
        for (String token : pageData.getTokens()) {
            System.out.println("  " + token);
        }

        System.out.println("\n🔗 Links extraídos:");
        for (String link : pageData.getExtractedLinks()) {
            System.out.println("  " + link);
        }

        System.out.println("========================================\n\n\n");
    }
}