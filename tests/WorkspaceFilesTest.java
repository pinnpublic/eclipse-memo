package dev.eclipsememo;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class WorkspaceFilesTest {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Path.of(args[0]), "files-test-");
        Path registered = Files.createDirectory(root.resolve("registered"));
        Path unimported = Files.createDirectory(root.resolve("unimported"));
        Files.createFile(unimported.resolve(".project"));
        Files.createDirectory(root.resolve(".metadata"));
        Path folder = Files.createDirectory(root.resolve("일반 폴더"));
        Files.writeString(root.resolve("메모.txt"), "한글 메모\n", StandardCharsets.UTF_8);
        WorkspaceFiles files = new WorkspaceFiles(root, registered::equals);
        List<String> names = files.list(root).stream().map(Object::toString).collect(Collectors.toList());
        check(names.equals(List.of("일반 폴더", "메모.txt")), "only ordinary files and folders; folders sort first");
        check(!files.isVisible(root.resolve(".metadata/settings")), "metadata descendants excluded");
        check(!files.isVisible(registered.resolve("text.txt")), "registered project descendants excluded");
        check(!files.isVisible(unimported.resolve("text.txt")), "unimported project descendants excluded");
        check(files.resolveName(folder, "새 메모.md").equals(folder.resolve("새 메모.md")), "Korean filenames with spaces");
        for (String name : List.of("..", ".", "../escape", "a/b", "a\\b", "C:foo", ".metadata", ".project", "con.txt", "a.", "a ", "")) {
            check(WorkspaceFiles.validateName(name) != null, "reject unsafe name: " + name);
        }
        rejected(() -> files.check(root.getParent().resolve("outside.txt"), false), "outside workspace");
        rejected(() -> files.resolveName(root, ".metadata"), "metadata as save target");
        rejected(() -> files.resolveName(registered, "file.txt"), "project as save target");
        check(WorkspaceFiles.detectEncoding(root.resolve("메모.txt")).equals("UTF-8"), "UTF-8 recognized");
        Path cp949 = root.resolve("옛날.txt");
        Files.write(cp949, "기존 한글 메모".getBytes(Charset.forName("MS949")));
        check(WorkspaceFiles.detectEncoding(cp949).equals("MS949"), "CP949 recognized");
        Path binary = root.resolve("binary.bin");
        Files.write(binary, new byte[] { 0, 1, 2 });
        rejected(() -> WorkspaceFiles.detectEncoding(binary), "binary rejected");
        System.out.println("PASS: " + assertions + " filesystem assertions; fixture: " + root);
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    @FunctionalInterface interface Operation { void run() throws Exception; }
    private static void rejected(Operation operation, String message) throws Exception {
        try { operation.run(); throw new AssertionError("not rejected: " + message); }
        catch (java.io.IOException expected) { assertions++; }
    }
}
