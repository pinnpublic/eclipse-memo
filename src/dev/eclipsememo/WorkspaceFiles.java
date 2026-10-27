package dev.eclipsememo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** File-system operations independent of Eclipse's project-only resource model. */
public final class WorkspaceFiles {
    private final Path root;
    private final Path realRoot;
    private final Predicate<Path> projectDirectory;

    public WorkspaceFiles(Path root, Predicate<Path> projectDirectory) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        this.realRoot = this.root.toRealPath();
        this.projectDirectory = projectDirectory;
    }

    public Path root() { return root; }

    public List<WorkspaceEntry> list(Path parent) throws IOException {
        check(parent, true);
        if (!Files.isDirectory(parent)) return Collections.emptyList();
        try (Stream<Path> stream = Files.list(parent)) {
            return stream.filter(this::isVisible)
                    .map(WorkspaceEntry::new)
                    .sorted(Comparator.comparing(WorkspaceEntry::isDirectory).reversed()
                            .thenComparing(WorkspaceEntry::toString, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        }
    }

    public boolean isVisible(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(root) || normalized.equals(root)) return false;
        Path relative = root.relativize(normalized);
        if (relative.getName(0).toString().equalsIgnoreCase(".metadata")) return false;
        Path current = root;
        for (Path segment : relative) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current) || projectDirectory.test(current)) return false;
            if (Files.isDirectory(current) && Files.exists(current.resolve(".project"))) return false;
        }
        return true;
    }

    public Path resolveName(Path parent, String name) throws IOException {
        String error = validateName(name);
        if (error != null) throw new IOException(error);
        check(parent, true);
        Path result = parent.resolve(name).toAbsolutePath().normalize();
        check(result, false);
        return result;
    }

    public static String validateName(String name) {
        if (name == null || name.isBlank()) return "파일 이름을 입력하세요.";
        if (name.equals(".") || name.equals("..") || name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*"))
            return "이름에는 경로나 특수 문자를 사용할 수 없습니다.";
        if (name.endsWith(".") || name.endsWith(" ")) return "이름 끝의 점이나 공백을 제거하세요.";
        if (name.equalsIgnoreCase(".metadata") || name.equalsIgnoreCase(".project"))
            return "이클립스 관리 파일 이름은 사용할 수 없습니다.";
        if (name.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?"))
            return "운영체제에서 예약한 이름입니다.";
        return null;
    }

    public void check(Path path, boolean allowRoot) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        if (!(allowRoot && normalized.equals(root)) && !isVisible(normalized))
            throw new IOException("프로젝트 밖의 워크스페이스 파일만 사용할 수 있습니다: " + path);
        Path existing = normalized;
        while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) existing = existing.getParent();
        if (existing == null || !existing.toRealPath().startsWith(realRoot))
            throw new IOException("워크스페이스 밖의 경로는 사용할 수 없습니다.");
    }

    /** New files use UTF-8; valid existing UTF-8 and Korean CP949 are kept as-is. */
    public static String detectEncoding(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        for (byte value : bytes) if (value == 0) throw new IOException("바이너리 파일은 메모 편집기로 열 수 없습니다.");
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
            return "UTF-8";
        } catch (CharacterCodingException ex) {
            try {
                java.nio.charset.Charset.forName("MS949").newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
                return "MS949";
            } catch (CharacterCodingException invalid) {
                throw new IOException("UTF-8 또는 CP949 텍스트 파일이 아닙니다.", invalid);
            }
        }
    }
}
