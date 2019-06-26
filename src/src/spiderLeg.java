package src;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.simple.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import weka.main.weka.SentimentClass;
import weka.main.weka.threeway.DataSetReader;
import weka.main.weka.threeway.DataSetWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//https://try.jsoup.org/
public class spiderLeg {
    private Map<Integer, String> hashMap = new HashMap<>(); //
    // We'll use a fake USER_AGENT so the web server thinks the robot is a normal web browser.
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";
    private List<String> links = new LinkedList<>();

    /*
     * This performs all the work. It makes an HTTP request, checks the response, and then gathers
     * up all the links on the page. Perform a searchForWord after the successful crawl
     *
     * @param url
     *            - The URL to visit
     * @return whether or not the crawl was successful
     */

    public boolean crawl(String url, BulkProcessor proc, String goal, String exclude, String include, String noneq)
    {
        boolean r = false;
        try
        {
            Connection connection = Jsoup.connect(url).userAgent(USER_AGENT);
            Document htmlDocument = connection.get();
            JSONObject obj=new JSONObject();
            String fulltext = "";
            String text = "";
            Elements comms = null;

            if(connection.response().statusCode() == 200) // 200 is the HTTP OK status code
            // indicating that everything is great.
            {
                System.out.println("\n**Visiting** Received web page at " + url);
                int hashLink = 0;
                Elements linksOnPage = htmlDocument.select("a");
                //System.out.println(linksOnPage);
                for(Element link : linksOnPage)
                {
                    hashLink = link.toString().hashCode();
                    hashMap.put(hashLink, link.toString());
                    if (link.toString().startsWith("/")){
                        link = new Element(("https://arstechnica.com"+link.toString()));
                    }
                    if ((link.toString().startsWith(include))
                            &&!(link.toString().contains(exclude))
                            &&!(link.toString().contains("&"))
                            &&!(link.toString().contains("#"))
                            &&!(link.toString().contains("?theme="))
                            &&!(link.toString().equals(noneq)))
                    {
                        if (doubleS(hashLink)) { //doubles check
                            this.links.add(link.absUrl("href"));
                        }
                    }
                }
                String trash = "Skip to main content Biz & IT Tech Science Policy Cars Gaming & Culture Store Forums Subscribe Close " +
                        "Navigate Store Subscribe Videos Features Reviews RSS Feeds Mobile Site About Ars Staff Directory Contact Us Advertise " +
                        "with Ars Reprints Filter by topic Biz & IT Tech Science Policy Cars Gaming & Culture Store Forums Settings Front page " +
                        "layout Grid List Site theme Black on white White on black Sign in Comment activity Sign up or login to join the " +
                        "discussions! Stay logged in | Having trouble? Sign up to comment and more Sign up ";
                String trash1 = "You must login or create an account to comment. Channel Ars Technica ← Previous story " +
                        "Next story → Related Stories Sponsored Stories Powered by Today on Ars Store Subscribe About Us RSS Feeds " +
                        "View Mobile Site Contact Us Staff Advertise with us Reprints Newsletter Signup Join the Ars Orbital " +
                        "Transmission mailing list to get weekly updates delivered to your inbox. CNMN Collection WIRED Media Group © 2019 " +
                        "Condé Nast. All rights reserved. Use of and/or registration on any portion of this site constitutes acceptance of our " +
                        "User Agreement (updated 5/25/18) and Privacy Policy and Cookie Statement (updated 5/25/18) and " +
                        "Ars Technica Addendum (effective 8/21/2018). Ars may earn compensation on sales from links on this site. " +
                        "Read our affiliate link policy. Your California Privacy Rights The material on this site may not be reproduced, " +
                        "distributed, transmitted, cached or otherwise used, except with the prior written permission of Condé Nast. Ad Choices";
                if (goal != "comments"){
                    text = htmlDocument.select("body").text();
                    text=text.trim();
                    if (text.startsWith(trash)) text = text.replace(trash, "");
                    if (text.endsWith(trash1)) text = text.replace(trash1, "");
                    fulltext = new StringBuilder().append(text).toString();
                    obj.put("url", url);
                    obj.put("header", htmlDocument.select("h1").text());
                    obj.put("text", fulltext);
                    obj.put("hash", hashLink);
                    proc.add(new IndexRequest("ars", goal).source(obj, XContentType.JSON));
                } else {
                    comms = htmlDocument.select("li:has(header)[id^=comment]");
                    for (Element comm : comms){
                        String author = comm.select("> header > div > span > a[href]").text();
                        String date = comm.select("> header > aside > a[title!=reply]").text();
                        String tmp = comm.select("> div.body").text().replace(comm.select("> div > div[class=quotetitle]").text(), "");
                        tmp = tmp.replace(comm.select("> div > div[class=quotecontent]").text(), "").trim();
                        tmp = comm.select("> div").text().replace(comm.select("> div > div[class=quotetitle]").text(), "");
                        tmp = tmp.replace(comm.select("> div > div[class=quotecontent]").text(), "").trim();
                        obj.put("author", author);
                        obj.put("date", date);
                        obj.put("text", tmp);
                        //System.out.println(obj.toString());
                        String csvFile = "dataset.csv";
                        DataSetReader.CSVInstance instance = null;
                        instance.phrase= tmp;
                        instance.phraseID = tmp.hashCode();
                        DataSetWriter writer = new DataSetWriter(csvFile);
                        writer.writeCSV(instance, SentimentClass.ThreeWayClazz.NEUTRAL);
                        try {
                            writer.close();
                        } catch (IOException e) {
                            System.out.println("Error while flushing/closing fileWriter !!!");
                            e.printStackTrace();
                        }

                        //proc.add(new IndexRequest("ars", goal).source(obj, XContentType.JSON));
                    }
                }
                System.out.println("Found (" + links.size() + ") links");
                r = true;
            }
            if(!connection.response().contentType().contains("text/html"))
            {
                System.out.println("**Failure** Retrieved something other than HTML");
            }
        }
        catch(IOException e)
        {
            // We were not successful in our HTTP request
            r = false;
        }
        return r;
    }

    private boolean doubleS(int key){
        return hashMap.get(key) != null;
    }

    public List<String> getLinks()
    {
        return this.links;
    }

}
