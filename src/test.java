import src.spider;

public class test {
    public static void main(String[] args)
    {
        spider spider = new spider();
        spider.search("https://arstechnica.com/science/2019/06/", "science/2019", "/?", "", "https://arstechnica.com/science/", 8);
        spider.search(" https://arstechnica.com/science/", "comments", "view=", "https://arstechnica.com", "", 8);
        // 9200 - elastic 5601 - kibana
    }
}
