package dev.eclipsememo;

import java.nio.file.Files;
import java.nio.file.Path;

/** A disk entry, deliberately not an Eclipse IResource or a synthetic project. */
public final class WorkspaceEntry {
    private final Path path;

    public WorkspaceEntry(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    public Path path() { return path; }
    public boolean isDirectory() { return Files.isDirectory(path); }
    @Override public String toString() { return path.getFileName().toString(); }
    @Override public int hashCode() { return path.hashCode(); }
    @Override public boolean equals(Object other) {
        return other instanceof WorkspaceEntry && path.equals(((WorkspaceEntry) other).path);
    }
}
