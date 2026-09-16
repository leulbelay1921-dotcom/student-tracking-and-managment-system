package com.sitms.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Repo hygiene guard (SITMS-057): committed source/config must never carry live secrets.
 *
 * <p>Two rules. (1) No high-certainty credential material anywhere in the repository
 * (private keys, JWTs, cloud/git/SaaS tokens). (2) Secret-keyed YAML/properties values under
 * {@code backend/src/main/resources} may only be environment placeholders (possibly with an
 * overridable default) or empty — never a bare literal credential.
 */
class RepositorySecretScanTest {

  private static final Set<String> PRUNED_DIRS =
      Set.of("node_modules", "target", "dist", "out-tsc", ".angular", "coverage", ".git",
          "testem", "__screenshots__");

  private static final Pattern PRIVATE_KEY =
      Pattern.compile("-----BEGIN (?:RSA |EC |DSA |ECDSA |OPENSSH )?PRIVATE KEY-----");

  private static final Pattern AWS_KEY = Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b");

  private static final Pattern GITHUB_TOKEN =
      Pattern.compile("\\b(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{30,}\\b");

  private static final Pattern JWT = Pattern.compile(
      "\\beyJ[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{10,}\\b");

  private static final Pattern SLACK_TOKEN = Pattern.compile("\\bxox[baprs]-[A-Za-z0-9-]{10,}\\b");

  private static final Pattern STRIPE_LIVE = Pattern.compile("\\b(?:sk|rk)_live_[A-Za-z0-9]{10,}\\b");

  private static final Pattern GOOGLE_API_KEY = Pattern.compile("\\bAIza[0-9A-Za-z_-]{35}\\b");

  /** Secret-keyed YAML/properties keys whose value must be a placeholder or empty. */
  private static final Pattern SECRET_KEY =
      Pattern.compile("^\\s*(?:[a-zA-Z0-9_.-]*[.:])?(?:password|passwd|pwd|secret|token|"
          + "client_?secret|api_?key|apikey)\\s*[:=]\\s*(.*)$");

  @Test
  void repoContainsNoHighCertaintyCredentialMaterial() throws IOException {
    List<String> hits = new ArrayList<>();
    walkScoped(repoRoot(), file -> hits.addAll(credentialsIn(file)));
    assertThat(hits).as("committed credential material must not exist")
        .isEmpty();
  }

  @Test
  void backendConfigNeverCarriesLiteralSecrets() throws IOException {
    Path configDir = repoRoot().resolve("backend/src/main/resources");
    if (!Files.isDirectory(configDir)) {
      return;
    }
    List<String> violations = new ArrayList<>();
    forEachConfigFile(configDir, file -> violations.addAll(literalSecretKeys(file)));
    assertThat(violations).as("config secret values must be env placeholders")
        .isEmpty();
  }

  private void forEachConfigFile(Path root, Consumer<Path> onFile) throws IOException {
    walkScopedInternal(root, file -> {
      String name = file.getFileName().toString();
      if (name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".properties")) {
        onFile.accept(file);
      }
    });
  }

  private void walkScoped(Path root, Consumer<Path> onFile) throws IOException {
    walkScopedInternal(root, file -> {
      if (inScope(file)) {
        onFile.accept(file);
      }
    });
  }

  private void walkScopedInternal(Path root, Consumer<Path> onFile) throws IOException {
    Files.walkFileTree(root, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (!dir.equals(root) && PRUNED_DIRS.contains(dir.getFileName().toString())) {
          return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        onFile.accept(file);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private List<String> credentialsIn(Path file) {
    List<String> hits = new ArrayList<>();
    List<String> lines;
    try {
      lines = Files.readAllLines(file);
    } catch (IOException e) {
      return hits;
    }
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      boolean matched = PRIVATE_KEY.matcher(line).find() || AWS_KEY.matcher(line).find()
          || GITHUB_TOKEN.matcher(line).find() || JWT.matcher(line).find()
          || SLACK_TOKEN.matcher(line).find() || STRIPE_LIVE.matcher(line).find()
          || GOOGLE_API_KEY.matcher(line).find();
      if (matched) {
        hits.add(relative(file) + ":" + (i + 1) + ": " + abbreviate(line.trim()));
      }
    }
    return hits;
  }

  private List<String> literalSecretKeys(Path file) {
    List<String> hits = new ArrayList<>();
    List<String> lines;
    try {
      lines = Files.readAllLines(file);
    } catch (IOException e) {
      return hits;
    }
    for (int i = 0; i < lines.size(); i++) {
      Matcher m = SECRET_KEY.matcher(lines.get(i));
      if (m.matches()) {
        String value = m.group(1).trim();
        if (value.isEmpty() || value.startsWith("${") || isQuotedEmpty(value)) {
          continue;
        }
        hits.add(relative(file) + ":" + (i + 1) + ": literal value for secret key: " + value);
      }
    }
    return hits;
  }

  private boolean isQuotedEmpty(String value) {
    return value.equals("\"\"") || value.equals("''") || "null".equals(value);
  }

  private boolean inScope(Path file) {
    String name = file.getFileName().toString();
    if (name.endsWith(".min.js") || name.endsWith(".map") || name.endsWith(".lock")
        || name.endsWith(".sum") || name.endsWith(".jar") || name.endsWith(".class")
        || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".ico")
        || name.endsWith(".woff2") || name.endsWith(".ttf") || name.endsWith(".woff")
        || name.endsWith(".gif") || name.endsWith(".svg")) {
      return false;
    }
    try {
      return Files.size(file) < 2_000_000; // skip generated/binary/large artifacts
    } catch (IOException e) {
      return false;
    }
  }

  private Path repoRoot() {
    // Surefire runs with the Maven module dir as CWD → its parent is the repo root.
    return Path.of("").toAbsolutePath().getParent();
  }

  private String relative(Path file) {
    return repoRoot().relativize(file).toString();
  }

  private String abbreviate(String line) {
    return line.length() <= 120 ? line : line.substring(0, 117) + "...";
  }
}