package com.sandboni.core.engine.sta;

import com.sandboni.core.engine.contract.ThrowingConsumer;
import com.sandboni.core.engine.sta.graph.Link;
import com.sandboni.core.engine.sta.graph.LinkType;
import com.sandboni.core.engine.sta.graph.vertex.Vertex;
import com.sandboni.core.scm.scope.Change;
import com.sandboni.core.scm.scope.ChangeScope;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Context {

    private final String filter;
    private Set<Link> links = new HashSet<>();
    private String currentLocation;
    private ChangeScope<Change> changeScope;
    private Collection<String> srcLocations;
    private Collection<String> testLocations;

    private Set<LinkType> adoptedLinkTypes;

    private boolean inScope(String actor) {
        return filter == null || (actor != null && actor.startsWith(filter));
    }

    private boolean inScope(Vertex vertex) {
        return inScope(vertex.getActor());
    }

    public ChangeScope<Change> getChangeScope() {
        return changeScope;
    }

    public Context(String[] srcLocation, String[] testLocation, String filter, ChangeScope<Change> changes) {
        this(srcLocation, testLocation, filter, changes, null);
    }

    public Context(String[] srcLocation, String[] testLocation, String filter, ChangeScope<Change> changes, String currentLocation) {
        this.srcLocations = Collections.unmodifiableCollection(Arrays.stream(srcLocation)
                .map(l -> new File(l).getAbsolutePath())
                .collect(Collectors.toCollection(ArrayList::new)));
        this.testLocations = Collections.unmodifiableCollection(Arrays.stream(testLocation)
                .map(l -> new File(l).getAbsolutePath())
                .collect(Collectors.toCollection(ArrayList::new)));

        this.filter = filter;
        this.changeScope = changes;
        this.adoptedLinkTypes = new HashSet<>();
        this.currentLocation = currentLocation;
    }

    public Context getLocalContext() {
        return new Context(this.srcLocations.toArray(new String[0]), this.testLocations.toArray(new String[0]), this.filter, this.changeScope, this.currentLocation);
    }

    public synchronized Stream<Link> getLinks() {
        return new ArrayList<>(links).parallelStream();
    }

    private int adoptLink(Link link) {
        link.setFilter(filter);
        adoptedLinkTypes.add(link.getLinkType());
        return links.add(link) ? 1 : 0;
    }

    public synchronized int addLink(Link link) {
        if (inScope(link.getCaller()) || inScope(link.getCallee()) || (link.getCaller().isSpecial() && link.getCallee().isSpecial())) {
            return adoptLink(link);
        }
        return 0;
    }

    public synchronized String getCurrentLocation() {
        return currentLocation;
    }

    public synchronized int addLinks(Link... linksToAdd) {
        int result = 0;
        for (Link link : linksToAdd) {
            result = result + addLink(link);
        }
        return result;
    }

    public synchronized void forEachLocation(ThrowingConsumer<String> consumer) {
        testLocations.forEach(s -> {
            currentLocation = s;
            consumer.accept(currentLocation);
        });

        srcLocations.forEach(s -> {
            currentLocation = s;
            consumer.accept(currentLocation);
        });
    }

    public boolean isAdoptedLinkType(LinkType...linkTypes){
        return Arrays.stream(linkTypes).allMatch(t -> adoptedLinkTypes.contains(t));
    }

    public Collection<String> getSrcLocations() {
        return srcLocations;
    }

    public Collection<String> getTestLocations() {
        return testLocations;
    }
}