package se.meepo.dinso.service;

import java.util.Set;

public record DemoProfile(
    String id,
    CustomerId customerId,
    PortalType portal,
    DemoRole role,
    String name,
    String description,
    Set<DemoPermission> permissions) {}
