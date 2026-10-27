package dev.eclipsememo;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;

/** Watches ordinary workspace directories, never .metadata or project contents. */
final class WorkspaceWatcher implements AutoCloseable {
    private final WorkspaceFiles files;
    private final Runnable changed;
    private final WatchService service;
    private final Thread thread;

    WorkspaceWatcher(WorkspaceFiles files, Runnable changed) throws IOException {
        this.files = files;
        this.changed = changed;
        service = files.root().getFileSystem().newWatchService();
        thread = new Thread(this::run, "Workspace Memo file watcher");
        thread.setDaemon(true);
    }

    void start() { thread.start(); }

    private void register(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(files.root()) && !files.isVisible(dir)) return FileVisitResult.SKIP_SUBTREE;
                dir.register(service, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFileFailed(Path path, IOException ex) {
                return FileVisitResult.SKIP_SUBTREE;
            }
        });
    }

    private void run() {
        try {
            register(files.root());
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = service.take();
                boolean update = false;
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) { update = true; continue; }
                    Path path = ((Path) key.watchable()).resolve((Path) event.context());
                    if (!files.isVisible(path)) continue;
                    update = true;
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(path)) {
                        try { register(path); } catch (IOException ex) { Activator.log("폴더 감시를 추가하지 못했습니다.", ex); }
                    }
                }
                key.reset();
                if (update) changed.run();
            }
        } catch (ClosedWatchServiceException | InterruptedException expected) {
            Thread.currentThread().interrupt();
        } catch (IOException ex) {
            Activator.log("파일 변경 감시가 중단되었습니다. 새로고침 버튼은 계속 사용할 수 있습니다.", ex);
        }
    }

    @Override public void close() throws IOException { service.close(); thread.interrupt(); }
}
