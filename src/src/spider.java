package src;

import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.System.currentTimeMillis;

public class spider {
    // Fields
    Client client;
    BulkProcessor bulkProcessor;
    private static final int MAX_PAGES_TO_SEARCH = 10000;
    private Set<String> pagesVisited = Collections.synchronizedSet(new HashSet<>());
    private List<String> pagesToVisit = Collections.synchronizedList(new LinkedList<>());
    private byte[] local = new byte[]{127, 0, 0, 1};

    /**
     * Returns the next URL to visit (in the order that they were found). We also do a check to make
     * sure this method doesn't return a URL that has already been visited.
     */
    private String nextUrl() {
        //System.out.println("nurl");
        String nextUrl;
        do {
            nextUrl = this.pagesToVisit.remove(0);
            if (this.pagesToVisit.size()==0){
                System.out.println("next url " + this.pagesToVisit.size());}
        } while (this.pagesVisited.contains(nextUrl));
        this.pagesVisited.add(nextUrl);
        return nextUrl;
    }

    public String currUrl() {
        //System.out.println("curl");
        String currentUrl = null;
        if (this.pagesToVisit.isEmpty()) {
            //System.out.println("no new urls");
        } else {
            currentUrl = this.nextUrl();
        }
        return currentUrl;
    }

    synchronized void task(spiderLeg leg, String goal, String exclude, String include, String noneq) {
        leg.crawl(currUrl(), bulkProcessor, goal, exclude, include, noneq); // Lots of stuff happening here. Look at the crawl method in
        // spiderLeg
        System.out.println(String.format("**Done** Visited %s web page(s)", this.pagesVisited.size()));
        this.pagesToVisit.addAll(leg.getLinks());
    }

    /**
     * Our main launching point for the Spider's functionality. Internally it creates spider legs
     * that make an HTTP request and parse the response (the web page).
     *
     * @param url        - The starting point of the spider
     * //@param searchWord - The word or string that you are searching for
     */
    public void search(String url, String goal, String exclude, String startWith, String noneq, int nTreads) {
        spiderLeg leg1 = new spiderLeg();
        {
            try {
                Settings settings = Settings.builder()
                        .put("cluster.name","elasticsearch")         /*"docker-cluster"*/
                        //.put("client.transport.ping_timeout","10")
                        .put("client.transport.sniff", true)
                        .build();
                client = new PreBuiltTransportClient(settings)
                        .addTransportAddress(new TransportAddress(InetAddress.getByAddress(local), 9300));
                System.out.println("ElasticSearch connected");
            } catch (UnknownHostException e) {
                e.printStackTrace();
                System.out.println("Not connected");
            }
        }
        final long[] t = {0};
        bulkProcessor = BulkProcessor.builder(
                client,
                new BulkProcessor.Listener() {
                    @Override
                    public void beforeBulk(long executionId,
                                           BulkRequest request) {
                        t[0] = currentTimeMillis();
                        System.out.println("Number of actions: " + request.numberOfActions());
                    }

                    @Override
                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          BulkResponse response) {
                        System.out.println("Time taken for bulk: " + (currentTimeMillis()-t[0]));
                        System.out.println("Failures: " + response.hasFailures());
                    }

                    @Override
                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          Throwable failure) { failure.printStackTrace(); }
                })
                .setBulkActions(1000)
                .setBulkSize(new ByteSizeValue(1, ByteSizeUnit.MB))
                .setFlushInterval(TimeValue.timeValueSeconds(3))
                .setConcurrentRequests(1)
                .setBackoffPolicy(
                        BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100), 3))
                .build();
        leg1.crawl(url, bulkProcessor, goal, exclude, startWith, noneq);
        this.pagesToVisit.addAll(leg1.getLinks());
        while (this.pagesVisited.size() < MAX_PAGES_TO_SEARCH) {
            Callable task = () -> {
                spiderLeg leg = new spiderLeg();
                task(leg, goal, exclude, startWith, noneq);
                return true;
            };
            ExecutorService service = Executors.newFixedThreadPool(nTreads);
            Future result = service.submit(task);
            service.shutdown();
        }
        client.close();
        pagesVisited = null;
        pagesToVisit = null;
    }

}
