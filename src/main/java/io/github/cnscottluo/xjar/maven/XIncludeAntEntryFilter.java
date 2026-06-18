package io.github.cnscottluo.xjar.maven;

import io.xjar.XEntryFilter;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;

public class XIncludeAntEntryFilter extends XAntEntryFilter implements XEntryFilter<JarArchiveEntry> {

    public XIncludeAntEntryFilter(String ant) {
        super(ant);
    }

    @Override
    public boolean filtrate(JarArchiveEntry entry) {
        return matches(entry.getName());
    }
}
