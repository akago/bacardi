package se.kth;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import se.kth.failure_detection.DetectedFileWithErrors;
import se.kth.japicmp_analyzer.JApiCmpAnalyze;
import se.kth.models.MavenErrorLog;
import se.kth.spoon.ApiMetadata;
import se.kth.spoon.Client;
import se.kth.spoon.SpoonFullyQualifiedNameExtractor;
import se.kth.spoon.SpoonUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {
        Map<String, String> options = parseArgs(args);

        if (options.containsKey("help") || !options.containsKey("benchmark") || !options.containsKey("project-root")) {
            printUsage();
            return;
        }

        File jsonFile = new File(options.get("benchmark"));
        Path projectRoot = Path.of(options.get("project-root"));

        ObjectMapper mapper = new ObjectMapper().setDateFormat(new StdDateFormat());
        JavaType type = mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
        Map<String, Object> json = mapper.readValue(jsonFile, type);

        String breakingCommit = (String) json.get("breakingCommit");
        String project = (String) json.get("project");

        Map<String, Object> updatedDependency = (Map<String, Object>) json.get("updatedDependency");
        String previousVersion = (String) updatedDependency.get("previousVersion");
        String newVersion = (String) updatedDependency.get("newVersion");
        String dependencyArtifactID = (String) updatedDependency.get("dependencyArtifactID");

        String previousJar = "%s-%s.jar".formatted(dependencyArtifactID, previousVersion);
        String newJar = "%s-%s.jar".formatted(dependencyArtifactID, newVersion);

        File logFile = projectRoot.resolve(breakingCommit)
                .resolve(project)
                .resolve("%s.log".formatted(breakingCommit)).toFile();

        MavenErrorInformation mavenErrorInformation = new MavenErrorInformation(logFile);

        try {
            MavenErrorLog errorLog = mavenErrorInformation.extractLineNumbersWithPaths(logFile.toString());

            Path newJarVersion = projectRoot.resolve(breakingCommit).resolve(newJar);
            Path oldJarVersion = projectRoot.resolve(breakingCommit).resolve(previousJar);

            ApiMetadata newApi = new ApiMetadata(newJarVersion.getFileName().toString(), newJarVersion);
            ApiMetadata oldApi = new ApiMetadata(oldJarVersion.getFileName().toString(), oldJarVersion);

            JApiCmpAnalyze japicmpAnalyzer = new JApiCmpAnalyze(oldApi, newApi);

            Client client = new Client(projectRoot.resolve(breakingCommit).resolve(project));
            client.setClasspath(List.of(oldJarVersion));

            SpoonUtilities spoonResults = new SpoonUtilities(client);

            SpoonConstructExtractor causingConstructExtractor = new SpoonConstructExtractor(
                    errorLog, japicmpAnalyzer, spoonResults, "pipeline"
            );

            Map<String, Set<DetectedFileWithErrors>> fileWithErrorsMap = causingConstructExtractor.extractCausingConstructs();

            if (fileWithErrorsMap.isEmpty()) {
                System.out.println("No errors detected");
                return;
            }
            fileWithErrorsMap.forEach((key, value) -> {
                
                value.forEach(detectedFileWithErrors -> {
                    if (detectedFileWithErrors.getExecutedElements() == null) {
                        System.out.println("No executed elements detected");
                        return;
                    }

                    detectedFileWithErrors.getExecutedElements().forEach(executedElement -> {
                        System.out.println("Executed element: " + executedElement);
                        System.out.println("FullyQualifiedName: " +
                                SpoonFullyQualifiedNameExtractor.getFullyQualifiedName(executedElement));
                    });

                    System.out.println("Detected file: " + detectedFileWithErrors.methodName);
                    detectedFileWithErrors.getApiChanges().forEach(apiChange -> {
                        System.out.println("Api change: " + apiChange.toDiffString());
                    });

                    System.out.println("-------------------------------------------------");
                    System.out.println(detectedFileWithErrors.getErrorInfo().getErrorMessage());

                });
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if ("--help".equals(args[i])) {
                map.put("help", "");
            } else if (args[i].startsWith("--") && i + 1 < args.length) {
                map.put(args[i].substring(2), args[i + 1]);
                i++;
            }
        }
        return map;
    }

    private static void printUsage() {
        System.out.println("""
            Usage: java -jar extractor.jar --benchmark <path/to/json> --project-root <path/to/project>
            
            Options:
              --benchmark      Path to benchmark JSON file (required)
              --project-root   Root path to the analyzed project files (required)
              --help           Show this help message
            """);
    }
}
