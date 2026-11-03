package com.algoprep.identity.domain.model;

import java.util.List;

/**
 * A set of {@code action_type} values written as prefixes, so a repository can turn it into a
 * predicate without knowing the catalog.
 *
 * @param prefixes an action matches when it starts with any of these
 * @param knownPrefixes every prefix the catalog knows, used when {@code includeUnmapped} is set
 * @param includeUnmapped also match an action that starts with none of {@code knownPrefixes}
 */
public record ActionMatch(
    List<String> prefixes, List<String> knownPrefixes, boolean includeUnmapped) {}
