package com.aegisnet.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * REDDIT SOCIAL INTELLIGENCE SERVICE
 * 
 * Fetches real-time social media posts from Reddit about Pakistan crisis events.
 * Uses Reddit's public JSON API (100% free, no API key, no signup).
 * 
 * Searches subreddits: r/pakistan, r/worldnews, r/weather
 * Keywords: flood, earthquake, landslide, storm, crisis, disaster, emergency
 */
@Service
@Slf4j
public class RedditSocialService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    // Reddit public JSON search endpoint
    private static final String REDDIT_SEARCH_URL =
            "https://www.reddit.com/search.json?q=%s&sort=new&t=week&limit=15";

    // Crisis keywords to search for
    private static final List<String> SEARCH_QUERIES = List.of(
            "pakistan flood disaster",
            "pakistan earthquake",
            "pakistan landslide",
            "pakistan storm snow",
            "pakistan heatwave emergency",
            "karachi lahore islamabad disaster"
    );

    public RedditSocialService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Fetches real social media posts about Pakistan crises from Reddit.
     * Returns structured results with post titles, scores, and mapped city/area.
     */
    public SocialResult fetchSocialSignals() {
        SocialResult result = new SocialResult();

        // Pick a random query to rotate and avoid rate limits
        String query = SEARCH_QUERIES.get((int)(System.currentTimeMillis() / 60000) % SEARCH_QUERIES.size());
        String url = String.format(REDDIT_SEARCH_URL, query.replace(" ", "+"));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "AegisNet-CrisisMonitor/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() != HttpStatus.OK) {
                log.warn("[Social] Reddit returned status: {}", response.getStatusCode());
                return result;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode children = root.path("data").path("children");

            if (children.isMissingNode() || !children.isArray()) {
                return result;
            }

            for (JsonNode child : children) {
                JsonNode data = child.path("data");
                String title = data.path("title").asText("");
                String subreddit = data.path("subreddit").asText("");
                int score = data.path("score").asInt(0);
                int numComments = data.path("num_comments").asInt(0);
                String selftext = data.path("selftext").asText("").toLowerCase();
                String permalink = data.path("permalink").asText("");
                double createdUtc = data.path("created_utc").asDouble(0);

                // Only process posts with genuine crisis content
                String titleLower = title.toLowerCase();
                if (!isRelevantPost(titleLower, selftext, subreddit)) continue;

                // Determine crisis type from post content
                String crisisType = detectCrisisType(titleLower + " " + selftext);
                if (crisisType == null) continue;

                // Map to city/area
                String area = mapToArea(titleLower + " " + selftext);
                int severity = calculateSocialSeverity(score, numComments, titleLower);

                SocialPost post = new SocialPost();
                post.title = title;
                post.subreddit = subreddit;
                post.score = score;
                post.numComments = numComments;
                post.crisisType = crisisType;
                post.area = area;
                post.severity = severity;
                post.permalink = "https://reddit.com" + permalink;
                post.createdUtc = createdUtc;

                result.posts.add(post);
                result.traceMessages.add(String.format(
                        "[Social] r/%s: \"%s\" (↑%d, %d comments) → %s in %s [Sev: %d]",
                        subreddit, truncate(title, 50), score, numComments, crisisType, area, severity));
            }

            log.info("[Social] Reddit scan complete: {} relevant posts found for query: {}", 
                    result.posts.size(), query);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("429") || msg.contains("Too Many"))) {
                log.warn("[Social] Reddit rate limit hit. Will retry next cycle.");
            } else {
                log.error("[Social] Reddit API error: {}", msg);
            }
        }

        return result;
    }

    // Subreddits that are never relevant for crisis intelligence
    private static final Set<String> IRRELEVANT_SUBS = Set.of(
            "ArchitecturePorn", "travel", "pics", "EarthPorn", "CityPorn",
            "AskReddit", "todayilearned", "funny", "memes", "gaming",
            "harrypotter", "Worldbuilding", "movies", "television", "books"
    );

    private boolean isRelevantPost(String title, String selftext, String subreddit) {
        // Reject known irrelevant subreddits
        if (IRRELEVANT_SUBS.contains(subreddit)) return false;

        String combined = title + " " + selftext;

        // Must contain CRISIS/URGENCY keywords (not just location mentions)
        List<String> crisisKeywords = List.of(
                "flood", "earthquake", "landslide", "storm", "heatwave",
                "cyclone", "drought", "disaster", "emergency", "rescue",
                "killed", "dead", "death", "trapped", "evacuate", "damage",
                "crisis", "relief", "ndma", "warning", "alert", "torrent",
                "quake", "devastat", "casualties", "stranded", "submerge",
                "collapsed", "destroyed", "displaced", "avalanche", "mudslide"
        );
        List<String> locations = List.of(
                "pakistan", "karachi", "lahore", "islamabad", "peshawar", "quetta",
                "murree", "gilgit", "swat", "sindh", "punjab", "balochistan",
                "kpk", "chitral", "hunza", "skardu", "muzaffarabad"
        );

        // Reject if post is about history, tourism, architecture, beauty
        List<String> rejectKeywords = List.of(
                "beautiful", "architecture", "tourism", "tourist", "travel",
                "historical", "heritage", "photography", "built in", "manor",
                "brewery", "restaurant", "hotel", "original building"
        );
        boolean isIrrelevant = rejectKeywords.stream().anyMatch(combined::contains);
        if (isIrrelevant) return false;

        boolean hasCrisis = crisisKeywords.stream().anyMatch(combined::contains);
        boolean hasLocation = locations.stream().anyMatch(combined::contains);
        return hasCrisis && hasLocation;
    }

    private String detectCrisisType(String text) {
        if (text.contains("flood") || text.contains("rain") || text.contains("torrent") || text.contains("submerge")) return "FLOOD";
        if (text.contains("earthquake") || text.contains("quake") || text.contains("seismic") || text.contains("tremor")) return "EARTHQUAKE";
        if (text.contains("landslide") || text.contains("mudslide") || text.contains("land slide")) return "LANDSLIDE";
        if (text.contains("snow") || text.contains("blizzard") || text.contains("avalanche") || text.contains("road closure")) return "SNOWSTORM";
        if (text.contains("heat") || text.contains("heatwave") || text.contains("temperature record")) return "HEATWAVE";
        if (text.contains("cyclone") || text.contains("hurricane") || text.contains("storm surge")) return "CYCLONE";
        if (text.contains("drought") || text.contains("water shortage") || text.contains("famine")) return "DROUGHT";
        if (text.contains("disaster") || text.contains("emergency") || text.contains("crisis")) return "CRISIS_ALERT";
        return null;
    }

    private String mapToArea(String text) {
        // Specific areas first
        if (text.contains("gilgit")) return "Gilgit";
        if (text.contains("hunza")) return "Hunza Valley";
        if (text.contains("skardu")) return "Skardu";
        if (text.contains("chitral")) return "Chitral";
        if (text.contains("swat")) return "Swat Valley";
        if (text.contains("murree")) return "Murree";
        if (text.contains("muzaffarabad")) return "Muzaffarabad";
        if (text.contains("jacobabad")) return "Jacobabad";
        if (text.contains("tharparkar") || text.contains("thar")) return "Tharparkar";
        // Cities
        if (text.contains("karachi")) return "Karachi";
        if (text.contains("lahore")) return "Lahore";
        if (text.contains("islamabad") || text.contains("rawalpindi")) return "Islamabad";
        if (text.contains("peshawar")) return "Peshawar";
        if (text.contains("quetta")) return "Quetta";
        // Provinces
        if (text.contains("sindh")) return "Sindh";
        if (text.contains("punjab")) return "Punjab";
        if (text.contains("balochistan") || text.contains("baluchistan")) return "Balochistan";
        if (text.contains("kpk") || text.contains("khyber")) return "KPK";
        return "Pakistan";
    }

    private int calculateSocialSeverity(int score, int numComments, String title) {
        int severity = 15; // base for any relevant post
        
        // Engagement amplifies severity
        if (score > 1000) severity += 25;
        else if (score > 500) severity += 20;
        else if (score > 100) severity += 15;
        else if (score > 20) severity += 10;
        
        if (numComments > 200) severity += 15;
        else if (numComments > 50) severity += 10;
        else if (numComments > 10) severity += 5;

        // Keyword amplification
        if (title.contains("killed") || title.contains("dead") || title.contains("death")) severity += 20;
        if (title.contains("emergency") || title.contains("rescue")) severity += 15;
        if (title.contains("trapped") || title.contains("stranded")) severity += 15;
        if (title.contains("warning") || title.contains("alert")) severity += 10;

        return Math.min(severity, 100);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    // --- Result containers ---

    public static class SocialResult {
        public List<SocialPost> posts = new ArrayList<>();
        public List<String> traceMessages = new ArrayList<>();
    }

    public static class SocialPost {
        public String title;
        public String subreddit;
        public int score;
        public int numComments;
        public String crisisType;
        public String area;
        public int severity;
        public String permalink;
        public double createdUtc;
    }
}
