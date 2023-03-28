package io.slingr.services.services.application;

import io.slingr.services.utils.Json;
import io.slingr.services.utils.converters.JsonSource;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Object that represents the information about a user on the application
 *
 * <p>Created by lefunes on 04/07/18.
 */
public class AppUser implements JsonSource {

    private final String id;
    private final String version;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String fullName;
    private final String status;
    private final boolean active;
    private final boolean developer;
    private final List<Json> groups;
    private final Json localization;
    private final Json permissions;

    /**
     * Creates a new application user
     *
     * @param id id of the user on the application
     * @param version version of the user information
     * @param email user email
     * @param firstName first name of the user
     * @param lastName last name of the user
     * @param fullName full name of the user
     * @param status status of the user
     * @param developer true if the user is a developer of the application
     * @param groups list of groups
     * @param localization information about the localization of the user
     * @param permissions permissions of the user
     */
    public AppUser(String id, String version, String email, String firstName, String lastName, String fullName, String status, boolean developer, List<Json> groups, Json localization, Json permissions) {
        this.id = id;
        this.version = version;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.status = status;
        this.active = StringUtils.isNotBlank(status) && status.equalsIgnoreCase("ACTIVE");
        this.developer = developer;
        this.groups = groups != null ? groups : new ArrayList<>();
        this.localization = localization != null && localization.isMap() ? localization : Json.map();
        this.permissions = permissions != null && permissions.isMap() ? permissions : Json.map();
    }

    /**
     * Gets the id of the user on the application
     *
     * @return id of the user on the application
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the version of the user information
     *
     * @return version of the user information
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the user email
     *
     * @return user email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Gets the first name of the user
     *
     * @return first name of the user
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Gets the last name of the user
     *
     * @return last name of the user
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Gets the full name of the user
     *
     * @return full name of the user
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Gets the status of the user
     *
     * @return status of the user
     */
    public String getStatus() {
        return status;
    }

    /**
     * Returns true if the user is active
     *
     * @return true if the user is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns true if the user is a developer of the application
     *
     * @return true if the user is a developer of the application
     */
    public boolean isDeveloper() {
        return developer;
    }

    /**
     * Gets the list of groups
     *
     * @return list of groups
     */
    public List<Json> getGroups() {
        return groups;
    }

    /**
     * Gets the information about the localization of the user
     *
     * @return information about the localization of the user
     */
    public Json getLocalization() {
        return localization;
    }

    /**
     * Gets the permissions of the user
     *
     * @return permissions of the user
     */
    public Json getPermissions() {
        return permissions;
    }

    @Override
    public Json toJson() {
        final List<Json> gps = new ArrayList<>();
        for (Json group : groups) {
            gps.add(group.cloneJson());
        }

        return Json.map()
                .setIfNotEmpty("id", id)
                .setIfNotEmpty("version", version)
                .setIfNotEmpty("email", email)
                .setIfNotEmpty("firstName", firstName)
                .setIfNotEmpty("lastName", lastName)
                .setIfNotEmpty("fullName", fullName)
                .setIfNotEmpty("status", status)
                .set("developer", developer)
                .setIfNotEmpty("groups", gps)
                .setIfNotEmpty("localization", localization.cloneJson())
                .setIfNotEmpty("permissions", permissions.cloneJson());
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    /**
     * Creates a new application user from the provided json
     *
     * @param json user information fo the user
     * @return application user
     */
    public static AppUser fromJson(Json json) {
        if(json == null) {
            return null;
        }
        return new AppUser(
                json.string("id"),
                json.string("version"),
                json.string("email"),
                json.string("firstName"),
                json.string("lastName"),
                json.string("fullName"),
                json.string("status"),
                json.bool("developer", false),
                json.jsons("groups"),
                json.json("localization"),
                json.json("permissions")
        );
    }
}
