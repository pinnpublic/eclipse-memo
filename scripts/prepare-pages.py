"""Verify a release asset and extract its p2 repository for GitHub Pages."""

import argparse
import hashlib
from pathlib import Path, PurePosixPath
import re
import stat
import zipfile


def prepare(assets: Path, version: str, output: Path) -> None:
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise ValueError("Version must be major.minor.patch")
    archive_name = f"eclipse-workspace-memo-update-site-{version}.zip"
    archive_path = assets / archive_name
    checksums = {}
    for line in (assets / "SHA256SUMS").read_text(encoding="utf-8").splitlines():
        digest, name = line.split("  ", 1)
        if not re.fullmatch(r"[a-fA-F0-9]{64}", digest) or name in checksums:
            raise ValueError("Invalid or duplicate checksum entry")
        checksums[name] = digest.lower()
    digest = hashlib.sha256(archive_path.read_bytes()).hexdigest()
    if checksums.get(archive_name) != digest:
        raise ValueError("Update-site ZIP SHA-256 does not match SHA256SUMS")

    required = {
        "content.jar",
        "artifacts.jar",
        "LICENSE",
        f"features/dev.eclipsememo.feature_{version}.jar",
        f"plugins/dev.eclipsememo_{version}.jar",
    }
    with zipfile.ZipFile(archive_path) as archive:
        for entry in archive.infolist():
            path = PurePosixPath(entry.filename)
            if (path.is_absolute() or ".." in path.parts
                    or "\\" in entry.filename or ":" in entry.filename
                    or stat.S_ISLNK(entry.external_attr >> 16)):
                raise ValueError(f"Unsafe ZIP entry: {entry.filename}")
        if not required.issubset(archive.namelist()):
            raise ValueError("Update-site ZIP is missing p2 metadata or artifacts")
        corrupt_entry = archive.testzip()
        if corrupt_entry:
            raise ValueError(f"Corrupt ZIP entry: {corrupt_entry}")
        output.mkdir(parents=True, exist_ok=False)
        archive.extractall(output)
    (output / ".nojekyll").write_text("", encoding="utf-8")
    print(f"Verified {archive_name}; Pages content is ready in {output}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--assets", required=True, type=Path)
    parser.add_argument("--version", required=True)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    prepare(args.assets, args.version, args.output)
