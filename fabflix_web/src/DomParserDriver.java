public class DomParserDriver {
    public static void main(String[] args) {
        MovieDomParser mdp = new MovieDomParser();
        ActorsDomParser adp = new ActorsDomParser();
        CastsDomParser cdp = new CastsDomParser();
        final long startTime = System.currentTimeMillis();
        try {
            mdp.runParser();
            adp.runParser();
            cdp.runParser();
            final long endTime = System.currentTimeMillis();
            System.out.println("Total execution time: " + (endTime - startTime));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
