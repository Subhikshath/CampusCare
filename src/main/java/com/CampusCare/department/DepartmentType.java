package com.CampusCare.department;

public enum DepartmentType {
    ELECTRICAL_MAINTENANCE(
            "Electrical Maintenance",
            "Power, lighting, electrical outlets, wiring, and electrical safety concerns."),
    CIVIL_MAINTENANCE(
            "Civil Maintenance",
            "Building damage, walls, ceilings, doors, windows, flooring, and structural repairs."),
    PLUMBING(
            "Plumbing",
            "Water leaks, taps, pipes, drains, toilets, and water supply."),
    IT_SUPPORT(
            "IT Support",
            "Campus Wi-Fi, network access, computers, projectors, and other IT equipment."),
    HOUSEKEEPING(
            "Housekeeping",
            "Cleaning, waste collection, spills, and general campus cleanliness."),
    HOSTEL_MAINTENANCE(
            "Hostel Maintenance",
            "Hostel building repairs and maintenance issues not related to room allocation or hostel administration."),
    TRANSPORT(
            "Transport",
            "General campus transportation issues not specifically related to college buses or transport facilities."),
    SECURITY(
            "Security",
            "Campus safety, security incidents, access control, and hazards requiring immediate attention."),
    ADMINISTRATION(
            "Administration",
            "General administrative, campus service, and policy questions that do not fit another department."),
    GROUNDS_AND_GARDENING(
            "Grounds and Gardening",
            "Campus landscaping, trees, gardens, outdoor grounds, and related maintenance."),
    TRANSPORT_SERVICES(
            "Transport Department",
            "College buses, bus delays, routes, vehicle maintenance, bus stops, and transport facilities."),
    LIBRARY_SERVICES(
            "Library Services Department",
            "Library access, damaged or missing books, seating, study spaces, and library facilities."),
    LABORATORY_MANAGEMENT(
            "Laboratory Management Department",
            "Lab equipment malfunctions, missing apparatus, lab access, and laboratory facilities."),
    SPORTS_AND_RECREATION(
            "Sports and Recreation Department",
            "Damaged sports equipment, playgrounds, recreation spaces, and sports facility maintenance."),
    HOSTEL_ADMINISTRATION(
            "Hostel Administration Department",
            "Hostel room allocation, hostel administration, common areas, and hostel facilities."),
    CANTEEN_AND_FOOD_SERVICES(
            "Canteen and Food Services Department",
            "Food quality, food hygiene, canteen cleanliness, meal service, and canteen facilities.");

    private final String displayName;
    private final String routingGuidance;

    DepartmentType(String displayName, String routingGuidance) {
        this.displayName = displayName;
        this.routingGuidance = routingGuidance;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRoutingGuidance() {
        return routingGuidance;
    }
}