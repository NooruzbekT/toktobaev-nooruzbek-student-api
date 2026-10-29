package kg.toktobaev.nooruzbek.studentapi.info;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shows who made the application and which version is running.
 */
@RestController
public class InfoController {

    private final String appName;
    private final String author;
    private final String version;

    public InfoController(@Value("${app.name}") String appName,
                          @Value("${app.author}") String author,
                          ObjectProvider<BuildProperties> buildProperties) {
        this.appName = appName;
        this.author = author;
        BuildProperties build = buildProperties.getIfAvailable();
        this.version = build != null ? build.getVersion() : "dev";
    }

    @GetMapping({"/", "/api/info"})
    public Map<String, String> info() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("app", appName);
        info.put("author", author);
        info.put("version", version);
        info.put("endpoints", "/api/students");
        return info;
    }
}
